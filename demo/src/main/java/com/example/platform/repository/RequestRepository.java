package com.example.platform.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.platform.model.Request;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    // Переопределяем стандартный метод findById для фильтрации по isArchived
    @Query("SELECT r FROM Request r WHERE r.id = :id AND r.isArchived = false")
    Optional<Request> findById(@Param("id") Long id);

    // Переопределяем стандартный метод findAll для фильтрации по isArchived
    @Query("SELECT r FROM Request r WHERE r.isArchived = false")
    List<Request> findAll();

    @Query("SELECT r FROM Request r WHERE r.user.id = :userId AND r.isArchived = false")
    List<Request> findByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Request r WHERE r.status = :status AND r.isArchived = false")
    List<Request> findByStatus(String status);

    @Query("SELECT r FROM Request r WHERE r.category = :category AND r.isArchived = false")
    List<Request> findByCategory(@Param("category") String category);

    @Query("SELECT r FROM Request r WHERE r.status = :status AND r.category = :category AND r.isArchived = false")
    List<Request> findByStatusAndCategory(@Param("status") String status, @Param("category") String category);

    @Query("SELECT r FROM Request r WHERE r.user.id = :userId AND r.status = :status AND r.isArchived = false")
    List<Request> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT r FROM Request r WHERE r.user.id = :userId AND r.isArchived = false ORDER BY r.creationDate DESC")
    List<Request> findAllUserRequests(@Param("userId") Long userId);

    @Query("SELECT DISTINCT r FROM Request r " +
            "JOIN HelpHistory h ON r.id = h.request.id " +
            "WHERE h.helper.id = :userId AND h.status = 'IN_PROGRESS' AND r.isArchived = false " +
            "ORDER BY h.startDate DESC")
    List<Request> findActiveHelpRequests(@Param("userId") Long userId);

    @Query("SELECT DISTINCT r FROM Request r " +
            "JOIN HelpHistory h ON r.id = h.request.id " +
            "WHERE h.helper.id = :userId AND h.status = :status AND r.isArchived = false")
    List<Request> findByHelperIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT DISTINCT r FROM Request r " +
            "JOIN HelpHistory h ON r.id = h.request.id " +
            "WHERE h.helper.id = :userId AND r.isArchived = false")
    List<Request> findAllHelpedRequests(@Param("userId") Long userId);

    @Query("SELECT r FROM Request r WHERE r.user.id = :userId AND r.isArchived = true")
    List<Request> findArchivedRequests(@Param("userId") Long userId);

    @Query("SELECT r FROM Request r WHERE r.user.id = :userId AND r.status = 'COMPLETED' AND r.isArchived = false")
    List<Request> findCompletedRequests(@Param("userId") Long userId);

    @Query("SELECT DISTINCT r FROM Request r " +
            "JOIN HelpHistory h ON r.id = h.request.id " +
            "WHERE h.helper.id = :userId AND h.status = 'COMPLETED' AND r.isArchived = false")
    List<Request> findCompletedHelpRequests(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM requests r " +
            "WHERE ST_Distance(" +
            "    ST_SetSRID(ST_MakePoint(r.longitude, r.latitude), 4326)::geography, " +
            "    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography" +
            ") <= :distance AND r.is_archived = false",
            nativeQuery = true)
    List<Request> findNearbyRequests(@Param("lat") double lat,
                                     @Param("lon") double lon,
                                     @Param("distance") double distance);

}