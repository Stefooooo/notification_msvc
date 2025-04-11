package app.service;

import app.model.NotificationStatus;
import app.model.NotificationType;
import app.model.Notification;
import app.model.NotificationPreference;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import app.web.dto.*;
import app.web.mapper.DtoMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository notificationRepository;
    private final MailSender mailSender;

    @Autowired
    public NotificationService(NotificationPreferenceRepository preferenceRepository, NotificationRepository notificationRepository, MailSender mailSender) {
        this.preferenceRepository = preferenceRepository;
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    public NotificationPreference upsertPreference(UpsertNotificationPreference dto) {

        Optional<NotificationPreference> userNotificationPreferenceOptional = preferenceRepository.findByUserId(dto.getUserId());

        if (userNotificationPreferenceOptional.isPresent()) {
            NotificationPreference preference = userNotificationPreferenceOptional.get();
            preference.setContactInfo(dto.getContactInfo());
            preference.setEnabled(dto.isNotificationEnabled());
            preference.setType(DtoMapper.fromNotificationTypeRequest(dto.getType()));
            preference.setUpdatedOn(LocalDateTime.now());
            preferenceRepository.save(preference);
            return preference;
        }

        NotificationPreference notificationPreference = NotificationPreference.builder()
                .userId(dto.getUserId())
                .type(DtoMapper.fromNotificationTypeRequest(dto.getType()))
                .enabled(dto.isNotificationEnabled())
                .contactInfo(dto.getContactInfo())
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        preferenceRepository.save(notificationPreference);
        return notificationPreference;
    }

    public NotificationPreference getPreferenceByUserId(UUID userId) {

        return preferenceRepository.findByUserId(userId).orElseThrow(() -> new NullPointerException("Notification preference for user id %s was not found.".formatted(userId)));
    }

    public Notification sendNotification(NotificationRequest notificationRequest) {

        UUID userId = notificationRequest.getUserId();
        NotificationPreference userPreference = getPreferenceByUserId(userId);

        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException("User with id %s does not allow to receive notifications.".formatted(userId));
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userPreference.getContactInfo());
        message.setSubject(notificationRequest.getSubject());
        message.setText(notificationRequest.getBody());

        // Entity building
        Notification notification = Notification.builder()
                .subject(notificationRequest.getSubject())
                .body(notificationRequest.getBody())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .deleted(false)
                .type(NotificationType.EMAIL)
                .build();

        try {
            mailSender.send(message);
            notification.setStatus(NotificationStatus.SUCCEEDED);
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("There was an issue sending an email to %s due to %s.".formatted(userPreference.getContactInfo(), e.getMessage()));
        }

        notificationRepository.save(notification);
        return notification;
    }

    public List<Notification> getNotificationHistory(UUID userId) {

        return notificationRepository.findAllByUserIdAndDeletedIsFalse(userId);
    }

    public NotificationPreference changeNotificationPreference(UUID userId, boolean enabled) {

        // If exist - return NotificationPreference
        // If does not exist - throws exception
        NotificationPreference notificationPreference = getPreferenceByUserId(userId);
        notificationPreference.setEnabled(enabled);
        notificationPreference.setUpdatedOn(LocalDateTime.now());
        preferenceRepository.save(notificationPreference);
        return notificationPreference;
    }

    public List<Notification> clearNotifications(UUID userId) {

        List<Notification> notifications = getNotificationHistory(userId);

        notifications.forEach(notification -> {
            notification.setDeleted(true);
            notificationRepository.save(notification);
        });

        return notifications;
    }

    public void retryFailedNotifications(UUID userId) {

        NotificationPreference userPreference = getPreferenceByUserId(userId);
        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException("User with id %s does not allow to receive notifications.".formatted(userId));
        }

        List<Notification> failedNotifications = notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.FAILED);
        failedNotifications = failedNotifications.stream().filter(notification ->  !notification.isDeleted()).toList();

        for (Notification notification : failedNotifications) {

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userPreference.getContactInfo());
            message.setSubject(notification.getSubject());
            message.setText(notification.getBody());

            try {
                mailSender.send(message);
                notification.setStatus(NotificationStatus.SUCCEEDED);
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.FAILED);
                log.warn("There was an issue sending an email to %s due to %s.".formatted(userPreference.getContactInfo(), e.getMessage()));
            }

            notificationRepository.save(notification);
        }
    }

    public NotificationPreference getOrAddPreference(UserWithNoPreference userWithNoPreference) {

        Optional<NotificationPreference> byUserId = preferenceRepository.findByUserId(userWithNoPreference.getId());
        if (byUserId.isPresent()){
            return byUserId.get();
        }

        UpsertNotificationPreference upsertNotificationPreference = UpsertNotificationPreference.builder()
                .notificationEnabled(false)
                .userId(userWithNoPreference.getId())
                .type(NotificationTypeRequest.EMAIL)
                .contactInfo(userWithNoPreference.getEmail())
                .build();

       return upsertPreference(upsertNotificationPreference);
    }
}