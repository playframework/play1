package play.exceptions;

import org.hibernate.exception.GenericJDBCException;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class JPAExceptionTest {
    @Test
    public void errorDescription_messageOnly() {
        assertEquals("A JPA error occurred (ups): <strong></strong>", new JPAException("ups").getErrorDescription());
    }

    @Test
    public void errorDescription_messageWithCause() {
        assertEquals("A JPA error occurred (ups): <strong>it happened</strong>", 
                new JPAException("ups", new IllegalArgumentException("it happened")).getErrorDescription());
    }

    @Test
    public void errorDescription_messageWithCause_GenericJDBCException() {
        assertEquals("A JPA error occurred (ups): <strong>sql fail</strong>. This is likely because the batch has broken some referential integrity. Check your cascade delete. SQL: (select - from dual)", 
                new JPAException("ups", new GenericJDBCException("sql fail", new SQLException("ORA-666"), "select - from dual")).getErrorDescription());
    }
}