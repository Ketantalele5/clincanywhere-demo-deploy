package com.clincanywhere.repository;

import com.clincanywhere.entity.ClinicLead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicLeadRepository extends JpaRepository<ClinicLead, Long> {
    java.util.List<ClinicLead> findAllByOrderByCreatedAtDesc();
    java.util.List<ClinicLead> findAllByAssignedToOrCreatedByOrderByCreatedAtDesc(String assignedTo, String createdBy);
}
