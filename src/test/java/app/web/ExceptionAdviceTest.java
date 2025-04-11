package app.web;

import app.web.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionAdviceTest {

    private final ExceptionAdvice exceptionAdvice = new ExceptionAdvice();

    @Test
    void testHandleNotFoundEndpoint() {
        // Act
        ResponseEntity<ErrorResponse> response = exceptionAdvice.handleNotFoundEndpoint();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getStatus());
        assertEquals("Not supported application endpoint.", body.getMessage());
    }

}