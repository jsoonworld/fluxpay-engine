package com.fluxpay.engine.domain.model.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentMethod")
class PaymentMethodTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create PaymentMethod with type and displayName")
        void shouldCreatePaymentMethodWithTypeAndDisplayName() {
            PaymentMethod method = new PaymentMethod(PaymentMethodType.CARD, "Visa ending in 1234");

            assertThat(method.type()).isEqualTo(PaymentMethodType.CARD);
            assertThat(method.displayName()).isEqualTo("Visa ending in 1234");
        }

        @Test
        @DisplayName("should create PaymentMethod with nullable displayName")
        void shouldCreatePaymentMethodWithNullableDisplayName() {
            PaymentMethod method = new PaymentMethod(PaymentMethodType.CARD, null);

            assertThat(method.type()).isEqualTo(PaymentMethodType.CARD);
            assertThat(method.displayName()).isNull();
        }

        @Test
        @DisplayName("should throw exception when type is null")
        void shouldThrowExceptionWhenTypeIsNull() {
            assertThatThrownBy(() -> new PaymentMethod(null, "Display Name"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
        }
    }

    @Nested
    @DisplayName("static factory methods")
    class StaticFactoryMethods {

        @Test
        @DisplayName("card() should create PaymentMethod with CARD type")
        void cardShouldCreatePaymentMethodWithCardType() {
            PaymentMethod method = PaymentMethod.card();

            assertThat(method.type()).isEqualTo(PaymentMethodType.CARD);
            assertThat(method.displayName()).isNull();
        }

        @Test
        @DisplayName("card(displayName) should create PaymentMethod with CARD type and displayName")
        void cardWithDisplayNameShouldCreatePaymentMethodWithCardTypeAndDisplayName() {
            PaymentMethod method = PaymentMethod.card("Visa ending in 4242");

            assertThat(method.type()).isEqualTo(PaymentMethodType.CARD);
            assertThat(method.displayName()).isEqualTo("Visa ending in 4242");
        }

        @Test
        @DisplayName("bankTransfer() should create PaymentMethod with BANK_TRANSFER type")
        void bankTransferShouldCreatePaymentMethodWithBankTransferType() {
            PaymentMethod method = PaymentMethod.bankTransfer();

            assertThat(method.type()).isEqualTo(PaymentMethodType.BANK_TRANSFER);
            assertThat(method.displayName()).isNull();
        }

        @Test
        @DisplayName("bankTransfer(displayName) should create PaymentMethod with BANK_TRANSFER type and displayName")
        void bankTransferWithDisplayNameShouldCreatePaymentMethodWithBankTransferTypeAndDisplayName() {
            PaymentMethod method = PaymentMethod.bankTransfer("KB Kookmin Bank");

            assertThat(method.type()).isEqualTo(PaymentMethodType.BANK_TRANSFER);
            assertThat(method.displayName()).isEqualTo("KB Kookmin Bank");
        }

        @Test
        @DisplayName("virtualAccount() should create PaymentMethod with VIRTUAL_ACCOUNT type")
        void virtualAccountShouldCreatePaymentMethodWithVirtualAccountType() {
            PaymentMethod method = PaymentMethod.virtualAccount();

            assertThat(method.type()).isEqualTo(PaymentMethodType.VIRTUAL_ACCOUNT);
            assertThat(method.displayName()).isNull();
        }

        @Test
        @DisplayName("virtualAccount(displayName) should create PaymentMethod with VIRTUAL_ACCOUNT type and displayName")
        void virtualAccountWithDisplayNameShouldCreatePaymentMethodWithVirtualAccountTypeAndDisplayName() {
            PaymentMethod method = PaymentMethod.virtualAccount("Shinhan Virtual Account");

            assertThat(method.type()).isEqualTo(PaymentMethodType.VIRTUAL_ACCOUNT);
            assertThat(method.displayName()).isEqualTo("Shinhan Virtual Account");
        }

        @Test
        @DisplayName("mobile() should create PaymentMethod with MOBILE type")
        void mobileShouldCreatePaymentMethodWithMobileType() {
            PaymentMethod method = PaymentMethod.mobile();

            assertThat(method.type()).isEqualTo(PaymentMethodType.MOBILE);
            assertThat(method.displayName()).isNull();
        }

        @Test
        @DisplayName("mobile(displayName) should create PaymentMethod with MOBILE type and displayName")
        void mobileWithDisplayNameShouldCreatePaymentMethodWithMobileTypeAndDisplayName() {
            PaymentMethod method = PaymentMethod.mobile("SKT Carrier Billing");

            assertThat(method.type()).isEqualTo(PaymentMethodType.MOBILE);
            assertThat(method.displayName()).isEqualTo("SKT Carrier Billing");
        }

        @Test
        @DisplayName("easyPay(provider) should create PaymentMethod with EASY_PAY type and provider as displayName")
        void easyPayShouldCreatePaymentMethodWithEasyPayTypeAndProvider() {
            PaymentMethod method = PaymentMethod.easyPay("Kakao Pay");

            assertThat(method.type()).isEqualTo(PaymentMethodType.EASY_PAY);
            assertThat(method.displayName()).isEqualTo("Kakao Pay");
        }

        @Test
        @DisplayName("easyPay(null) should throw exception")
        void easyPayWithNullShouldThrowException() {
            assertThatThrownBy(() -> PaymentMethod.easyPay(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("provider");
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("should be equal when type and displayName are same")
        void shouldBeEqualWhenTypeAndDisplayNameAreSame() {
            PaymentMethod method1 = PaymentMethod.card("Visa");
            PaymentMethod method2 = PaymentMethod.card("Visa");

            assertThat(method1).isEqualTo(method2);
            assertThat(method1.hashCode()).isEqualTo(method2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when types differ")
        void shouldNotBeEqualWhenTypesDiffer() {
            PaymentMethod method1 = PaymentMethod.card();
            PaymentMethod method2 = PaymentMethod.bankTransfer();

            assertThat(method1).isNotEqualTo(method2);
        }

        @Test
        @DisplayName("should not be equal when displayNames differ")
        void shouldNotBeEqualWhenDisplayNamesDiffer() {
            PaymentMethod method1 = PaymentMethod.card("Visa");
            PaymentMethod method2 = PaymentMethod.card("MasterCard");

            assertThat(method1).isNotEqualTo(method2);
        }

        @Test
        @DisplayName("should be equal when both displayNames are null")
        void shouldBeEqualWhenBothDisplayNamesAreNull() {
            PaymentMethod method1 = PaymentMethod.card();
            PaymentMethod method2 = PaymentMethod.card();

            assertThat(method1).isEqualTo(method2);
            assertThat(method1.hashCode()).isEqualTo(method2.hashCode());
        }
    }
}
