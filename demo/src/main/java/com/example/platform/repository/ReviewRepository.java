package com.example.platform.repository;

import com.example.platform.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByHelperId(Long helperId);
    List<Review> findByRequestId(Long requestId);
    List<Review> findByAuthorId(Long authorId);
}