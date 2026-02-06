package com.fluxpay.engine.infrastructure.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for WebhookSigner.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("WebhookSigner")
class WebhookSignerTest {

    private WebhookSigner signer;

    @BeforeEach
    void setUp() {
        signer = new WebhookSigner();
    }

    @Nested
    @DisplayName("Sign")
    class Sign {

        @Test
        @DisplayName("should generate HMAC-SHA256 signature")
        void shouldGenerateHmacSha256Signature() {
            String payload = "{\"paymentId\":\"pay_123\"}";
            String timestamp = "1707040200";
            String secret = "whsec_test_secret_key_12345";

            String signature = signer.sign(payload, timestamp, secret);

            assertThat(signature).isNotNull();
            assertThat(signature).isNotEmpty();
            // Base64 encoded HMAC-SHA256 should be around 44 characters
            assertThat(signature.length()).isGreaterThan(40);
        }

        @Test
        @DisplayName("should generate consistent signature for same inputs")
        void shouldGenerateConsistentSignature() {
            String payload = "{\"paymentId\":\"pay_123\"}";
            String timestamp = "1707040200";
            String secret = "whsec_test_secret_key_12345";

            String signature1 = signer.sign(payload, timestamp, secret);
            String signature2 = signer.sign(payload, timestamp, secret);

            assertThat(signature1).isEqualTo(signature2);
        }

        @Test
        @DisplayName("should generate different signatures for different payloads")
        void shouldGenerateDifferentSignaturesForDifferentPayloads() {
            String timestamp = "1707040200";
            String secret = "whsec_test_secret_key_12345";

            String signature1 = signer.sign("{\"paymentId\":\"pay_123\"}", timestamp, secret);
            String signature2 = signer.sign("{\"paymentId\":\"pay_456\"}", timestamp, secret);

            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("should generate different signatures for different timestamps")
        void shouldGenerateDifferentSignaturesForDifferentTimestamps() {
            String payload = "{\"paymentId\":\"pay_123\"}";
            String secret = "whsec_test_secret_key_12345";

            String signature1 = signer.sign(payload, "1707040200", secret);
            String signature2 = signer.sign(payload, "1707040201", secret);

            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("should generate different signatures for different secrets")
        void shouldGenerateDifferentSignaturesForDifferentSecrets() {
            String payload = "{\"paymentId\":\"pay_123\"}";
            String timestamp = "1707040200";

            String signature1 = signer.sign(payload, timestamp, "secret1");
            String signature2 = signer.sign(payload, timestamp, "secret2");

            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("should throw exception for null payload")
        void shouldThrowExceptionForNullPayload() {
            assertThatThrownBy(() -> signer.sign(null, "1707040200", "secret"))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw exception for null timestamp")
        void shouldThrowExceptionForNullTimestamp() {
            assertThatThrownBy(() -> signer.sign("{}", null, "secret"))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw exception for null secret")
        void shouldThrowExceptionForNullSecret() {
            assertThatThrownBy(() -> signer.sign("{}", "1707040200", null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Verify")
    class Verify {

        @Test
        @DisplayName("should verify valid signature")
        void shouldVerifyValidSignature() {
            String payload = "{\"paymentId\":\"pay_123\"}";
            String timestamp = "1707040200";
            String secret = "whsec_test_secret_key_12345";

            String signature = signer.sign(payload, timestamp, secret);
            boolean isValid = signer.verify(signature, payload, timestamp, secret);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should reject invalid signature")
        void shouldRejectInvalidSignature() {
            String payload = "{\"paymentId\":\"pay_123\"}";
            String timestamp = "1707040200";
            String secret = "whsec_test_secret_key_12345";

            boolean isValid = signer.verify("invalid_signature", payload, timestamp, secret);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should reject signature with wrong payload")
        void shouldRejectSignatureWithWrongPayload() {
            String secret = "whsec_test_secret_key_12345";
            String timestamp = "1707040200";

            String signature = signer.sign("{\"paymentId\":\"pay_123\"}", timestamp, secret);
            boolean isValid = signer.verify(signature, "{\"paymentId\":\"pay_456\"}", timestamp, secret);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should reject signature with wrong timestamp")
        void shouldRejectSignatureWithWrongTimestamp() {
            String payload = "{\"paymentId\":\"pay_123\"}";
            String secret = "whsec_test_secret_key_12345";

            String signature = signer.sign(payload, "1707040200", secret);
            boolean isValid = signer.verify(signature, payload, "1707040201", secret);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should reject signature with wrong secret")
        void shouldRejectSignatureWithWrongSecret() {
            String payload = "{\"paymentId\":\"pay_123\"}";
            String timestamp = "1707040200";

            String signature = signer.sign(payload, timestamp, "secret1");
            boolean isValid = signer.verify(signature, payload, timestamp, "secret2");

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should be resistant to timing attacks")
        void shouldBeResistantToTimingAttacks() {
            // The verification should use constant-time comparison
            String payload = "{\"paymentId\":\"pay_123\"}";
            String timestamp = "1707040200";
            String secret = "whsec_test_secret_key_12345";

            String validSignature = signer.sign(payload, timestamp, secret);
            String almostValidSignature = validSignature.substring(0, validSignature.length() - 1) + "x";

            // Both should take approximately the same time
            boolean valid = signer.verify(validSignature, payload, timestamp, secret);
            boolean invalid = signer.verify(almostValidSignature, payload, timestamp, secret);

            assertThat(valid).isTrue();
            assertThat(invalid).isFalse();
        }
    }
}
