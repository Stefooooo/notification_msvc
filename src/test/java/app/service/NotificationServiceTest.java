package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.web.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;



    @Test
    void testUpsertPreference_WhenExists() {
        UUID userId = UUID.randomUUID();
        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("test@example.com")
                .type(NotificationTypeRequest.EMAIL)
                .build();
        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .build();

        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.of(preference));


        NotificationPreference result = notificationService.upsertPreference(dto);

        assertEquals(preference.getUserId(), result.getUserId());
        assertTrue(result.isEnabled());
        verify(preferenceRepository, times(1)).save(result);
    }

    @Test
    void testUpsertPreference_WhenNotExists() {
        UUID userId = UUID.randomUUID();
        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("test@example.com")
                .type(NotificationTypeRequest.EMAIL)
                .build();
        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .build();
        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.empty());


        NotificationPreference result = notificationService.upsertPreference(dto);

        assertNotNull(result);
        verify(preferenceRepository, times(1)).save(result);
    }

    @Test
    void testGetPreferenceByUserId() {
        NotificationPreference preference = NotificationPreference.builder()
                .userId(UUID.randomUUID())
                .contactInfo("test@example.com")
                .build();
        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.of(preference));

        NotificationPreference result = notificationService.getPreferenceByUserId(UUID.randomUUID());

        assertEquals(preference.getUserId(), result.getUserId());
        assertEquals(preference.getContactInfo(), result.getContactInfo());
    }

    @Test
    void testGetPreferenceByUserId_ThrowsException() {
        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.empty());

        assertThrows(NullPointerException.class, () -> notificationService.getPreferenceByUserId(UUID.randomUUID()));
    }

    @Test
    void testSendNotification_Success() {
        NotificationRequest request = new NotificationRequest(UUID.randomUUID(), "Test Subject", "Test Body");
        NotificationPreference preference = NotificationPreference.builder()
                .userId(UUID.randomUUID())
                .contactInfo("test@example.com")
                .enabled(true)
                .build();
        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.of(preference));


        Notification notification = notificationService.sendNotification(request);

        assertEquals(NotificationStatus.SUCCEEDED, notification.getStatus());
        verify(notificationRepository, times(1)).save(notification);
    }

//    @Test
//    void testSendNotification_Failure() {
//        NotificationRequest request = new NotificationRequest(UUID.randomUUID(), "Test Subject", "Test Body");
//        NotificationPreference preference = NotificationPreference.builder()
//                .userId(UUID.randomUUID())
//                .contactInfo("test@example.com")
//                .enabled(true)
//                .build();
//        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.of(preference));
//
//        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
//
//        // Simulating a failure in sending email
//        doThrow(new RuntimeException("Mail sending failed")).when(mailSender).send(simpleMailMessage);
//
//        Notification notification = notificationService.sendNotification(request);
//
//        assertEquals(NotificationStatus.FAILED, notification.getStatus());
//        verify(mailSender, times(1)).send(simpleMailMessage);
//        verify(notificationRepository, times(1)).save(notification);
//    }

    @Test
    void testGetNotificationHistory() {
        List<Notification> notifications = Arrays.asList(new Notification(), new Notification());
        when(notificationRepository.findAllByUserIdAndDeletedIsFalse(any())).thenReturn(notifications);

        List<Notification> result = notificationService.getNotificationHistory(UUID.randomUUID());

        assertEquals(2, result.size());
        verify(notificationRepository, times(1)).findAllByUserIdAndDeletedIsFalse(any());
    }

    @Test
    void testChangeNotificationPreference() {
        NotificationPreference preference = NotificationPreference.builder()
                .userId(UUID.randomUUID())
                .contactInfo("test@example.com")
                .enabled(true)
                .build();

        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.of(preference));


        NotificationPreference result = notificationService.changeNotificationPreference(UUID.randomUUID(), false);

        assertFalse(result.isEnabled());
        verify(preferenceRepository, times(1)).save(result);
    }

    @Test
    void testClearNotifications() {
        List<Notification> notifications = Arrays.asList(new Notification(),new Notification());
        when(notificationRepository.findAllByUserIdAndDeletedIsFalse(any())).thenReturn(notifications);


        List<Notification> result = notificationService.clearNotifications(UUID.randomUUID());

        verify(notificationRepository, times(2)).save(any());
    }

    @Test
    void testRetryFailedNotifications() {
        NotificationPreference preference = NotificationPreference.builder()
                .userId(UUID.randomUUID())
                .contactInfo("test@example.com")
                .enabled(true)
                .build();
        List<Notification> failedNotifications = Arrays.asList(new Notification(),new Notification());
        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.of(preference));
        when(notificationRepository.findAllByUserIdAndStatus(any(), any())).thenReturn(failedNotifications);


        notificationService.retryFailedNotifications(UUID.randomUUID());

        verify(notificationRepository, times(2)).save(any());
//        verify(mailSender, times(2)).send(ArgumentMatchers.any());
    }

    @Test
    void testGetOrAddPreference_WhenExists() {
        UserWithNoPreference user = new UserWithNoPreference(UUID.randomUUID(), "user@example.com");
        NotificationPreference preference = NotificationPreference.builder()
                .userId(UUID.randomUUID())
                .contactInfo("test@example.com")
                .enabled(true)
                .build();
        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.of(preference));

        NotificationPreference result = notificationService.getOrAddPreference(user);

        assertEquals(preference.getUserId(), result.getUserId());
        verify(preferenceRepository, times(1)).findByUserId(any());
    }

    @Test
    void testGetOrAddPreference_WhenNotExists() {
        UserWithNoPreference user = new UserWithNoPreference(UUID.randomUUID(), "user@example.com");
        when(preferenceRepository.findByUserId(any())).thenReturn(Optional.empty());


        NotificationPreference result = notificationService.getOrAddPreference(user);

        assertNotNull(result);
        verify(preferenceRepository, times(1)).save(result);
    }

}