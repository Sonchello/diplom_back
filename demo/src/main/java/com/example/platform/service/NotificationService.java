package com.example.platform.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.platform.model.Notification;
import com.example.platform.model.Request;
import com.example.platform.model.User;
import com.example.platform.repository.NotificationRepository;
import com.example.platform.repository.RequestRepository;
import com.example.platform.repository.UserRepository;

@Service
@Transactional
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestRepository requestRepository;

    public void createHelpCompletionNotification(Long requestId, Long helperId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        User helper = userRepository.findById(helperId)
                .orElseThrow(() -> new RuntimeException("Helper not found"));

        String message = String.format(
                " %s сообщает, что оказал помощь по вашему запросу '%s'. Пожалуйста, подтвердите получение помощи.",
                helper.getName(),
                request.getDescription()
        );

        Notification notification = new Notification();
        notification.setUser(request.getUser());
        notification.setRequest(request);
        notification.setMessage(message);
        notification.setType("HELP_COMPLETION");
        notification.setStatus("UNREAD");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setActionUrl("/requests/" + requestId + "/confirm-help");
        notification.setActionNeeded(true);
        notification.setFromUser(helper);

        notificationRepository.save(notification);
    }

    // Общий метод для создания уведомлений
    public void createNotification(Long userId, Long requestId, String message, String type, boolean actionNeeded, String actionUrl, Long fromUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Request request = null;
        if (requestId != null) {
            request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));
        }

        User fromUser = null;
        if (fromUserId != null) {
            fromUser = userRepository.findById(fromUserId)
                    .orElseThrow(() -> new RuntimeException("From User not found"));
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setRequest(request);
        notification.setMessage(message);
        notification.setType(type);
        notification.setStatus("UNREAD"); // Новые уведомления всегда UNREAD
        notification.setCreatedAt(LocalDateTime.now());
        notification.setActionNeeded(actionNeeded);
        notification.setActionUrl(actionUrl);
        notification.setFromUser(fromUser);

        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public void deleteNotificationForRequestAndType(Long requestId, String type) {
        System.out.println("Deleting notification for request: " + requestId + " and type: " + type);
        notificationRepository.deleteByRequestIdAndType(requestId, type);
    }
}