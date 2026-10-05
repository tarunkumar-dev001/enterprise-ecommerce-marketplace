package com.ecommerce.marketplace.service;

import com.ecommerce.marketplace.event.OrderPlacedEvent;
import com.ecommerce.marketplace.event.OrderStatusUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class OrderEmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderEmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public OrderEmailNotificationService(
            JavaMailSender mailSender,
            @Value("${marketplace.mail.from:no-reply@marketplace.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        send(event.customerEmail(),
                "Order confirmation #" + event.orderId(),
                "Your order has been placed.\n"
                        + "Order ID: " + event.orderId() + "\n"
                        + "Total: " + event.totalAmount().toPlainString() + "\n"
                        + "Payment status: PENDING");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendOrderStatusUpdate(OrderStatusUpdatedEvent event) {
        send(event.customerEmail(),
                "Order #" + event.orderId() + " status update",
                "The fulfillment status of your order has changed to " + event.status() + ".\n"
                        + "Order ID: " + event.orderId());
    }

    private void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            logger.error("Could not send order notification email to {}", recipient, exception);
        }
    }
}