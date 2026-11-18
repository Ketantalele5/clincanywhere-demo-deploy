package com.clincanywhere.repository;

import com.clincanywhere.entity.DirectMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    @Query("select m from DirectMessage m where m.clinicId = :clinic and m.recipient = :recipient order by m.id desc")
    List<DirectMessage> findByClinicAndRecipient(@Param("clinic") String clinic, @Param("recipient") String recipient, Pageable pageable);
    @Query("select m from DirectMessage m where m.clinicId = :clinic and ((m.sender = :a and m.recipient = :b) or (m.sender = :b and m.recipient = :a)) order by m.id desc")
    List<DirectMessage> findConversation(@Param("clinic") String clinic, @Param("a") String a,
                                         @Param("b") String b, Pageable pageable);
}
