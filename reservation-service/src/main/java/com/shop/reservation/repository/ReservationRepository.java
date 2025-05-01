package com.shop.reservation.repository;

import com.shop.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {
    List<Reservation> findByUserIdAndStatus(String userId, Reservation.ReservationStatus status);

    List<Reservation> findByCartId(String cartId);

    @Query("SELECT r FROM Reservation r WHERE r.expiresAt <= :now AND r.status = 'ACTIVE'")
    List<Reservation> findExpiredReservations(@Param("now") LocalDateTime now);

    List<Reservation> findByProductIdAndStatus(String productId, Reservation.ReservationStatus status);
}

