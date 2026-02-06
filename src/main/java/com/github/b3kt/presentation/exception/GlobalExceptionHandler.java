package com.github.b3kt.presentation.exception;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.domain.exception.UserNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

/**
 * Global exception handler for REST endpoints.
 * Maps domain exceptions to appropriate HTTP responses.
 */
@Provider
public class GlobalExceptionHandler {

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
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(exception.getMessage()))
                    .build();
        }
    }
}

