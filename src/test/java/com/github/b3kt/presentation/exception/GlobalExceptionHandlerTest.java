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
    @DisplayName("OptimisticLockingExceptionMapper returns 400 without the internal message")
    void optimisticLockException() {
        GlobalExceptionHandler.OptimisticLockingExceptionMapper mapper =
                new GlobalExceptionHandler.OptimisticLockingExceptionMapper();
        Response response = mapper.toResponse(
                new OptimisticLockException("Row was updated by another transaction [TbSpkEntity#42]"));
        assertEquals(400, response.getStatus());
        String error = ((ApiResponse<?>) response.getEntity()).getError();
        assertEquals("This record was changed by someone else. Reload and try again.", error);
        assertFalse(error.contains("TbSpkEntity"));
    }

    @Test
    @DisplayName("IllegalArgumentExceptionMapper hides parse failures but keeps domain messages")
    void illegalArgumentParseFailure() {
        GlobalExceptionHandler.IllegalArgumentExceptionMapper mapper =
                new GlobalExceptionHandler.IllegalArgumentExceptionMapper();

        Response parse = mapper.toResponse(new NumberFormatException("For input string: \"not-a-number\""));
        assertEquals(400, parse.getStatus());
        assertEquals("Invalid value format", ((ApiResponse<?>) parse.getEntity()).getError());

        Response domain = mapper.toResponse(new IllegalArgumentException("Password must be at least 8 characters"));
        assertEquals("Password must be at least 8 characters", ((ApiResponse<?>) domain.getEntity()).getError());
    }

    @Test
    @DisplayName("EntityNotFoundExceptionMapper returns 404 without the entity/id")
    void entityNotFound() {
        GlobalExceptionHandler.EntityNotFoundExceptionMapper mapper =
                new GlobalExceptionHandler.EntityNotFoundExceptionMapper();
        Response response = mapper.toResponse(
                new jakarta.persistence.EntityNotFoundException("Entity not found with id: 42"));
        assertEquals(404, response.getStatus());
        assertEquals("Data not found", ((ApiResponse<?>) response.getEntity()).getError());
    }

    @Test
    @DisplayName("UnhandledExceptionMapper hides details behind a reference id")
    void unhandledException() {
        GlobalExceptionHandler.UnhandledExceptionMapper mapper =
                new GlobalExceptionHandler.UnhandledExceptionMapper();
        Response response = mapper.toResponse(
                new IllegalStateException("ERROR: relation \"users\" does not exist; SQL [select ...]"));

        assertEquals(500, response.getStatus());
        String error = ((ApiResponse<?>) response.getEntity()).getError();
        assertTrue(error.startsWith("Internal server error. Reference: "), error);
        assertFalse(error.contains("users"), error);
        assertFalse(error.contains("SQL"), error);
        // A different failure gets its own reference, so a log line can be found for each report
        String other = ((ApiResponse<?>) mapper.toResponse(new IllegalStateException("x")).getEntity()).getError();
        assertNotEquals(error, other);
    }

    @Test
    @DisplayName("UnhandledExceptionMapper passes JAX-RS responses through unchanged")
    void unhandledExceptionPassesThroughWebApplicationException() {
        GlobalExceptionHandler.UnhandledExceptionMapper mapper =
                new GlobalExceptionHandler.UnhandledExceptionMapper();

        Response response = mapper.toResponse(new jakarta.ws.rs.NotFoundException());

        assertEquals(404, response.getStatus());
    }

    @SuppressWarnings("unchecked")
    private <T> ConstraintViolation<T> mockConstraintViolation(String message) {
        ConstraintViolation<T> v = mock(ConstraintViolation.class);
        when(v.getMessage()).thenReturn(message);
        return v;
    }
}
