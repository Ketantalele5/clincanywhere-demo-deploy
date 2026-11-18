package com.clincanywhere.repository;

import java.util.Optional;
import com.clincanywhere.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.clincanywhere.dto.MessageContact;
import org.springframework.data.domain.Pageable;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByRole(String role);
    long countByRole(String role);
    java.util.List<UserAccount> findAllByRoleOrderByUsername(String role);

    @Query("select distinct a from UserAccount a join a.clinicIds clinic where clinic = :clinicId order by a.username")
    java.util.List<UserAccount> findClinicAccounts(@Param("clinicId") String clinicId);

    @Query("select case when count(a) > 0 then true else false end from UserAccount a join a.clinicIds clinic where a.username = :username and clinic = :clinicId")
    boolean existsMembership(@Param("username") String username, @Param("clinicId") String clinicId);

    @Query("select a.role from UserAccount a where a.username = :username")
    Optional<String> findRoleByUsername(@Param("username") String username);

    @Query("select new com.clincanywhere.dto.MessageContact(a.username, a.role) from UserAccount a join a.clinicIds clinic where clinic = :clinicId and a.username <> :username and (:currentRole <> 'PATIENT' or a.role in ('DOCTOR', 'ADMIN')) and (a.role <> 'PATIENT' or :currentRole in ('DOCTOR', 'ADMIN', 'PLATFORM_OWNER')) order by a.username")
    java.util.List<MessageContact> findMessageContacts(@Param("clinicId") String clinicId, @Param("username") String username,
                                                        @Param("currentRole") String currentRole);

    @Query("select new com.clincanywhere.dto.MessageContact(a.username, a.role) from UserAccount a join a.clinicIds clinic where clinic = :clinicId and a.username <> :username and (:currentRole <> 'PATIENT' or a.role in ('DOCTOR', 'ADMIN')) and (a.role <> 'PATIENT' or :currentRole in ('DOCTOR', 'ADMIN', 'PLATFORM_OWNER')) and lower(a.username) like lower(concat('%', :search, '%')) order by a.username")
    java.util.List<MessageContact> searchMessageContacts(@Param("clinicId") String clinicId, @Param("username") String username,
                                                          @Param("currentRole") String currentRole, @Param("search") String search,
                                                          Pageable pageable);
}
