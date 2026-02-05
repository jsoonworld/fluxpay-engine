package com.fluxpay.engine.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Currency")
class CurrencyTest {

    @Nested
    @DisplayName("decimal places")
    class DecimalPlaces {

        @ParameterizedTest(name = "{0} should have {1} decimal places")
        @CsvSource({
            "KRW, 0",
            "JPY, 0",
            "USD, 2",
            "EUR, 2"
        })
        @DisplayName("should return correct decimal places for each currency")
        void shouldReturnCorrectDecimalPlaces(String currencyCode, int expectedDecimalPlaces) {
            Currency currency = Currency.valueOf(currencyCode);
            assertThat(currency.getDecimalPlaces()).isEqualTo(expectedDecimalPlaces);
        }

        @Test
        @DisplayName("KRW should have 0 decimal places")
        void krwShouldHaveZeroDecimalPlaces() {
            assertThat(Currency.KRW.getDecimalPlaces()).isZero();
        }

        @Test
        @DisplayName("USD should have 2 decimal places")
        void usdShouldHaveTwoDecimalPlaces() {
            assertThat(Currency.USD.getDecimalPlaces()).isEqualTo(2);
        }

        @Test
        @DisplayName("JPY should have 0 decimal places")
        void jpyShouldHaveZeroDecimalPlaces() {
            assertThat(Currency.JPY.getDecimalPlaces()).isZero();
        }

        @Test
        @DisplayName("EUR should have 2 decimal places")
        void eurShouldHaveTwoDecimalPlaces() {
            assertThat(Currency.EUR.getDecimalPlaces()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("enum values")
    class EnumValues {

        @Test
        @DisplayName("should have exactly 4 currencies defined")
        void shouldHaveExactlyFourCurrencies() {
            assertThat(Currency.values()).hasSize(4);
        }

        @Test
        @DisplayName("should contain all expected currencies")
        void shouldContainAllExpectedCurrencies() {
            assertThat(Currency.values())
                .containsExactlyInAnyOrder(Currency.KRW, Currency.USD, Currency.JPY, Currency.EUR);
        }
    }
}
