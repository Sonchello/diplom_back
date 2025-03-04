package com.example.platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.platform.model.HelpHistory;

@Repository
public interface HelpHistoryRepository extends JpaRepository<HelpHistory, Long> {
    @Query("SELECT h FROM HelpHistory h WHERE h.helper.id = :userId AND h.status = :status AND h.endDate IS NULL")
    List<HelpHistory> findActiveHelpsByUserId(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT h FROM HelpHistory h WHERE h.helper.id = :userId AND h.status = 'COMPLETED'")
    List<HelpHistory> findCompletedHelpsByUserId(@Param("userId") Long userId);

    List<HelpHistory> findByRequestId(Long requestId);

    @Query("SELECT COUNT(h) > 0 FROM HelpHistory h WHERE h.request.id = :requestId AND h.helper.id = :helperId AND h.status = 'IN_PROGRESS'")
    boolean existsByRequestIdAndHelperId(@Param("requestId") Long requestId, @Param("helperId") Long helperId);

    @Query("SELECT h FROM HelpHistory h WHERE h.request.id = :requestId AND h.helper.id = :helperId AND h.status = 'IN_PROGRESS'")
    List<HelpHistory> findActiveHelpsByRequestAndHelper(@Param("requestId") Long requestId, @Param("helperId") Long helperId);

    @Query("SELECT h FROM HelpHistory h WHERE h.request.id = :requestId AND h.helper.id = :helperId ORDER BY h.startDate DESC")
    List<HelpHistory> findByRequestIdAndHelperIdOrderByStartDateDesc(@Param("requestId") Long requestId, @Param("helperId") Long helperId);

    @Query("SELECT h FROM HelpHistory h WHERE h.request.id = :requestId AND h.status = :status")
    List<HelpHistory> findByRequestIdAndStatus(@Param("requestId") Long requestId, @Param("status") String status);
}