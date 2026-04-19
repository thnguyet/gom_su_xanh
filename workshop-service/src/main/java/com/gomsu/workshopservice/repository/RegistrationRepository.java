package com.gomsu.workshopservice.repository;

import com.gomsu.workshopservice.entity.RegistrationStatus;
import com.gomsu.workshopservice.entity.WorkshopRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RegistrationRepository extends JpaRepository<WorkshopRegistration,Long> {
    @Query("SELECT COALESCE(SUM(r.ticketQuantity), 0) FROM WorkshopRegistration r " +
            "WHERE r.workshop.id = :workshopId AND r.status != 'CANCELLED'")
    Integer countSoldTicketsByWorkshopId(@Param("workshopId") Long workshopId);
    @Query("SELECT r FROM WorkshopRegistration r " +
            "JOIN FETCH r.workshop w " +
            "LEFT JOIN FETCH w.images " +
            "WHERE r.customerId = :userId " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:fromDate IS NULL OR w.startDate >= :fromDate) " +
            "AND (:toDate IS NULL OR w.startDate <= :toDate)")
    Page<WorkshopRegistration> findAllByFilter(
            @Param("userId") Long userId,
            @Param("status") RegistrationStatus status,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
