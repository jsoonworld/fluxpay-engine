package com.fluxpay.engine.domain.model.order;

import com.fluxpay.engine.domain.model.common.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Owned Entity representing a line item within an Order aggregate.
 *
 * <p>This class is immutable and has identity-based equality (compared by its id).
 * As an owned entity, it exists only within the context of its parent Order aggregate
 * and cannot exist independently. All access and modifications should go through
 * the Order aggregate root.</p>
 *
 * <p>Note: Despite being immutable, this is NOT a Value Object because it has
 * a unique identity that persists across its lifecycle.</p>
 */
public final class OrderLineItem {

    private final UUID id;
    private final String productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;
    private final Money totalPrice;

    private OrderLineItem(UUID id, String productId, String productName,
                          int quantity, Money unitPrice, Money totalPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    /**
     * Factory method to create a new OrderLineItem.
     * Automatically calculates totalPrice from unitPrice * quantity.
     *
     * @param productId   the product identifier (must not be null or blank)
     * @param productName the product name (must not be null or blank)
     * @param quantity    the quantity (must be positive)
     * @param unitPrice   the unit price (must not be null)
     * @return a new OrderLineItem instance
     * @throws IllegalArgumentException if productId or productName is null/blank, or quantity <= 0
     * @throws NullPointerException     if unitPrice is null
     */
    public static OrderLineItem create(String productId, String productName,
                                        int quantity, Money unitPrice) {
        validateProductId(productId);
        validateProductName(productName);
        validateQuantity(quantity);
        Objects.requireNonNull(unitPrice, "Unit price is required");

        UUID id = UUID.randomUUID();
        Money totalPrice = unitPrice.multiply(quantity);
        return new OrderLineItem(id, productId, productName, quantity, unitPrice, totalPrice);
    }

    /**
     * Restores an OrderLineItem from persistence.
     * Used when reconstituting an order from the database.
     *
     * @param id          the line item ID
     * @param productId   the product identifier
     * @param productName the product name
     * @param quantity    the quantity
     * @param unitPrice   the unit price
     * @param totalPrice  the total price
     * @return a restored OrderLineItem instance
     */
    public static OrderLineItem restore(UUID id, String productId, String productName,
                                         int quantity, Money unitPrice, Money totalPrice) {
        Objects.requireNonNull(id, "ID is required for restoration");
        validateProductId(productId);
        validateProductName(productName);
        validateQuantity(quantity);
        Objects.requireNonNull(unitPrice, "Unit price is required");
        Objects.requireNonNull(totalPrice, "Total price is required");

        return new OrderLineItem(id, productId, productName, quantity, unitPrice, totalPrice);
    }

    private static void validateProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID is required and cannot be blank");
        }
    }

    private static void validateProductName(String productName) {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name is required and cannot be blank");
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    /**
     * Creates a new OrderLineItem with a different quantity.
     * The totalPrice is recalculated.
     *
     * @param newQuantity the new quantity
     * @return a new OrderLineItem with the updated quantity
     */
    public OrderLineItem withQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        Money newTotalPrice = unitPrice.multiply(newQuantity);
        return new OrderLineItem(id, productId, productName, newQuantity, unitPrice, newTotalPrice);
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderLineItem that = (OrderLineItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OrderLineItem{" +
               "id=" + id +
               ", productId='" + productId + '\'' +
               ", productName='" + productName + '\'' +
               ", quantity=" + quantity +
               ", unitPrice=" + unitPrice +
               ", totalPrice=" + totalPrice +
               '}';
    }
}
