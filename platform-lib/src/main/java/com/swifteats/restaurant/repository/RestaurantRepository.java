package com.swifteats.restaurant.repository;

import com.swifteats.common.domain.RestaurantStatus;
import com.swifteats.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    @Query("""
            SELECT DISTINCT r FROM Restaurant r
            LEFT JOIN r.cuisines c
            WHERE r.status = :activeStatus
              AND (:city IS NULL OR r.city = :city)
              AND (:cuisine IS NULL OR c.name = :cuisine)
              AND (:isOpen IS NULL OR r.open = :isOpen)
              AND (:minRating IS NULL OR r.rating >= :minRating)
              AND (:namePattern IS NULL OR LOWER(r.name) LIKE :namePattern)
            """)
    Page<Restaurant> searchActive(
            @Param("activeStatus") RestaurantStatus activeStatus,
            @Param("city") String city,
            @Param("cuisine") String cuisine,
            @Param("isOpen") Boolean isOpen,
            @Param("minRating") BigDecimal minRating,
            @Param("namePattern") String namePattern,
            Pageable pageable);

    @Query("""
            SELECT r FROM Restaurant r
            LEFT JOIN FETCH r.menuItems
            WHERE r.id = :id AND r.status = :activeStatus
            """)
    Optional<Restaurant> findActiveWithMenu(
            @Param("id") UUID id,
            @Param("activeStatus") RestaurantStatus activeStatus);

    @Query("""
            SELECT r FROM Restaurant r
            WHERE (:status IS NULL OR r.status = :status)
            ORDER BY r.name ASC
            """)
    List<Restaurant> findForAdmin(@Param("status") RestaurantStatus status);
}
