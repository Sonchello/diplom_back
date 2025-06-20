package com.example.platform.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.platform.model.HelpHistory;
import com.example.platform.model.Request;
import com.example.platform.model.User;
import com.example.platform.repository.HelpHistoryRepository;
import com.example.platform.repository.NotificationRepository;
import com.example.platform.repository.RequestRepository;
import com.example.platform.repository.ReviewRepository;
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

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

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

            // Проверяем, что deadlineDate не в прошлом
            if (request.getDeadlineDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Deadline date cannot be in the past");
            }

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

        // Добавляем рейтинг пользователя (создателя) в каждый Request
        for (Request req : activeRequests) {
            if (req.getUser() != null) {
                // Вычисляем средний рейтинг из отзывов
                double avgRating = 0;
                var reviews = reviewRepository.findByHelperId(req.getUser().getId());
                if (reviews != null && !reviews.isEmpty()) {
                    avgRating = reviews.stream()
                            .mapToInt(r -> r.getRating())
                            .average()
                            .orElse(0);
                }

                // Устанавливаем рейтинг пользователя
                req.getUser().setRating((int)Math.round(avgRating));

                // Убеждаемся, что имя пользователя установлено
                if (req.getUser().getName() == null || req.getUser().getName().trim().isEmpty()) {
                    req.getUser().setName("Пользователь");
                }
            }
        }
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
            // request.setCompletionDate(LocalDateTime.now()); // Удалено, так как поле удалено из Request
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

        // Проверяем, не откликнулся ли уже этот пользователь
        List<HelpHistory> existingHelps = helpHistoryRepository.findByRequestIdAndHelperIdOrderByStartDateDesc(requestId, userId);
        if (!existingHelps.isEmpty()) {
            HelpHistory lastHelp = existingHelps.get(0);
            if ("IN_PROGRESS".equals(lastHelp.getStatus()) || "PENDING_CONFIRMATION".equals(lastHelp.getStatus())) {
                throw new RuntimeException("Вы уже откликнулись на этот запрос");
            }
        }

        // Создаем новую запись в истории помощи
        HelpHistory helpHistory = new HelpHistory();
        helpHistory.setRequest(request);
        helpHistory.setHelper(helper);
        helpHistory.setStatus("IN_PROGRESS");
        helpHistory.setStartDate(LocalDateTime.now());

        // Сохраняем запись в истории
        helpHistoryRepository.save(helpHistory);

        // Обновляем статус запроса
        request.setStatus("IN_PROGRESS");
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
    public void archiveRequest(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only archive your own requests");
        }

        // Вместо физического удаления, устанавливаем флаг isArchived в true
        request.setArchived(true);
        requestRepository.save(request);

        // Удаление записей helpHistory теперь не нужно, т.к. связь ON DELETE CASCADE удалена в БД,
        // а при архивации мы не удаляем сам запрос, поэтому история сохраняется.
        // helpHistoryRepository.deleteAll(helpHistoryRepository.findByRequestId(requestId)); // Эта строка удалена

        // Физическое удаление запроса теперь не выполняется
        // requestRepository.delete(request); // Эта строка удалена
    }

    @Transactional
    public Request completeHelp(Long requestId, Long helperId) {
        // Находим запись в help_history для этого запроса и этого помощника со статусом IN_PROGRESS
        List<HelpHistory> activeHelps = helpHistoryRepository.findActiveHelpsByRequestAndHelper(requestId, helperId);

        if (activeHelps.isEmpty()) {
            throw new RuntimeException("Активная запись истории помощи для этого запроса и помощника не найдена");
        }

        // Предполагаем, что есть только одна активная запись IN_PROGRESS для данной пары запрос-помощник
        HelpHistory helpHistory = activeHelps.get(0);

        // Обновляем запись в help_history
        helpHistory.setStatus("PENDING_CONFIRMATION"); // Или сразу "COMPLETED" в зависимости от флоу
        helpHistory.setEndDate(LocalDateTime.now());
        helpHistoryRepository.save(helpHistory);

        // Создаем уведомление для создателя запроса
        notificationService.createHelpCompletionNotification(requestId, helperId);

        // Возвращаем обновленный запрос (статус запроса в таблице requests обновляется при подтверждении/отклонении)
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        return request; // Или requestRepository.save(request) если нужно сохранить изменения в запросе сразу
    }

    @Transactional
    public Request cancelHelp(Long requestId, Long helperId) {
        // Находим запись в help_history для этого запроса и этого помощника со статусом IN_PROGRESS
        List<HelpHistory> activeHelps = helpHistoryRepository.findActiveHelpsByRequestAndHelper(requestId, helperId);

        if (activeHelps.isEmpty()) {
            throw new RuntimeException("Активная запись истории помощи для этого запроса и помощника не найдена");
        }

        // Предполагаем, что есть только одна активная запись IN_PROGRESS для данной пары запрос-помощник
        HelpHistory helpHistory = activeHelps.get(0);

        // Обновляем запись в help_history
        helpHistory.setStatus("CANCELLED");
        helpHistory.setEndDate(LocalDateTime.now());
        helpHistoryRepository.save(helpHistory);

        // Проверяем, остались ли еще активные помощники по этому запросу в help_history
        List<HelpHistory> remainingActiveHelps = helpHistoryRepository.findByRequestIdAndStatus(requestId, "IN_PROGRESS");

        // Если активных помощников не осталось, меняем статус запроса на ACTIVE
        if (remainingActiveHelps.isEmpty()) {
            Request request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));
            request.setStatus("ACTIVE");
            requestRepository.save(request);
        }

        // Удаляем сброс activeHelper, helpStartDate
        // request.setActiveHelper(null); // Удалено
        // request.setHelpStartDate(null); // Удалено

        // Возвращаем запрос (статус запроса мог измениться)
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        return request;
    }

    public List<Request> getActiveHelpRequests(Long userId) {
        return requestRepository.findByHelperIdAndStatus(userId, "IN_PROGRESS");
    }

    public List<Request> getAllHelpedRequests(Long userId) {
        return requestRepository.findAllHelpedRequests(userId);
    }

    public List<Request> getCompletedHelpRequests(Long userId) {
        return requestRepository.findCompletedHelpRequests(userId);
    }

    public List<Request> filterRequests(String category, List<String> statuses, Double maxDistance,
                                        Double userLat, Double userLon) {
        // Если указаны координаты и расстояние, используем пространственный запрос
        if (maxDistance != null && userLat != null && userLon != null && maxDistance > 0) {
            // Конвертируем расстояние из метров в градусы (примерно)
            double distanceInDegrees = maxDistance / 111000.0; // 1 градус ≈ 111 км
            List<Request> nearbyRequests = requestRepository.findNearbyRequests(userLat, userLon, distanceInDegrees);

            // Дополнительная фильтрация по категории и статусу
            return nearbyRequests.stream()
                    .filter(request -> {
                        boolean matches = true;
                        if (category != null && !category.equals("all")) {
                            matches = matches && request.getCategory().equals(category);
                        }
                        if (statuses != null && !statuses.isEmpty()) {
                            matches = matches && statuses.contains(request.getStatus());
                        }
                        return matches;
                    })
                    .collect(Collectors.toList());
        }

        // Если координаты не указаны, используем обычную фильтрацию
        List<Request> requests = requestRepository.findAll();
        return requests.stream()
                .filter(request -> {
                    boolean matches = true;
                    if (category != null && !category.equals("all")) {
                        matches = matches && request.getCategory().equals(category);
                    }
                    if (statuses != null && !statuses.isEmpty()) {
                        matches = matches && statuses.contains(request.getStatus());
                    }
                    return matches;
                })
                .collect(Collectors.toList());
    }

    public Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId).orElse(null);
    }

    @Transactional
    public Request confirmHelpCompletion(Long requestId, Long userId) {
        System.out.println("Confirming help completion for request: " + requestId + ", userId: " + userId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    System.out.println("Request not found: " + requestId);
                    return new RuntimeException("Request not found");
                });

        if (!request.getUser().getId().equals(userId)) {
            System.out.println("User " + userId + " is not the creator of request " + requestId);
            throw new RuntimeException("Only the request creator can confirm help completion");
        }

        // Находим записи в help_history со статусом PENDING_CONFIRMATION для этого запроса
        List<HelpHistory> pendingHelps = helpHistoryRepository.findByRequestIdAndStatus(requestId, "PENDING_CONFIRMATION");
        System.out.println("Found " + pendingHelps.size() + " pending help records");

        // Обрабатываем все записи в статусе PENDING_CONFIRMATION для данного запроса
        for (HelpHistory helpHistory : pendingHelps) {
            // Обновляем статус в help_history на COMPLETED
            helpHistory.setStatus("COMPLETED");
            // helpHistory.setEndDate(LocalDateTime.now()); // endDate уже установлен в completeHelp
            helpHistoryRepository.save(helpHistory);
            System.out.println("Updated help history status to COMPLETED for helpHistory id: " + helpHistory.getId());

            // Добавляем помощника в список helpers запроса, если его там еще нет (на случай, если multiple helpers)
            User helper = helpHistory.getHelper();
            if (request.getHelpers() == null) {
                request.setHelpers(new ArrayList<>());
            }
            if (!request.getHelpers().contains(helper)) {
                request.getHelpers().add(helper);
                System.out.println("Added helper " + helper.getId() + " to request helpers list");
            }
        }

        // Проверяем, есть ли еще активные (IN_PROGRESS) или ожидающие подтверждения (PENDING_CONFIRMATION) записи в help_history для этого запроса
        List<HelpHistory> remainingActiveOrPending = helpHistoryRepository.findByRequestIdAndStatus(requestId, "IN_PROGRESS");
        remainingActiveOrPending.addAll(helpHistoryRepository.findByRequestIdAndStatus(requestId, "PENDING_CONFIRMATION"));

        // Если активных или ожидающих подтверждения записей не осталось, меняем статус запроса на COMPLETED
        if (remainingActiveOrPending.isEmpty()) {
            request.setStatus("COMPLETED");
            // request.setCompletionDate(LocalDateTime.now()); // completionDate удалено
            System.out.println("Updated request status to COMPLETED");
        }

        // Удаляем сброс activeHelper и возврат статуса запроса в ACTIVE (теперь статус COMPLETED)
        // request.setActiveHelper(null); // Удалено
        // request.setStatus("ACTIVE"); // Удалено

        Request savedRequest = requestRepository.save(request); // Сохраняем изменения в запросе (статус и helpers)

        // Удаляем уведомление о завершении помощи после подтверждения
        notificationService.deleteNotificationForRequestAndType(requestId, "HELP_COMPLETION");
        System.out.println("Deleted help completion notification for request: " + requestId);

        return savedRequest;
    }

    // @Scheduled(fixedRate = 300000) // Проверка каждые 5 минут
    // @Transactional
    // public void checkAndCleanupExpiredRequests() {
    //     LocalDateTime now = LocalDateTime.now();
    //     List<Request> expiredRequests = requestRepository.findExpiredActiveRequests(now);

    //     for (Request request : expiredRequests) {
    //         request.setIsExpired(true);
    //         request.setStatus("CANCELLED");
    //         request.setArchived(true);

    //         // Создаем уведомление для владельца запроса
    //         Notification notification = new Notification();
    //         notification.setUser(request.getUser());
    //         notification.setRequest(request);
    //         notification.setMessage("Ваш запрос '" + request.getDescription() + "' был автоматически отменен из-за истечения срока");
    //         notification.setType("REQUEST_EXPIRED");
    //         notification.setStatus("UNREAD");
    //         notification.setCreatedAt(LocalDateTime.now());
    //         notification.setActionNeeded(false);


    //         notificationRepository.save(notification);
    //     }

    //     requestRepository.saveAll(expiredRequests);
    // }

    @Transactional
    public void rejectHelpCompletion(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only the request creator can reject help completion");
        }

        // Находим записи в help_history со статусом PENDING_CONFIRMATION для этого запроса
        List<HelpHistory> pendingHelps = helpHistoryRepository.findByRequestIdAndStatus(requestId, "PENDING_CONFIRMATION");

        // Обновляем статус каждой такой записи в help_history на CANCELLED
        for (HelpHistory helpHistory : pendingHelps) {
            helpHistory.setStatus("CANCELLED");
            helpHistory.setEndDate(LocalDateTime.now()); // Устанавливаем дату завершения отмены
            helpHistoryRepository.save(helpHistory);
        }

        // Проверяем, есть ли еще активные (IN_PROGRESS) записи в help_history для этого запроса
        List<HelpHistory> remainingActive = helpHistoryRepository.findByRequestIdAndStatus(requestId, "IN_PROGRESS");

        // Если активных помощников не осталось, возвращаем статус запроса на ACTIVE
        if (remainingActive.isEmpty()) {
            request.setStatus("ACTIVE");
        }

        // Удаляем сброс activeHelper и возврат статуса запроса в ACTIVE (теперь статус зависит от remainingActive)
        // request.setActiveHelper(null); // Удалено
        // request.setStatus("ACTIVE"); // Удалено

        requestRepository.save(request); // Сохраняем изменения в запросе (статус)

        // Удаляем уведомление о завершении помощи после отклонения
        notificationService.deleteNotificationForRequestAndType(requestId, "HELP_COMPLETION");
    }
}

