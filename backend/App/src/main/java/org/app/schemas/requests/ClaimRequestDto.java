package org.app.schemas.requests;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ToString
public class ClaimRequestDto {

    // ==========================================
    // 1. CONSUMER DATA (PII)
    // ==========================================
    @NotBlank(message = "Full name is required")
    private String fullName;

    // Optional (as requested)
    private String email;

    // Optional (as requested)
    private String phone;

    @NotBlank(message = "ID card (cédula) is required")
    @Pattern(regexp = "^\\d{10}$", message = "ID card must be exactly 10 numeric digits")
    private String idCard; // Mandatory now

    // ==========================================
    // 2. PURCHASE OR SERVICE INFORMATION
    // ==========================================
    @NotBlank(message = "Claim type is required")
    private String claimType;

    @NotBlank(message = "Product or service name is required")
    private String productOrService;

    @NotBlank(message = "Provider or establishment is required")
    private String provider;

    @NotBlank(message = "Provider city is required")
    private String providerCity; // Added per your UI requirement

    @NotNull(message = "Purchase date is required")
    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDate purchaseDate;

    @NotNull(message = "Paid amount is required")
    @DecimalMin(value = "0.01", message = "Paid amount must be greater than zero")
    private BigDecimal paidAmount;

    // ==========================================
    // 3. CLAIM DETAILS
    // ==========================================
    @NotBlank(message = "Incident description is required")
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String incidentDescription; // "Describe what happened"

    @NotBlank(message = "Requested solution is required")
    private String requestedSolution; // "What solution do you request?"
}