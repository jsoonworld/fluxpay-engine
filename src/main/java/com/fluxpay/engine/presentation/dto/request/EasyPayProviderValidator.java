package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator that ensures easyPayProvider is not blank when paymentMethod is EASY_PAY.
 */
public class EasyPayProviderValidator implements ConstraintValidator<ValidEasyPayProvider, ApprovePaymentRequest> {

    private static final String EASY_PAY = "EASY_PAY";

    @Override
    public boolean isValid(ApprovePaymentRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        String paymentMethod = request.paymentMethod();
        String easyPayProvider = request.easyPayProvider();

        if (EASY_PAY.equals(paymentMethod)) {
            return easyPayProvider != null && !easyPayProvider.isBlank();
        }

        return true;
    }
}
