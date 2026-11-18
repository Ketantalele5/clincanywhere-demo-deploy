package com.clincanywhere.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import com.clincanywhere.entity.BillingPayment;

public interface BillingPaymentRepository extends JpaRepository<BillingPayment, Long> {
    List<BillingPayment> findByClinicIdAndInvoiceRecordIdOrderByIdDesc(String clinicId, long invoiceRecordId);

    List<BillingPayment> findByClinicIdAndStatusInOrderByIdDesc(String clinicId, List<String> statuses);

    Optional<BillingPayment> findFirstByClinicIdAndInvoiceRecordIdAndStatusInOrderByIdDesc(
            String clinicId, long invoiceRecordId, List<String> statuses);

    Optional<BillingPayment> findByGatewayOrderId(String gatewayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BillingPayment p where p.clinicId=:clinic and p.gatewayOrderId=:orderId")
    Optional<BillingPayment> lockByClinicAndGatewayOrderId(@Param("clinic") String clinicId,
                                                            @Param("orderId") String orderId);

    boolean existsByClinicIdAndInvoiceRecordId(String clinicId, long invoiceRecordId);
}
