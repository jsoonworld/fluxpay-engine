package com.fluxpay.engine.domain.model.payment;

import java.util.Objects;

/**
 * Value Object representing a payment method.
 * Contains the type of payment and an optional display name.
 */
public record PaymentMethod(PaymentMethodType type, String displayName) {

    public PaymentMethod {
        Objects.requireNonNull(type, "Payment method type is required");
        // displayName is nullable
    }

    /**
     * Creates a card payment method without a display name.
     */
    public static PaymentMethod card() {
        return new PaymentMethod(PaymentMethodType.CARD, null);
    }

    /**
     * Creates a card payment method with a display name.
     */
    public static PaymentMethod card(String displayName) {
        return new PaymentMethod(PaymentMethodType.CARD, displayName);
    }

    /**
     * Creates a bank transfer payment method without a display name.
     */
    public static PaymentMethod bankTransfer() {
        return new PaymentMethod(PaymentMethodType.BANK_TRANSFER, null);
    }

    /**
     * Creates a bank transfer payment method with a display name.
     */
    public static PaymentMethod bankTransfer(String displayName) {
        return new PaymentMethod(PaymentMethodType.BANK_TRANSFER, displayName);
    }

    /**
     * Creates a virtual account payment method without a display name.
     */
    public static PaymentMethod virtualAccount() {
        return new PaymentMethod(PaymentMethodType.VIRTUAL_ACCOUNT, null);
    }

    /**
     * Creates a virtual account payment method with a display name.
     */
    public static PaymentMethod virtualAccount(String displayName) {
        return new PaymentMethod(PaymentMethodType.VIRTUAL_ACCOUNT, displayName);
    }

    /**
     * Creates a mobile payment method without a display name.
     */
    public static PaymentMethod mobile() {
        return new PaymentMethod(PaymentMethodType.MOBILE, null);
    }

    /**
     * Creates a mobile payment method with a display name.
     */
    public static PaymentMethod mobile(String displayName) {
        return new PaymentMethod(PaymentMethodType.MOBILE, displayName);
    }

    /**
     * Creates an easy pay payment method with a provider name as display name.
     * Provider is required for easy pay methods.
     *
     * @param provider the name of the easy pay provider (e.g., "Kakao Pay", "Naver Pay")
     * @throws NullPointerException if provider is null
     */
    public static PaymentMethod easyPay(String provider) {
        Objects.requireNonNull(provider, "Easy pay provider is required");
        return new PaymentMethod(PaymentMethodType.EASY_PAY, provider);
    }
}
