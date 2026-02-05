package com.fluxpay.engine.domain.model.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentMethodType")
class PaymentMethodTypeTest {

    @Nested
    @DisplayName("enum values")
    class EnumValues {

        @Test
        @DisplayName("should have exactly 5 payment method types")
        void shouldHaveExactlyFivePaymentMethodTypes() {
            assertThat(PaymentMethodType.values()).hasSize(5);
        }

        @Test
        @DisplayName("should contain all expected payment method types")
        void shouldContainAllExpectedPaymentMethodTypes() {
            assertThat(PaymentMethodType.values())
                .containsExactlyInAnyOrder(
                    PaymentMethodType.CARD,
                    PaymentMethodType.BANK_TRANSFER,
                    PaymentMethodType.VIRTUAL_ACCOUNT,
                    PaymentMethodType.MOBILE,
                    PaymentMethodType.EASY_PAY
                );
        }

        @Test
        @DisplayName("should have CARD payment method type")
        void shouldHaveCardPaymentMethodType() {
            assertThat(PaymentMethodType.CARD).isNotNull();
            assertThat(PaymentMethodType.CARD.name()).isEqualTo("CARD");
        }

        @Test
        @DisplayName("should have BANK_TRANSFER payment method type")
        void shouldHaveBankTransferPaymentMethodType() {
            assertThat(PaymentMethodType.BANK_TRANSFER).isNotNull();
            assertThat(PaymentMethodType.BANK_TRANSFER.name()).isEqualTo("BANK_TRANSFER");
        }

        @Test
        @DisplayName("should have VIRTUAL_ACCOUNT payment method type")
        void shouldHaveVirtualAccountPaymentMethodType() {
            assertThat(PaymentMethodType.VIRTUAL_ACCOUNT).isNotNull();
            assertThat(PaymentMethodType.VIRTUAL_ACCOUNT.name()).isEqualTo("VIRTUAL_ACCOUNT");
        }

        @Test
        @DisplayName("should have MOBILE payment method type")
        void shouldHaveMobilePaymentMethodType() {
            assertThat(PaymentMethodType.MOBILE).isNotNull();
            assertThat(PaymentMethodType.MOBILE.name()).isEqualTo("MOBILE");
        }

        @Test
        @DisplayName("should have EASY_PAY payment method type")
        void shouldHaveEasyPayPaymentMethodType() {
            assertThat(PaymentMethodType.EASY_PAY).isNotNull();
            assertThat(PaymentMethodType.EASY_PAY.name()).isEqualTo("EASY_PAY");
        }
    }

    @Nested
    @DisplayName("valueOf")
    class ValueOf {

        @Test
        @DisplayName("should return CARD for 'CARD' string")
        void shouldReturnCardForCardString() {
            assertThat(PaymentMethodType.valueOf("CARD")).isEqualTo(PaymentMethodType.CARD);
        }

        @Test
        @DisplayName("should return BANK_TRANSFER for 'BANK_TRANSFER' string")
        void shouldReturnBankTransferForBankTransferString() {
            assertThat(PaymentMethodType.valueOf("BANK_TRANSFER")).isEqualTo(PaymentMethodType.BANK_TRANSFER);
        }

        @Test
        @DisplayName("should return VIRTUAL_ACCOUNT for 'VIRTUAL_ACCOUNT' string")
        void shouldReturnVirtualAccountForVirtualAccountString() {
            assertThat(PaymentMethodType.valueOf("VIRTUAL_ACCOUNT")).isEqualTo(PaymentMethodType.VIRTUAL_ACCOUNT);
        }

        @Test
        @DisplayName("should return MOBILE for 'MOBILE' string")
        void shouldReturnMobileForMobileString() {
            assertThat(PaymentMethodType.valueOf("MOBILE")).isEqualTo(PaymentMethodType.MOBILE);
        }

        @Test
        @DisplayName("should return EASY_PAY for 'EASY_PAY' string")
        void shouldReturnEasyPayForEasyPayString() {
            assertThat(PaymentMethodType.valueOf("EASY_PAY")).isEqualTo(PaymentMethodType.EASY_PAY);
        }
    }
}
