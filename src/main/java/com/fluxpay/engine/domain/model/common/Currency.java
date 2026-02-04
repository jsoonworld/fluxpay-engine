package com.fluxpay.engine.domain.model.common;

/**
 * Represents supported currencies in the payment system.
 */
public enum Currency {
    KRW("Korean Won", 0),
    USD("US Dollar", 2),
    JPY("Japanese Yen", 0),
    EUR("Euro", 2);

    private final String displayName;
    private final int decimalPlaces;

    Currency(String displayName, int decimalPlaces) {
        this.displayName = displayName;
        this.decimalPlaces = decimalPlaces;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}
