package com.example.platform.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.platform.model.Review;
import com.example.platform.model.User;
import com.example.platform.repository.ReviewRepository;
import com.example.platform.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Инициализируем списки, если они null
        if (user.getCreatedRequests() == null) {
            user.setCreatedRequests(new ArrayList<>());
        }
        if (user.getHelpedRequests() == null) {
            user.setHelpedRequests(new ArrayList<>());
        }

        // Вычисляем и устанавливаем рейтинг пользователя
        calculateAndSetUserRating(user);

        return user;
    }

    private void calculateAndSetUserRating(User user) {
        List<Review> reviews = reviewRepository.findByHelperId(user.getId());
        double avgRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0);
        user.setRating((int)Math.round(avgRating));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Map<String, Object> getStatistics(Long userId) {
        User user = getUserById(userId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("createdRequests", user.getCreatedRequests().size());
        statistics.put("helpedRequests", user.getHelpedRequests().size());
        statistics.put("rating", user.getRating());

        return statistics;
    }

    public User updateRating(Long userId, Integer rating) {
        User user = getUserById(userId);
        user.setRating(rating);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(User user) {
        // Проверяем существование пользователя
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Обновляем только разрешенные поля
        existingUser.setName(user.getName());
        existingUser.setAvatarUrl(user.getAvatarUrl());
        existingUser.setBirthDate(user.getBirthDate());

        // Сохраняем обновленного пользователя
        return userRepository.save(existingUser);
    }

    public List<Map<String, Object>> getUsersRating() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("id", user.getId());
                    userData.put("name", user.getName());
                    List<Review> reviews = reviewRepository.findByHelperId(user.getId());
                    int helpedCount = reviews.size();
                    double avgRating = helpedCount > 0
                            ? reviews.stream().mapToInt(Review::getRating).average().orElse(0)
                            : 0;
                    userData.put("rating", avgRating);
                    userData.put("helpedCount", helpedCount);
                    userData.put("avatarUrl", user.getAvatarUrl());
                    return userData;
                })
                .filter(u -> (int)u.get("helpedCount") > 0)
                .sorted((a, b) -> {
                    int cmp = Double.compare((double) b.get("rating"), (double) a.get("rating"));
                    if (cmp != 0) return cmp;
                    return Integer.compare((int) b.get("helpedCount"), (int) a.get("helpedCount"));
                })
                .collect(Collectors.toList());
    }
}