package kz.halyk.maqsat.recommendation.exception;

public class AiResponseException extends RuntimeException {
    public AiResponseException(String message) {
        super(message);
    }

    public AiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
