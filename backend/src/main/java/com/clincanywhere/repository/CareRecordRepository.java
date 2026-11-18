package com.clincanywhere.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import com.clincanywhere.entity.CareRecord;

public interface CareRecordRepository extends JpaRepository<CareRecord, Long> {
    @org.springframework.data.jpa.repository.Query("select distinct r.clinicId from CareRecord r where r.category='settings' and r.externalId='clinic-profile' order by r.clinicId")
    List<String> findAllClinicIds();

    List<CareRecord> findByClinicIdAndCategoryOrderByIdAsc(String clinicId, String category);

    List<CareRecord> findByClinicIdAndCategoryOrderByIdAsc(String clinicId, String category, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("select r from CareRecord r where r.clinicId=:clinic and r.category=:category and (r.searchId=:query or r.searchName=:query or r.searchPhone=:query) order by r.id asc")
    List<CareRecord> searchByClinicAndCategory(@org.springframework.data.repository.query.Param("clinic") String clinic,
                                                @org.springframework.data.repository.query.Param("category") String category,
                                                @org.springframework.data.repository.query.Param("query") String query,
                                                Pageable pageable);

    @org.springframework.data.jpa.repository.Query("select r from CareRecord r where r.searchId='' order by r.id asc")
    List<CareRecord> findRecordsNeedingSearchIndexes(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("select r from CareRecord r where r.searchId='' or r.payloadJson not like 'enc:v1:%' order by r.id asc")
    List<CareRecord> findRecordsNeedingEncryptionOrSearchIndexes(Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from CareRecord r where r.clinicId=:clinic and r.category=:category order by r.id asc")
    List<CareRecord> lockByClinicIdAndCategoryOrderByIdAsc(@org.springframework.data.repository.query.Param("clinic") String clinicId,
                                                            @org.springframework.data.repository.query.Param("category") String category);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from CareRecord r where r.clinicId=:clinic and r.category=:category and r.searchName=:searchName")
    List<CareRecord> lockByClinicCategoryAndSearchName(@org.springframework.data.repository.query.Param("clinic") String clinic,
                                                        @org.springframework.data.repository.query.Param("category") String category,
                                                        @org.springframework.data.repository.query.Param("searchName") String searchName);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from CareRecord r where r.clinicId=:clinic and r.category=:category and r.externalId=:externalId")
    Optional<CareRecord> lockByClinicAndCategoryAndExternalId(@org.springframework.data.repository.query.Param("clinic") String clinic,
                                                               @org.springframework.data.repository.query.Param("category") String category,
                                                               @org.springframework.data.repository.query.Param("externalId") String externalId);

    Optional<CareRecord> findByClinicIdAndCategoryAndExternalId(
            String clinicId, String category, String externalId);

    Optional<CareRecord> findFirstByClinicIdAndCategoryAndExternalId(
            String clinicId, String category, String externalId);

    boolean existsByClinicIdAndCategoryAndExternalId(String clinicId, String category, String externalId);

    boolean existsByClinicId(String clinicId);

    void deleteByClinicId(String clinicId);

    void deleteByClinicIdAndCategoryAndExternalId(String clinicId, String category, String externalId);
}
