package com.gomsu.workshopservice.repository;

import com.gomsu.workshopservice.entity.RegistrationStatus;
import com.gomsu.workshopservice.entity.WorkshopRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface RegistrationRepository extends JpaRepository<WorkshopRegistration,Long> {
    @Query("SELECT COALESCE(SUM(r.ticketQuantity), 0) FROM WorkshopRegistration r " +
            "WHERE r.workshop.id = :workshopId AND r.status != 'CANCELLED'")
    Integer countSoldTicketsByWorkshopId(@Param("workshopId") Long workshopId);
    @Query(value = "SELECT r FROM WorkshopRegistration r " +
            "JOIN FETCH r.workshop w " +
            "WHERE (:status IS NULL OR r.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:fromDate IS NULL OR r.registrationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR r.registrationDate <= :toDate)",
            countQuery = "SELECT COUNT(r) FROM WorkshopRegistration r JOIN r.workshop w " +
                    "WHERE (:status IS NULL OR r.status = :status) " +
                    "AND (:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "AND (:fromDate IS NULL OR r.registrationDate >= :fromDate) " +
                    "AND (:toDate IS NULL OR r.registrationDate <= :toDate)")
    Page<WorkshopRegistration> findAllAdminByFilter(
            @Param("status") RegistrationStatus status,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT r FROM WorkshopRegistration r " +
            "JOIN FETCH r.workshop w " +
            "LEFT JOIN FETCH w.images " +
            "WHERE r.customerId = :userId " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:fromDate IS NULL OR r.registrationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR r.registrationDate <= :toDate)")
    Page<WorkshopRegistration> findAllByFilter(
            @Param("userId") Long userId,
            @Param("status") RegistrationStatus status,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT r FROM WorkshopRegistration r " +
            "WHERE r.workshop.id = :workshopId " +
            "AND (:status IS NULL OR r.status = :status) " +
            // Chỉ lọc theo customerName vì Nguyệt đã bỏ trường Email
            "AND (:keyword IS NULL OR LOWER(r.customerName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:fromDate IS NULL OR r.registrationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR r.registrationDate <= :toDate)")
    Page<WorkshopRegistration> findAttendeesByFilter(
            @Param("workshopId") Long workshopId,
            @Param("status") RegistrationStatus status,
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT new map(" +
            "w.id as workshopId, " +
            "w.name as workshopName, " +
            "COALESCE(SUM(CASE WHEN r.status = 'COMPLETED' THEN r.ticketQuantity ELSE 0 END), 0) as totalTickets, " +
            "COALESCE(SUM(CASE WHEN r.status = 'COMPLETED' THEN r.totalPrice ELSE 0 END), 0) as totalRevenue) " +
            "FROM Workshop w " +
            "LEFT JOIN WorkshopRegistration r ON w.id = r.workshop.id " +
            "WHERE w.id = :workshopId " +
            "GROUP BY w.id, w.name")
    Map<String, Object> getWorkshopStatsByWorkshopID(@Param("workshopId") Long workshopId);

    @Query("SELECT new map(" +
            "w.id as workshopId, " +
            "w.name as workshopName, " +
            "COALESCE(SUM(CASE WHEN r.status IN ('COMPLETED') THEN r.ticketQuantity ELSE 0 END), 0) as totalTickets, " +
            "COALESCE(SUM(CASE WHEN r.status IN ('COMPLETED') THEN r.totalPrice ELSE 0 END), 0) as totalRevenue) " +
            "FROM Workshop w " +
            "LEFT JOIN WorkshopRegistration r ON w.id = r.workshop.id " +
            "GROUP BY w.id, w.name")
    List<Map<String, Object>> getAllWorkshopStats();
}
