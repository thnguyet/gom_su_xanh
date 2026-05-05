package com.gomsu.workshopservice.repository;

import com.gomsu.workshopservice.entity.Workshop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {
    @Query("SELECT w FROM Workshop w WHERE " +
            "(:activeParam IS NULL OR w.active = :activeParam) AND " +
            "(:isAdmin IS TRUE OR w.active IS TRUE) AND " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR CONCAT(w.id, '') LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:location IS NULL OR LOWER(w.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:minPrice IS NULL OR w.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR w.price <= :maxPrice) AND " +
            "(:fromDate IS NULL OR w.startDate >= :fromDate) AND " +
            "(:toDate IS NULL OR w.endDate <= :toDate)")
    Page<Workshop> findAllForUserAndAdmin(
            String keyword, String location,
            Double minPrice, Double maxPrice,
            LocalDateTime fromDate, LocalDateTime toDate,
            Boolean isAdmin,
            Boolean activeParam, // <-- Thêm mới
            Pageable pageable);

    // Tru ve khi dang ki thanh cong
    @Modifying
    @Transactional
    @Query("UPDATE Workshop w SET w.currentParticipants = w.currentParticipants + :quantity " +
            "WHERE w.id = :id AND (w.currentParticipants + :quantity) <= w.maxParticipants")
    int updateParticipants(@Param("id") Long id, @Param("quantity") Integer quantity);

    // Cong lai ve khi huy ve
    @Modifying
    @Transactional
    @Query("UPDATE Workshop w SET w.currentParticipants = w.currentParticipants - :quantity " +
            "WHERE w.id = :id AND w.currentParticipants >= :quantity")
    int decreaseParticipants(@Param("id") Long id, @Param("quantity") Integer quantity);

    Optional<Workshop> findBySlug(String slug);
}
