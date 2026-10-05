package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.entity.Order;
import com.ecommerce.marketplace.entity.OrderItem;
import com.ecommerce.marketplace.entity.PaymentStatus;
import com.ecommerce.marketplace.entity.User;
import com.ecommerce.marketplace.exception.BusinessException;
import com.ecommerce.marketplace.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.repository.OrderRepository;
import com.ecommerce.marketplace.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public InvoiceService(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateInvoice(String username, Long orderId) {
        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Order order = orderRepository.findByIdAndUser(orderId, customer)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessException("An invoice is available only for paid orders", HttpStatus.CONFLICT);
        }

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            InvoiceWriter writer = new InvoiceWriter(document);
            writer.line("INVOICE", 20, true);
            writer.line("Enterprise Marketplace", 12, true);
            writer.line("Invoice number: " + order.getId(), 10, false);
            writer.line("Order date: " + order.getPlacedAt().format(DATE_FORMAT), 10, false);
            writer.line("Payment status: " + order.getPaymentStatus(), 10, false);
            writer.line("DEMO PAYMENT - no real money was charged", 10, false);
            writer.line("Fulfillment status: " + order.getStatus(), 10, false);
            writer.line("Bill to: " + customer.getUsername(), 10, false);
            writer.line("Email: " + customer.getEmail(), 10, false);
            writer.line("", 10, false);
            writer.line("Items", 14, true);
            writer.line("Description | Qty | Unit price | Line total", 10, true);

            for (OrderItem item : order.getItems()) {
                BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                writer.line(item.getProduct().getName(), 10, false);
                writer.line("  " + item.getQuantity() + " | " + money(item.getUnitPrice())
                        + " | " + money(lineTotal), 10, false);
            }

            BigDecimal subtotal = order.getSubtotalAmount() == null ? order.getTotalAmount() : order.getSubtotalAmount();
            BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
            writer.line("", 10, false);
            writer.line("Subtotal: " + money(subtotal), 10, false);
            if (order.getCouponCode() != null) {
                writer.line("Coupon: " + order.getCouponCode(), 10, false);
            }
            writer.line("Discount: -" + money(discount), 10, false);
            writer.line("Total paid: " + money(order.getTotalAmount()), 14, true);
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate order invoice", exception);
        }
    }

    private String money(BigDecimal amount) {
        return "INR " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String pdfSafe(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static final class InvoiceWriter implements AutoCloseable {
        private final PDDocument document;
        private PDPageContentStream contentStream;
        private float y = 750;

        private InvoiceWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void line(String text, float fontSize, boolean bold) throws IOException {
            if (y < 55) {
                newPage();
            }
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(
                    bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), fontSize);
            contentStream.newLineAtOffset(50, y);
            contentStream.showText(pdfSafe(text));
            contentStream.endText();
            y -= fontSize + 8;
        }

        private void newPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
            document.addPage(new PDPage());
            contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages() - 1));
            y = 750;
        }

        @Override
        public void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }
}
