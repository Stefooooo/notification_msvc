package app.web;

import app.model.Notification;
import app.model.NotificationPreference;
import app.service.NotificationService;
import app.web.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;


    @Test
    void testUpsertNotificationPreference() {
        UpsertNotificationPreference request = new UpsertNotificationPreference();
        NotificationPreference preference = new NotificationPreference();
        when(notificationService.upsertPreference(request)).thenReturn(preference);

        ResponseEntity<NotificationPreferenceResponse> response = controller.upsertNotificationPreference(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetUserNotificationPreference() {
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = new NotificationPreference();
        when(notificationService.getPreferenceByUserId(userId)).thenReturn(preference);

        ResponseEntity<NotificationPreferenceResponse> response = controller.getUserNotificationPreference(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    void testSendNotification() {
        NotificationRequest request = new NotificationRequest();
        Notification notification = new Notification();
        when(notificationService.sendNotification(request)).thenReturn(notification);

        ResponseEntity<NotificationResponse> response = controller.sendNotification(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetNotificationHistory() {
        UUID userId = UUID.randomUUID();
        List<Notification> notifications = List.of(new Notification(), new Notification());
        when(notificationService.getNotificationHistory(userId)).thenReturn(notifications);

        ResponseEntity<List<NotificationResponse>> response = controller.getNotificationHistory(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testChangeNotificationPreference() {
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = new NotificationPreference();
        when(notificationService.changeNotificationPreference(userId, true)).thenReturn(preference);

        ResponseEntity<NotificationPreferenceResponse> response = controller.changeNotificationPreference(userId, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

//    @Test
//    void testClearNotificationHistory() {
//        UUID userId = UUID.randomUUID();
//        doNothing().when(notificationService).clearNotifications(any());
//
//        ResponseEntity<Void> response = controller.clearNotificationHistory(userId);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNull(response.getBody());
//    }

    @Test
    void testRetryFailedNotifications() {
        UUID userId = UUID.randomUUID();
        doNothing().when(notificationService).retryFailedNotifications(userId);

        ResponseEntity<Void> response = controller.retryFailedNotifications(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
    }

}