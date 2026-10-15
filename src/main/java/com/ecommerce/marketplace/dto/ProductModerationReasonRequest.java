package com.ecommerce.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductModerationReasonRequest(
        @NotBlank(message = "Moderation reason is required")
        @Size(max = 1000, message = "Moderation reason must be 1000 characters or fewer")
        String reason) {
}
