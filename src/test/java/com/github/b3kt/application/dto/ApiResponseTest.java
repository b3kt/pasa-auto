package com.github.b3kt.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    @DisplayName("Default constructor")
    void defaultConstructor() {
        ApiResponse<String> r = new ApiResponse<>();
        assertFalse(r.isSuccess());
        assertNull(r.getMessage());
        assertNull(r.getData());
        assertNull(r.getError());
    }

    @Test
    @DisplayName("Parameterized constructor")
    void parameterizedConstructor() {
        ApiResponse<String> r = new ApiResponse<>(true, "OK", "data");
        assertTrue(r.isSuccess());
        assertEquals("OK", r.getMessage());
        assertEquals("data", r.getData());
        assertNull(r.getError());
    }

    @Test
    @DisplayName("success(data) creates success response")
    void successData() {
        ApiResponse<Integer> r = ApiResponse.success(42);
        assertTrue(r.isSuccess());
        assertEquals("Success", r.getMessage());
        assertEquals(42, r.getData());
    }

    @Test
    @DisplayName("success(message, data) creates success response with custom message")
    void successMessageData() {
        ApiResponse<String> r = ApiResponse.success("Custom", "result");
        assertTrue(r.isSuccess());
        assertEquals("Custom", r.getMessage());
        assertEquals("result", r.getData());
    }

    @Test
    @DisplayName("error creates error response")
    void error() {
        ApiResponse<Void> r = ApiResponse.error("Something went wrong");
        assertFalse(r.isSuccess());
        assertEquals("Something went wrong", r.getError());
        assertNull(r.getData());
    }

    @Test
    @DisplayName("Setters work correctly")
    void setters() {
        ApiResponse<String> r = new ApiResponse<>();
        r.setSuccess(true);
        r.setMessage("msg");
        r.setData("d");
        r.setError("err");

        assertTrue(r.isSuccess());
        assertEquals("msg", r.getMessage());
        assertEquals("d", r.getData());
        assertEquals("err", r.getError());
    }
}
