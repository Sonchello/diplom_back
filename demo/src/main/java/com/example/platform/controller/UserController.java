package com.example.platform.controller;

import com.example.platform.model.User;
import com.example.platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(@RequestParam String email) {
        try {
            System.out.println("Getting current user for email: " + email);
            User user = userService.findByEmail(email);
            System.out.println("Found user: " + user.getId() + ", " + user.getEmail());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/statistics")
    public Map<String, Object> getStatistics(@PathVariable Long userId) {
        return userService.getStatistics(userId);
    }

    @PutMapping("/{userId}/rating")
    public User updateRating(@PathVariable Long userId, @RequestParam Integer rating) {
        return userService.updateRating(userId, rating);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) MultipartFile avatar) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            if (name != null && !name.isEmpty()) {
                user.setName(name);
            }

            if (birthDate != null && !birthDate.isEmpty()) {
                user.setBirthDate(LocalDate.parse(birthDate));
            }

            if (avatar != null && !avatar.isEmpty()) {
                String fileName = UUID.randomUUID().toString() +
                        getFileExtension(avatar.getOriginalFilename());
                Path filePath = Paths.get(uploadDir, fileName);
                Files.copy(avatar.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                user.setAvatarUrl("/uploads/" + fileName);
            }

            User updatedUser = userService.updateUser(user);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при обновлении пользователя: " + e.getMessage());
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) return "";
        return fileName.substring(lastIndexOf);
    }
}