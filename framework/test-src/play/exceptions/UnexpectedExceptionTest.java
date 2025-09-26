package play.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnexpectedExceptionTest {
    @Test
    public void errorDescription_messageOnly() {
        assertEquals("Unexpected error : ups", new UnexpectedException("ups").getErrorDescription());
    }

    @Test
    public void errorDescription_messageWithCause() {
        assertEquals("Unexpected error : ups, caused by exception <strong>IllegalArgumentException</strong>:<br/> <strong>forbidden</strong>", 
                new UnexpectedException("ups", new IllegalArgumentException("forbidden")).getErrorDescription());
    }
}