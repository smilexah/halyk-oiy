package sdu.ai.lab.authservice.exceptions.handler;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Ответ с информацией об ошибке")
public class ErrorResponse {
    @Schema(description = "Время возникновения ошибки", example = "2025-12-17T10:08:26.865")
    private LocalDateTime timestamp;
    
    @Schema(description = "HTTP статус код", example = "400")
    private int status;
    
    @Schema(description = "Краткое описание ошибки", example = "Неверный запрос")
    private String error;
    
    @Schema(description = "Подробное сообщение об ошибке", example = "Превышен максимальный размер загружаемого файла")
    private String message;
    
    @Schema(description = "Ошибки валидации полей", nullable = true)
    private Map<String, String> validationErrors;
}
