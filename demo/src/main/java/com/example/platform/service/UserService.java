package com.example.platform.service;

import com.example.platform.model.User;
import com.example.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

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

        return user;
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
}