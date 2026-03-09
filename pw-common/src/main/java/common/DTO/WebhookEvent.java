package common.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Common DTO for webhook events received from PonteWire.
 * Represents the raw event payload with its source, event data, and timestamp.
 * Implemented as a record to keep the data structure concise and immutable.
 */

public record WebhookEvent(
        @NotBlank(message = "Source must not be blank")
        String source,

        @NotNull(message = "Data must not be null")
        Map<String, Object> data,

        LocalDateTime timestamp
) {}