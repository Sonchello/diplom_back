package com.example.platform.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.platform.model.HelpHistory;
import com.example.platform.model.Request;
import com.example.platform.model.User;
import com.example.platform.repository.HelpHistoryRepository;
import com.example.platform.repository.RequestRepository;
import com.example.platform.repository.UserRepository;

@Service
public class RequestService {
    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HelpHistoryRepository helpHistoryRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Request createRequest(Long userId, Request request) {
        try {
            System.out.println("Creating request for userId: " + userId);

            if (userId == null || userId == 0) {
                throw new RuntimeException("Invalid userId: " + userId);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        System.out.println("User not found for id: " + userId);
                        return new RuntimeException("User not found for id: " + userId);
                    });

            System.out.println("Found user: " + user.getId() + ", " + user.getEmail());

            request.setUser(user);
            request.setCreationDate(LocalDateTime.now());
            request.setStatus("ACTIVE");
            request.setArchived(false);

            if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
                throw new RuntimeException("Description is required");
            }
            if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
                throw new RuntimeException("Category is required");
            }

            Request savedRequest = requestRepository.save(request);
            System.out.println("Request saved successfully with id: " + savedRequest.getId());
            return savedRequest;
        } catch (Exception e) {
            System.err.println("Error in createRequest: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error creating request: " + e.getMessage());
        }
    }

    public List<Request> getActiveRequests() {
        List<Request> activeRequests = requestRepository.findByStatus("ACTIVE");
        List<Request> inProgressRequests = requestRepository.findByStatus("IN_PROGRESS");
        activeRequests.addAll(inProgressRequests);
        return activeRequests;
    }

    @Transactional
    public void deleteRequest(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own requests");
        }

        // First delete associated help history records
        helpHistoryRepository.deleteAll(helpHistoryRepository.findByRequestId(requestId));

        // Then delete the request
        requestRepository.delete(request);
    }

    @Transactional
    public Request updateRequestStatus(Long requestId, String status, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only update your own requests");
        }

        request.setStatus(status);
        if ("COMPLETED".equals(status)) {
            request.setCompletionDate(LocalDateTime.now());
        }
        return requestRepository.save(request);
    }

    @Transactional
    public void respondToRequest(Long userId, Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        User helper = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Проверяем, не является ли пользователь создателем запроса
        if (request.getUser().getId().equals(userId)) {
            throw new RuntimeException("Вы не можете откликнуться на свой собственный запрос");
        }

        // Проверяем, не откликнулся ли уже пользователь на этот запрос
        boolean alreadyResponded = helpHistoryRepository.existsByRequestIdAndHelperId(requestId, userId);
        if (alreadyResponded) {
            throw new RuntimeException("Вы уже откликнулись на этот запрос");
        }

        // Создаем новую запись в истории помощи
        HelpHistory helpHistory = new HelpHistory();
        helpHistory.setRequest(request);
        helpHistory.setHelper(helper);
        helpHistory.setStatus("IN_PROGRESS");
        helpHistory.setStartDate(LocalDateTime.now());
        helpHistoryRepository.save(helpHistory);

        // Добавляем помощника в список помощников запроса
        if (!request.getHelpers().contains(helper)) {
            request.getHelpers().add(helper);
        }

        // Сохраняем обновленный запрос
        requestRepository.save(request);
    }

    public List<Request> getUserRequests(Long userId) {
        return requestRepository.findAllUserRequests(userId);
    }

    public List<Request> getHelpedRequestsByUser(Long userId) {
        return requestRepository.findAllHelpedRequests(userId);
    }

    public List<Request> getArchivedRequests(Long userId) {
        return requestRepository.findArchivedRequests(userId);
    }

    public List<Request> getCompletedRequests(Long userId) {
        return requestRepository.findCompletedRequests(userId);
    }

    @Transactional
    public Request archiveRequest(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only archive your own requests");
        }

        request.setArchived(true);
        return requestRepository.save(request);
    }

    @Transactional
    public Request completeHelp(Long requestId, Long helperId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getActiveHelper().getId().equals(helperId)) {
            throw new RuntimeException("Only the active helper can complete this request");
        }

        // Обновляем запись в help_history
        HelpHistory helpHistory = helpHistoryRepository.findActiveHelpsByUserId(helperId, "IN_PROGRESS")
                .stream()
                .filter(h -> h.getRequest().getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Help history not found"));

        helpHistory.setStatus("PENDING_CONFIRMATION");
        helpHistory.setEndDate(LocalDateTime.now());
        helpHistoryRepository.save(helpHistory);

        // Создаем уведомление для создателя запроса
        notificationService.createHelpCompletionNotification(requestId, helperId);

        return request;
    }

    @Transactional
    public Request cancelHelp(Long requestId, Long helperId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getActiveHelper().getId().equals(helperId)) {
            throw new RuntimeException("Only the active helper can cancel help");
        }

        // Обновляем запись в help_history
        HelpHistory helpHistory = helpHistoryRepository.findActiveHelpsByUserId(helperId, "IN_PROGRESS")
                .stream()
                .filter(h -> h.getRequest().getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Help history not found"));

        helpHistory.setStatus("CANCELLED");
        helpHistory.setEndDate(LocalDateTime.now());
        helpHistoryRepository.save(helpHistory);

        // Обновляем запрос
        request.setStatus("ACTIVE");
        request.setActiveHelper(null);
        request.setHelpStartDate(null);
        return requestRepository.save(request);
    }

    public List<Request> getActiveHelpRequests(Long userId) {
        return requestRepository.findByActiveHelperIdAndStatus(userId, "IN_PROGRESS");
    }

    public List<Request> getAllHelpedRequests(Long userId) {
        return requestRepository.findAllHelpedRequests(userId);
    }

    public List<Request> getCompletedHelpRequests(Long userId) {
        return requestRepository.findCompletedHelpRequests(userId);
    }

    public List<Request> filterRequests(String category, String urgency, String status, Double maxDistance,
                                        Double userLat, Double userLon) {
        List<Request> requests = requestRepository.findAll();

        return requests.stream()
                .filter(request -> {
                    boolean matches = true;

                    if (category != null && !category.equals("all")) {
                        matches = matches && request.getCategory().equals(category);
                    }

                    if (urgency != null && !urgency.equals("all")) {
                        matches = matches && request.getUrgency().equals(urgency);
                    }

                    if (status != null) {
                        matches = matches && request.getStatus().equals(status);
                    }

                    if (maxDistance != null && userLat != null && userLon != null) {
                        double distance = calculateDistance(userLat, userLon,
                                request.getLatitude(), request.getLongitude());
                        matches = matches && distance <= maxDistance;
                    }

                    return matches;
                })
                .collect(Collectors.toList());
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId).orElse(null);
    }

    @Transactional
    public Request confirmHelpCompletion(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only the request creator can confirm help completion");
        }

        // Находим активную запись в help_history
        HelpHistory helpHistory = helpHistoryRepository.findByRequestIdAndStatus(requestId, "PENDING_CONFIRMATION")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No pending help confirmation found"));

        // Обновляем статус в help_history на COMPLETED
        helpHistory.setStatus("COMPLETED");
        helpHistoryRepository.save(helpHistory);

        // Добавляем помощника в список helpers
        User helper = helpHistory.getHelper();
        if (!request.getHelpers().contains(helper)) {
            request.getHelpers().add(helper);
        }

        // Сбрасываем активного помощника и возвращаем запрос в статус ACTIVE
        request.setActiveHelper(null);
        request.setStatus("ACTIVE");
        return requestRepository.save(request);
    }
}