package com.shop.queue.repository;

import com.shop.queue.entity.QueueRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueueRequestRepository extends JpaRepository<QueueRequest, String> {
    List<QueueRequest> findByProductIdAndStatusOrderByCreatedAtAsc(String productId, QueueRequest.QueueRequestStatus status);

    Page<QueueRequest> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<QueueRequest> findByUserIdAndProductIdAndStatusIn(
            String userId,
            String productId,
            List<QueueRequest.QueueRequestStatus> statuses);

    @Query("SELECT MIN(qr.position) FROM QueueRequest qr WHERE qr.productId = :productId AND qr.status = 'PENDING'")
    Integer findMinPositionByProductId(@Param("productId") String productId);

    @Query("SELECT COUNT(qr) FROM QueueRequest qr WHERE qr.productId = :productId AND qr.status = 'PENDING'")
    Integer countPendingRequestsByProductId(@Param("productId") String productId);

    @Query("SELECT qr FROM QueueRequest qr WHERE qr.productId = :productId AND qr.status = 'PENDING' ORDER BY qr.position ASC")
    List<QueueRequest> findPendingRequestsByProductIdOrderByPosition(@Param("productId") String productId);
}
