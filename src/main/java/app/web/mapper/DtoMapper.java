package app.web.mapper;


import lombok.experimental.UtilityClass;
import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationType;
import app.web.dto.NotificationPreferenceResponse;
import app.web.dto.NotificationResponse;
import app.web.dto.NotificationTypeRequest;

@UtilityClass
public class DtoMapper {


    public static NotificationType fromNotificationTypeRequest(NotificationTypeRequest dto) {

        return switch (dto) {
            case EMAIL -> NotificationType.EMAIL;
        };
    }

    // Build dto from entity
    public static NotificationPreferenceResponse fromNotificationPreference(NotificationPreference entity) {

        return NotificationPreferenceResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .contactInfo(entity.getContactInfo())
                .enabled(entity.isEnabled())
                .userId(entity.getUserId())
                .build();
    }

    public static NotificationResponse fromNotification(Notification entity) {

        // DTO building!
        return NotificationResponse.builder()
                .subject(entity.getSubject())
                .status(entity.getStatus())
                .createdOn(entity.getCreatedOn())
                .type(entity.getType())
                .build();
    }
}
