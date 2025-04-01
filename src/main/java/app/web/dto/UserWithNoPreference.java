package app.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserWithNoPreference {

    private UUID id;

    private String email;

}
