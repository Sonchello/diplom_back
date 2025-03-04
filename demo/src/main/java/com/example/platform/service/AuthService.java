package com.example.platform.service;

import com.example.platform.model.User;
import com.example.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    // Регистрация пользователя
    public Map<String, String> registerUser(User user) {
        Map<String, String> response = new HashMap<>();
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            response.put("message", "Пользователь с таким email уже существует");
            return response;
        }
        userRepository.save(user);
        response.put("message", "Пользователь успешно зарегистрирован");
        return response;
    }

    // Вход пользователя
    @Autowired
    private JwtService jwtService;

    public Map<String, String> loginUser(String email, String password) {
        Map<String, String> response = new HashMap<>();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (user.getPassword().equals(password)) {
            String token = jwtService.generateToken(email); // Генерация JWT
            response.put("message", "Вход выполнен успешно");
            response.put("token", token); // Возвращаем реальный токен
        } else {
            response.put("message", "Неверный пароль");
        }
        return response;
    }
}
