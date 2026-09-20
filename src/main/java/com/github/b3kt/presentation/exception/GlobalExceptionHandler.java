package com.github.b3kt.presentation.exception;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.domain.exception.TooManyAttemptsException;
import com.github.b3kt.domain.exception.UserNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

/**
 * Global exception handler for REST endpoints.
 * Maps domain exceptions to appropriate HTTP responses.
 */
@Provider
public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Provider
    public static class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {
        @Override
        public Response toResponse(AuthenticationException exception) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error(exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class TooManyAttemptsExceptionMapper implements ExceptionMapper<TooManyAttemptsException> {
        @Override
        public Response toResponse(TooManyAttemptsException exception) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .header("Retry-After", exception.getRetryAfterSeconds())
                    .entity(ApiResponse.error(exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class UserNotFoundExceptionMapper implements ExceptionMapper<UserNotFoundException> {
        @Override
        public Response toResponse(UserNotFoundException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error(exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
        @Override
        public Response toResponse(ConstraintViolationException exception) {
            String message = exception.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Validation error: " + message))
                    .build();
        }
    }

    @Provider
    public static class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
        @Override
        public Response toResponse(IllegalArgumentException exception) {
            // Domain rules (e.g. the password policy) phrase these for the user; a failed parse does not
            String message = exception instanceof NumberFormatException
                    ? "Invalid value format"
                    : exception.getMessage();
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(message))
                    .build();
        }
    }


    @Provider
    public static class OptimisticLockingExceptionMapper implements ExceptionMapper<OptimisticLockException> {
        @Override
        public Response toResponse(OptimisticLockException exception) {
            // The raw message names entity classes and identifiers; the user only needs to retry
            LOG.debug("Optimistic lock conflict", exception);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("This record was changed by someone else. Reload and try again."))
                    .build();
        }
    }

    @Provider
    public static class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {
        @Override
        public Response toResponse(EntityNotFoundException exception) {
            LOG.debug("Entity not found", exception);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Data not found"))
                    .build();
        }
    }

    /**
     * Last resort for anything not mapped above: the client gets a generic message and a reference id,
     * while the details (stack trace, SQL, entity names) stay in the log under that same id.
     */
    @Provider
    public static class UnhandledExceptionMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception exception) {
            // Responses built by JAX-RS itself (404, 405, security failures) must pass through unchanged
            if (exception instanceof WebApplicationException webApplicationException) {
                return webApplicationException.getResponse();
            }
            String errorId = UUID.randomUUID().toString();
            LOG.errorf(exception, "Unhandled exception [errorId=%s]", errorId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error. Reference: " + errorId))
                    .build();
        }
    }
}

