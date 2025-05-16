package com.example.platform.service;

import com.example.platform.model.Review;
import com.example.platform.model.User;
import com.example.platform.model.Request;
import com.example.platform.repository.ReviewRepository;
import com.example.platform.repository.UserRepository;
import com.example.platform.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Review addReview(Long helperId, Long authorId, Long requestId, int rating, String text) {
        System.out.println("Adding review - helperId: " + helperId + ", authorId: " + authorId + ", requestId: " + requestId);

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
        }

        User helper = userRepository.findById(helperId)
                .orElseThrow(() -> new RuntimeException("Помощник с ID " + helperId + " не найден"));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Автор с ID " + authorId + " не найден"));

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Запрос с ID " + requestId + " не найден"));

        // Проверяем, не оставлял ли уже автор отзыв для этого запроса
        List<Review> existingReviews = reviewRepository.findByRequestId(requestId);
        boolean hasExistingReview = existingReviews.stream()
                .anyMatch(review -> review.getAuthor().getId().equals(authorId));

        if (hasExistingReview) {
            throw new RuntimeException("Вы уже оставили отзыв для этого запроса");
        }

        Review review = new Review();
        review.setHelper(helper);
        review.setAuthor(author);
        review.setRequest(request);
        review.setRating(rating);
        review.setText(text);

        System.out.println("Saving review for request: " + requestId);
        Review savedReview = reviewRepository.save(review);

        // Обновляем рейтинг помощника
        List<Review> helperReviews = reviewRepository.findByHelperId(helperId);
        double avgRating = helperReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0);
        helper.setRating((int)Math.round(avgRating));
        userRepository.save(helper);

        // Удаляем уведомление о необходимости оставить отзыв
        notificationService.deleteNotificationForRequestAndType(requestId, "REVIEW_REQUESTED");

        return savedReview;
    }

    public List<Review> getReviewsForHelper(Long helperId) {
        if (!userRepository.existsById(helperId)) {
            throw new RuntimeException("Помощник с ID " + helperId + " не найден");
        }
        return reviewRepository.findByHelperId(helperId);
    }

    public List<Review> getReviewsForRequest(Long requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new RuntimeException("Запрос с ID " + requestId + " не найден");
        }
        return reviewRepository.findByRequestId(requestId);
    }

    public List<Review> getReviewsByAuthor(Long authorId) {
        if (!userRepository.existsById(authorId)) {
            throw new RuntimeException("Автор с ID " + authorId + " не найден");
        }
        return reviewRepository.findByAuthorId(authorId);
    }
}