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
                "Помощник %s сообщает, что оказал помощь по вашему запросу '%s'. Пожалуйста, подтвердите получение помощи.",
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
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}