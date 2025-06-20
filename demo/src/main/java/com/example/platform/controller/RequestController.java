package com.example.platform.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.platform.model.Request;
import com.example.platform.service.RequestService;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/requests")
public class RequestController {
    @Autowired
    private RequestService requestService;

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("Received payload: " + payload);

            Long userId = null; // Инициализация с null
            if (payload.containsKey("userId")) {
                userId = Long.parseLong(payload.get("userId").toString());
                System.out.println("Parsed userId: " + userId);
            } else {
                return ResponseEntity.badRequest().body("UserId is required");
            }

            String description = (String) payload.get("description");
            String category = (String) payload.get("category");
            Double latitude = null; // Инициализация с null
            Double longitude = null; // Инициализация с null

            if (payload.containsKey("latitude")) {
                latitude = Double.parseDouble(payload.get("latitude").toString());
            }

            if (payload.containsKey("longitude")) {
                longitude = Double.parseDouble(payload.get("longitude").toString());
            }

            // Получаем deadline_date из payload
            String deadlineDateStr = (String) payload.get("deadline_date");
            if (deadlineDateStr == null) {
                return ResponseEntity.badRequest().body("Deadline date is required");
            }

            LocalDateTime deadlineDate;
            try {
                deadlineDate = LocalDateTime.parse(deadlineDateStr);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid deadline date format");
            }

            Request request = new Request();
            request.setDescription(description);
            request.setCategory(category);
            request.setLatitude(latitude);
            request.setLongitude(longitude);
            request.setDeadlineDate(deadlineDate);

            Request createdRequest = requestService.createRequest(userId, request);
            return ResponseEntity.ok(createdRequest);
        } catch (Exception e) {
            System.err.println("Error in controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating request: " + e.getMessage());
        }
    }

    @GetMapping("/active")
    public List<Request> getActiveRequests() {
        return requestService.getActiveRequests();
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<?> deleteRequest(
            @PathVariable Long requestId,
            @RequestParam Long userId) {
        try {
            // Проверяем, существует ли запрос (используем findById, который теперь фильтрует неархивированные)
            Request request = requestService.getRequestById(requestId);
            if (request == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Request not found with id: " + requestId
                        ));
            }

            // Проверяем, является ли пользователь создателем запроса
            if (request.getUser() == null || !request.getUser().getId().equals(userId)) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "You can only delete your own requests"
                        ));
            }

            // Вызываем метод архивации вместо удаления
            requestService.archiveRequest(requestId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Request archived successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error archiving request: " + e.getMessage()
                    ));
        }
    }

    @PutMapping("/{requestId}/status")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable Long requestId,
            @RequestParam String status,
            @RequestParam Long userId) {
        try {
            Request updatedRequest = requestService.updateRequestStatus(requestId, status, userId);
            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{requestId}/help")
    public ResponseEntity<?> respondToRequest(
            @PathVariable Long requestId,
            @RequestParam Long userId) {
        try {
            requestService.respondToRequest(userId, requestId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRequests(@PathVariable Long userId) {
        try {
            List<Request> requests = requestService.getUserRequests(userId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/helped")
    public ResponseEntity<?> getHelpedRequestsByUser(@PathVariable Long userId) {
        try {
            List<Request> requests = requestService.getHelpedRequestsByUser(userId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<?> filterRequests(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLon,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tab) {
        try {
            // Валидация параметров
            if (maxDistance != null && maxDistance < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Максимальное расстояние не может быть отрицательным"
                ));
            }

            if (userLat != null && (userLat < -90 || userLat > 90)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Некорректная широта"
                ));
            }

            if (userLon != null && (userLon < -180 || userLon > 180)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Некорректная долгота"
                ));
            }


            if (userLat != null && userLon != null && maxDistance == null) {
                maxDistance = 1000.0;
            }

            if (maxDistance != null && (userLat == null || userLon == null)) {
                maxDistance = null;
            }

            List<String> targetStatuses = null; // Изначально статусы не фильтруем

            if ("active".equals(tab)) {
                targetStatuses = new ArrayList<>();
                targetStatuses.add("ACTIVE");
                targetStatuses.add("IN_PROGRESS");
            } else if (status != null) {
                targetStatuses = new ArrayList<>();
                targetStatuses.add(status);
            }

            List<Request> requests = requestService.filterRequests(
                    category, targetStatuses, maxDistance, userLat, userLon);

            // Дополнительная фильтрация на основе вкладки
            if (tab != null && userId != null) {
                switch (tab) {
                    case "my":
                        // Для вкладки "Мои запросы" фильтруем только по пользователю, статус любой
                        requests = requestService.getUserRequests(userId);
                        break;
                    case "responses":
                        requests = requests.stream()
                                .filter(request -> request.getHelpHistory() != null &&
                                        request.getHelpHistory().stream()
                                                .anyMatch(h -> h.getStatus().equals("IN_PROGRESS") &&
                                                        h.getHelper() != null &&
                                                        h.getHelper().getId().equals(userId)))
                                .collect(Collectors.toList());
                        break;
                }
            } else if (userId != null) {
                // Если tab не указан, но указан userId, предполагаем, что нужны все запросы пользователя
                requests = requestService.getUserRequests(userId);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", requests,
                    "total", requests.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Ошибка при фильтрации запросов: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/user/{userId}/active-helps")
    public ResponseEntity<?> getActiveHelpRequests(@PathVariable Long userId) {
        try {
            List<Request> requests = requestService.getActiveHelpRequests(userId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/completed-helps")
    public ResponseEntity<?> getCompletedHelpRequests(@PathVariable Long userId) {
        try {
            List<Request> requests = requestService.getCompletedHelpRequests(userId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{requestId}/complete-help")
    public ResponseEntity<?> completeHelp(
            @PathVariable Long requestId,
            @RequestParam Long helperId) {
        try {
            Request updatedRequest = requestService.completeHelp(requestId, helperId);
            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{requestId}/cancel-help")
    public ResponseEntity<?> cancelHelp(
            @PathVariable Long requestId,
            @RequestParam Long helperId) {
        try {
            Request updatedRequest = requestService.cancelHelp(requestId, helperId);
            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{requestId}/confirm-help")
    public ResponseEntity<?> confirmHelpCompletion(
            @PathVariable Long requestId,
            @RequestParam Long userId) {
        try {
            Request request = requestService.confirmHelpCompletion(requestId, userId);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{requestId}/reject-help")
    public ResponseEntity<?> rejectHelpCompletion(
            @PathVariable Long requestId,
            @RequestParam Long userId) {
        try {
            requestService.rejectHelpCompletion(requestId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}