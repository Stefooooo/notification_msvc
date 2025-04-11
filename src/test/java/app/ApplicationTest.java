package app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {
    @Test
    void testMainMethodRunsWithoutException() {
        assertDoesNotThrow(() -> Application.main(new String[]{}));
    }

}