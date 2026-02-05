package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that easyPayProvider is provided when paymentMethod is EASY_PAY.
 */
@Documented
@Constraint(validatedBy = EasyPayProviderValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEasyPayProvider {
    String message() default "easyPayProvider is required for EASY_PAY payments";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
