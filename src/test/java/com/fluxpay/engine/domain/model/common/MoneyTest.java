package com.fluxpay.engine.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money")
class MoneyTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create Money with BigDecimal amount and currency")
        void shouldCreateMoneyWithBigDecimalAndCurrency() {
            Money money = Money.of(BigDecimal.valueOf(1000), Currency.KRW);

            assertThat(money.amount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(money.currency()).isEqualTo(Currency.KRW);
        }

        @Test
        @DisplayName("should create Money with long amount and currency")
        void shouldCreateMoneyWithLongAndCurrency() {
            Money money = Money.of(1000L, Currency.KRW);

            assertThat(money.amount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(money.currency()).isEqualTo(Currency.KRW);
        }

        @Test
        @DisplayName("should create KRW money using krw factory method")
        void shouldCreateKrwMoneyUsingFactory() {
            Money money = Money.krw(10000L);

            assertThat(money.amount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
            assertThat(money.currency()).isEqualTo(Currency.KRW);
        }

        @Test
        @DisplayName("should create zero money using zero factory method")
        void shouldCreateZeroMoney() {
            Money money = Money.zero(Currency.USD);

            assertThat(money.amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(money.currency()).isEqualTo(Currency.USD);
        }

        @Test
        @DisplayName("should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThatThrownBy(() -> Money.of(null, Currency.KRW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw exception when currency is null")
        void shouldThrowExceptionWhenCurrencyIsNull() {
            assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(1000), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("should throw exception when amount is negative")
        void shouldThrowExceptionWhenAmountIsNegative() {
            assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-100), Currency.KRW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("should apply currency decimal places with rounding")
        void shouldApplyCurrencyDecimalPlacesWithRounding() {
            // USD has 2 decimal places, 10.555 should round to 10.56
            Money money = Money.of(new BigDecimal("10.555"), Currency.USD);

            assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("10.56"));
        }

        @Test
        @DisplayName("should round down to zero decimal places for KRW")
        void shouldRoundDownToZeroDecimalPlacesForKrw() {
            Money money = Money.of(new BigDecimal("1000.5"), Currency.KRW);

            assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("1001"));
        }
    }

    @Nested
    @DisplayName("arithmetic operations")
    class ArithmeticOperations {

        @Test
        @DisplayName("should add two money objects with same currency")
        void shouldAddTwoMoneyObjectsWithSameCurrency() {
            Money a = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(50), Currency.USD);

            Money result = a.add(b);

            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(150));
            assertThat(result.currency()).isEqualTo(Currency.USD);
        }

        @Test
        @DisplayName("should throw exception when adding money with different currencies")
        void shouldThrowExceptionWhenAddingDifferentCurrencies() {
            Money usd = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money krw = Money.of(BigDecimal.valueOf(100), Currency.KRW);

            assertThatThrownBy(() -> usd.add(krw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("should subtract two money objects with same currency")
        void shouldSubtractTwoMoneyObjectsWithSameCurrency() {
            Money a = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(30), Currency.USD);

            Money result = a.subtract(b);

            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(70));
            assertThat(result.currency()).isEqualTo(Currency.USD);
        }

        @Test
        @DisplayName("should throw exception when subtracting money with different currencies")
        void shouldThrowExceptionWhenSubtractingDifferentCurrencies() {
            Money usd = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money krw = Money.of(BigDecimal.valueOf(50), Currency.KRW);

            assertThatThrownBy(() -> usd.subtract(krw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("should throw exception when subtraction results in negative amount")
        void shouldThrowExceptionWhenSubtractionResultsInNegative() {
            Money a = Money.of(BigDecimal.valueOf(50), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(100), Currency.USD);

            assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("should multiply money by a factor")
        void shouldMultiplyMoneyByFactor() {
            Money money = Money.of(BigDecimal.valueOf(100), Currency.USD);

            Money result = money.multiply(BigDecimal.valueOf(3));

            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(300));
            assertThat(result.currency()).isEqualTo(Currency.USD);
        }

        @Test
        @DisplayName("should multiply and apply decimal rounding")
        void shouldMultiplyAndApplyDecimalRounding() {
            Money money = Money.of(new BigDecimal("10.00"), Currency.USD);

            Money result = money.multiply(new BigDecimal("0.333"));

            // 10.00 * 0.333 = 3.33 (rounded to 2 decimal places)
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("3.33"));
        }

        @Test
        @DisplayName("should throw exception when multiplying by negative factor")
        void shouldThrowExceptionWhenMultiplyingByNegativeFactor() {
            Money money = Money.of(BigDecimal.valueOf(100), Currency.USD);

            assertThatThrownBy(() -> money.multiply(BigDecimal.valueOf(-2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
        }
    }

    @Nested
    @DisplayName("comparison")
    class Comparison {

        @Test
        @DisplayName("should return true when money is greater than other")
        void shouldReturnTrueWhenGreaterThan() {
            Money a = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(50), Currency.USD);

            assertThat(a.isGreaterThan(b)).isTrue();
            assertThat(b.isGreaterThan(a)).isFalse();
        }

        @Test
        @DisplayName("should return false when amounts are equal for isGreaterThan")
        void shouldReturnFalseWhenEqualForGreaterThan() {
            Money a = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(100), Currency.USD);

            assertThat(a.isGreaterThan(b)).isFalse();
        }

        @Test
        @DisplayName("should return true when money is less than other")
        void shouldReturnTrueWhenLessThan() {
            Money a = Money.of(BigDecimal.valueOf(50), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(100), Currency.USD);

            assertThat(a.isLessThan(b)).isTrue();
            assertThat(b.isLessThan(a)).isFalse();
        }

        @Test
        @DisplayName("should return false when amounts are equal for isLessThan")
        void shouldReturnFalseWhenEqualForLessThan() {
            Money a = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(100), Currency.USD);

            assertThat(a.isLessThan(b)).isFalse();
        }

        @Test
        @DisplayName("should throw exception when comparing money with different currencies")
        void shouldThrowExceptionWhenComparingDifferentCurrencies() {
            Money usd = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money krw = Money.of(BigDecimal.valueOf(100), Currency.KRW);

            assertThatThrownBy(() -> usd.isGreaterThan(krw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");

            assertThatThrownBy(() -> usd.isLessThan(krw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("should return true when amount is zero")
        void shouldReturnTrueWhenZero() {
            Money zero = Money.zero(Currency.USD);

            assertThat(zero.isZero()).isTrue();
        }

        @Test
        @DisplayName("should return false when amount is not zero")
        void shouldReturnFalseWhenNotZero() {
            Money money = Money.of(BigDecimal.valueOf(100), Currency.USD);

            assertThat(money.isZero()).isFalse();
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("should be equal when amount and currency are same")
        void shouldBeEqualWhenAmountAndCurrencyAreSame() {
            Money a = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(100), Currency.USD);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("should not be equal when amounts differ")
        void shouldNotBeEqualWhenAmountsDiffer() {
            Money a = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(200), Currency.USD);

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("should not be equal when currencies differ")
        void shouldNotBeEqualWhenCurrenciesDiffer() {
            Money a = Money.of(BigDecimal.valueOf(100), Currency.USD);
            Money b = Money.of(BigDecimal.valueOf(100), Currency.EUR);

            assertThat(a).isNotEqualTo(b);
        }
    }
}
