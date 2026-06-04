package kz.halyk.maqsat.ai.exceptions;

public class AiResponseException extends RuntimeException {
    public AiResponseException(String message) {
        super(message);
    }

    public AiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
