package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EasyPayProviderValidatorTest {

    private EasyPayProviderValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new EasyPayProviderValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    @DisplayName("should return true when request is null")
    void shouldReturnTrueWhenRequestIsNull() {
        // When
        boolean result = validator.isValid(null, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when paymentMethod is CARD")
    void shouldReturnTrueWhenPaymentMethodIsCard() {
        // Given
        var request = new ApprovePaymentRequest("CARD", null);

        // When
        boolean result = validator.isValid(request, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when paymentMethod is BANK_TRANSFER")
    void shouldReturnTrueWhenPaymentMethodIsBankTransfer() {
        // Given
        var request = new ApprovePaymentRequest("BANK_TRANSFER", null);

        // When
        boolean result = validator.isValid(request, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when EASY_PAY with valid provider")
    void shouldReturnTrueWhenEasyPayWithValidProvider() {
        // Given
        var request = new ApprovePaymentRequest("EASY_PAY", "KAKAOPAY");

        // When
        boolean result = validator.isValid(request, context);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when EASY_PAY with null provider")
    void shouldReturnFalseWhenEasyPayWithNullProvider() {
        // Given
        var request = new ApprovePaymentRequest("EASY_PAY", null);

        // When
        boolean result = validator.isValid(request, context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when EASY_PAY with blank provider")
    void shouldReturnFalseWhenEasyPayWithBlankProvider() {
        // Given
        var request = new ApprovePaymentRequest("EASY_PAY", "   ");

        // When
        boolean result = validator.isValid(request, context);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when EASY_PAY with empty provider")
    void shouldReturnFalseWhenEasyPayWithEmptyProvider() {
        // Given
        var request = new ApprovePaymentRequest("EASY_PAY", "");

        // When
        boolean result = validator.isValid(request, context);

        // Then
        assertThat(result).isFalse();
    }
}
