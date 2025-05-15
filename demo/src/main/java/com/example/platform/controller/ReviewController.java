package com.example.platform.controller;

import com.example.platform.model.Review;
import com.example.platform.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<?> addReview(@RequestBody ReviewRequest request) {
        try {
            Review review = reviewService.addReview(
                    request.helperId,
                    request.authorId,
                    request.requestId,
                    request.rating,
                    request.text
            );
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при создании отзыва: " + e.getMessage());
        }
    }

    @GetMapping("/helper/{helperId}")
    public ResponseEntity<?> getReviewsForHelper(@PathVariable Long helperId) {
        try {
            List<Review> reviews = reviewService.getReviewsForHelper(helperId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при получении отзывов: " + e.getMessage());
        }
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<?> getReviewsForRequest(@PathVariable Long requestId) {
        try {
            List<Review> reviews = reviewService.getReviewsForRequest(requestId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при получении отзывов: " + e.getMessage());
        }
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<?> getReviewsByAuthor(@PathVariable Long authorId) {
        try {
            List<Review> reviews = reviewService.getReviewsByAuthor(authorId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при получении отзывов: " + e.getMessage());
        }
    }

    public static class ReviewRequest {
        public Long helperId;
        public Long authorId;
        public Long requestId;
        public int rating;
        public String text;
    }
}