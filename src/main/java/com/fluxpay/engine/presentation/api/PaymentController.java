package com.fluxpay.engine.presentation.api;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.fluxpay.engine.domain.model.payment.PaymentMethodType;
import com.fluxpay.engine.domain.service.PaymentService;
import com.fluxpay.engine.presentation.dto.ApiResponse;
import com.fluxpay.engine.presentation.dto.request.ApprovePaymentRequest;
import com.fluxpay.engine.presentation.dto.request.CreatePaymentRequest;
import com.fluxpay.engine.presentation.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for Payment operations.
 * Provides endpoints for creating, approving, confirming, and retrieving payments.
 */
@Tag(name = "Payments", description = "Payment management API")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Creates a new payment for an order.
     *
     * @param request the payment creation request
     * @return a Mono containing the created payment response
     */
    @Operation(summary = "Create payment", description = "Creates a new payment for an order")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<PaymentResponse>> createPayment(
        @Valid @RequestBody CreatePaymentRequest request
    ) {
        log.info("Creating payment for order: {}", request.orderId());

        Currency currency = Currency.valueOf(request.currency());
        Money amount = Money.of(request.amount(), currency);

        return paymentService.createPayment(OrderId.of(request.orderId()), amount)
            .map(payment -> ApiResponse.success(PaymentResponse.from(payment)));
    }

    /**
     * Requests approval for a payment.
     *
     * @param paymentId the payment ID
     * @param request the approval request containing payment method
     * @return a Mono containing the approved payment response
     */
    @Operation(summary = "Approve payment", description = "Requests PG approval for a payment")
    @PostMapping("/{paymentId}/approve")
    public Mono<ApiResponse<PaymentResponse>> approvePayment(
        @Parameter(description = "Payment ID") @PathVariable String paymentId,
        @Valid @RequestBody ApprovePaymentRequest request
    ) {
        log.info("Requesting approval for payment: {}", paymentId);

        PaymentMethod method = createPaymentMethod(request);

        return paymentService.requestApproval(PaymentId.of(paymentId), method)
            .map(payment -> ApiResponse.success(PaymentResponse.from(payment)));
    }

    /**
     * Confirms a payment (captures the funds).
     *
     * @param paymentId the payment ID
     * @return a Mono containing the confirmed payment response
     */
    @Operation(summary = "Confirm payment", description = "Confirms a payment after approval")
    @PostMapping("/{paymentId}/confirm")
    public Mono<ApiResponse<PaymentResponse>> confirmPayment(
        @Parameter(description = "Payment ID") @PathVariable String paymentId
    ) {
        log.info("Confirming payment: {}", paymentId);

        return paymentService.confirmPayment(PaymentId.of(paymentId))
            .map(payment -> ApiResponse.success(PaymentResponse.from(payment)));
    }

    /**
     * Gets a payment by ID.
     *
     * @param paymentId the payment ID
     * @return a Mono containing the payment response
     */
    @Operation(summary = "Get payment", description = "Retrieves a payment by its ID")
    @GetMapping("/{paymentId}")
    public Mono<ApiResponse<PaymentResponse>> getPayment(
        @Parameter(description = "Payment ID") @PathVariable String paymentId
    ) {
        log.info("Getting payment: {}", paymentId);

        return paymentService.getPayment(PaymentId.of(paymentId))
            .map(payment -> ApiResponse.success(PaymentResponse.from(payment)));
    }

    /**
     * Creates a PaymentMethod from the approval request.
     *
     * @param request the approval request
     * @return the payment method
     */
    private PaymentMethod createPaymentMethod(ApprovePaymentRequest request) {
        PaymentMethodType type = PaymentMethodType.valueOf(request.paymentMethod());

        return switch (type) {
            case CARD -> PaymentMethod.card();
            case BANK_TRANSFER -> PaymentMethod.bankTransfer();
            case VIRTUAL_ACCOUNT -> PaymentMethod.virtualAccount();
            case MOBILE -> PaymentMethod.mobile();
            case EASY_PAY -> PaymentMethod.easyPay(request.easyPayProvider());
        };
    }
}
