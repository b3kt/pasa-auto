package com.github.b3kt.presentation.exception;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.domain.exception.UserNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("AuthenticationExceptionMapper returns 401")
    void authenticationException() {
        GlobalExceptionHandler.AuthenticationExceptionMapper mapper =
                new GlobalExceptionHandler.AuthenticationExceptionMapper();
        Response response = mapper.toResponse(new AuthenticationException("Invalid credentials"));
        assertEquals(401, response.getStatus());
        ApiResponse<?> entity = (ApiResponse<?>) response.getEntity();
        assertEquals("Invalid credentials", entity.getError());
    }

    @Test
    @DisplayName("UserNotFoundExceptionMapper returns 404")
    void userNotFoundException() {
        GlobalExceptionHandler.UserNotFoundExceptionMapper mapper =
                new GlobalExceptionHandler.UserNotFoundExceptionMapper();
        Response response = mapper.toResponse(new UserNotFoundException("User not found"));
        assertEquals(404, response.getStatus());
        ApiResponse<?> entity = (ApiResponse<?>) response.getEntity();
        assertEquals("User not found", entity.getError());
    }

    @Test
    @DisplayName("ConstraintViolationExceptionMapper returns 400 with validation messages")
    void constraintViolationException() {
        GlobalExceptionHandler.ConstraintViolationExceptionMapper mapper =
                new GlobalExceptionHandler.ConstraintViolationExceptionMapper();

        ConstraintViolation<?> violation = mockConstraintViolation("must not be blank");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        Response response = mapper.toResponse(exception);
        assertEquals(400, response.getStatus());
        ApiResponse<?> entity = (ApiResponse<?>) response.getEntity();
        assertTrue(entity.getError().contains("must not be blank"));
    }

    @Test
    @DisplayName("ConstraintViolationExceptionMapper joins multiple messages")
    void constraintViolationExceptionMultiple() {
        GlobalExceptionHandler.ConstraintViolationExceptionMapper mapper =
                new GlobalExceptionHandler.ConstraintViolationExceptionMapper();

        ConstraintViolation<?> v1 = mockConstraintViolation("must not be blank");
        ConstraintViolation<?> v2 = mockConstraintViolation("size must be between 3 and 20");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(v1, v2));

        Response response = mapper.toResponse(exception);
        assertEquals(400, response.getStatus());
    }

    @Test
    @DisplayName("IllegalArgumentExceptionMapper returns 400")
    void illegalArgumentException() {
        GlobalExceptionHandler.IllegalArgumentExceptionMapper mapper =
                new GlobalExceptionHandler.IllegalArgumentExceptionMapper();
        Response response = mapper.toResponse(new IllegalArgumentException("Bad request"));
        assertEquals(400, response.getStatus());
        assertEquals("Bad request", ((ApiResponse<?>) response.getEntity()).getError());
    }

    @Test
    @DisplayName("OptimisticLockingExceptionMapper returns 400")
    void optimisticLockException() {
        GlobalExceptionHandler.OptimisticLockingExceptionMapper mapper =
                new GlobalExceptionHandler.OptimisticLockingExceptionMapper();
        Response response = mapper.toResponse(new OptimisticLockException("Version conflict"));
        assertEquals(400, response.getStatus());
        assertEquals("Version conflict", ((ApiResponse<?>) response.getEntity()).getError());
    }

    @SuppressWarnings("unchecked")
    private <T> ConstraintViolation<T> mockConstraintViolation(String message) {
        ConstraintViolation<T> v = mock(ConstraintViolation.class);
        when(v.getMessage()).thenReturn(message);
        return v;
    }
}
