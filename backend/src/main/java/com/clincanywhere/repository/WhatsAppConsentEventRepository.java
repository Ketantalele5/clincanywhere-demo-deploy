package com.clincanywhere.repository;
import com.clincanywhere.entity.WhatsAppConsentEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WhatsAppConsentEventRepository extends JpaRepository<WhatsAppConsentEvent,Long> {
    Optional<WhatsAppConsentEvent> findTopByClinicIdAndPhoneFingerprintOrderByIdDesc(String clinicId,String phoneFingerprint);
}
