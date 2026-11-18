package com.clincanywhere.repository;
import com.clincanywhere.entity.PatientNotificationItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PatientNotificationRepository extends JpaRepository<PatientNotificationItem,Long>{java.util.List<PatientNotificationItem> findTop100ByClinicIdOrderByIdDesc(String clinicId);}
