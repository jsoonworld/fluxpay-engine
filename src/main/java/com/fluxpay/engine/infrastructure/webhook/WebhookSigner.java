package com.fluxpay.engine.infrastructure.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

/**
 * Handles HMAC-SHA256 signing and verification for webhooks.
 * Uses constant-time comparison to prevent timing attacks.
 */
@Component
public class WebhookSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Generates an HMAC-SHA256 signature for the given payload.
     * The signature is computed over: timestamp + "." + payload
     *
     * @param payload the JSON payload to sign
     * @param timestamp the timestamp (Unix epoch seconds as string)
     * @param secret the signing secret
     * @return Base64-encoded HMAC-SHA256 signature
     * @throws NullPointerException if any parameter is null
     * @throws WebhookSignatureException if signing fails
     */
    public String sign(String payload, String timestamp, String secret) {
        Objects.requireNonNull(payload, "Payload is required");
        Objects.requireNonNull(timestamp, "Timestamp is required");
        Objects.requireNonNull(secret, "Secret is required");

        String message = timestamp + "." + payload;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookSignatureException("Failed to generate signature", e);
        }
    }

    /**
     * Verifies an HMAC-SHA256 signature against the given payload.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param signature the signature to verify
     * @param payload the JSON payload
     * @param timestamp the timestamp
     * @param secret the signing secret
     * @return true if the signature is valid, false otherwise
     * @throws NullPointerException if any parameter is null
     */
    public boolean verify(String signature, String payload, String timestamp, String secret) {
        Objects.requireNonNull(signature, "Signature is required");
        Objects.requireNonNull(payload, "Payload is required");
        Objects.requireNonNull(timestamp, "Timestamp is required");
        Objects.requireNonNull(secret, "Secret is required");

        String expected = sign(payload, timestamp, secret);
        return constantTimeEquals(expected.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}
