package org.aeroguard.gateway.exception;

/**
 * Domain exception thrown when event publishing to message broker fails or times out.
 */
public class EventPublishingException extends RuntimeException {

    public EventPublishingException(String message) {
        super(message);
    }

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
