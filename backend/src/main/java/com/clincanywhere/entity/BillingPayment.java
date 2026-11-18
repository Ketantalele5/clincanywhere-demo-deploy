package com.clincanywhere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One cash receipt or one Razorpay payment attempt for a clinic invoice. */
@Entity
@Table(name = "billing_payments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_billing_payment_gateway_order", columnNames = "gateway_order_id"),
        @UniqueConstraint(name = "uq_billing_payment_gateway_payment", columnNames = "gateway_payment_id")
})
public class BillingPayment extends BaseEntity {
    @Column(name = "clinic_id", nullable = false, length = 40)
    private String clinicId;

    @Column(name = "invoice_record_id", nullable = false)
    private long invoiceRecordId;

    @Column(name = "invoice_external_id", nullable = false, length = 80)
    private String invoiceExternalId;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 16)
    private String method;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "gateway_order_id", length = 80)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 80)
    private String gatewayPaymentId;

    @Column(name = "gateway_receipt", length = 40)
    private String gatewayReceipt;

    protected BillingPayment() { }

    public BillingPayment(String clinicId, long invoiceRecordId, String invoiceExternalId,
                          long amountPaise, String currency, String method, String status,
                          String createdBy, String gatewayReceipt) {
        this.clinicId = clinicId;
        this.invoiceRecordId = invoiceRecordId;
        this.invoiceExternalId = invoiceExternalId;
        this.amountPaise = amountPaise;
        this.currency = currency;
        this.method = method;
        this.status = status;
        this.createdBy = createdBy;
        this.gatewayReceipt = gatewayReceipt;
    }

    public String getClinicId() { return clinicId; }
    public long getInvoiceRecordId() { return invoiceRecordId; }
    public String getInvoiceExternalId() { return invoiceExternalId; }
    public long getAmountPaise() { return amountPaise; }
    public String getCurrency() { return currency; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public String getGatewayOrderId() { return gatewayOrderId; }
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public String getGatewayReceipt() { return gatewayReceipt; }

    public void attachGatewayOrder(String orderId) {
        this.gatewayOrderId = orderId;
        this.status = "PENDING";
    }

    public void failGatewayAttempt(String paymentId) {
        if (paymentId != null && !paymentId.isBlank()) this.gatewayPaymentId = paymentId;
        if (!"COMPLETED".equals(status) && !"REVIEW_REQUIRED".equals(status)) this.status = "FAILED";
    }

    public void completeGatewayPayment(String paymentId, boolean reviewRequired) {
        if (paymentId != null && !paymentId.isBlank()) this.gatewayPaymentId = paymentId;
        this.status = reviewRequired ? "REVIEW_REQUIRED" : "COMPLETED";
    }
}
