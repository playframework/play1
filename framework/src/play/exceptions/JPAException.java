package play.exceptions;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.exception.GenericJDBCException;

/**
 * JPA exception
 */
public class JPAException extends PlayException implements SourceAttachment {

    public JPAException(String message) {
        super(message, null);
    }

    public JPAException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorTitle() {
        return "JPA error";
    }

    @Override
    public String getErrorDescription() {
        if(getCause() != null && getCause() instanceof GenericJDBCException) {
            String SQL = ((GenericJDBCException)getCause()).getSQL();
            return String.format("A JPA error occurred (%s): <strong>%s</strong>. This is likely because the batch has broken some referential integrity. Check your cascade delete, in case of ...", getMessage(), getCause() == null ? "" : getCause().getMessage(), SQL);
        }
        return String.format("A JPA error occurred (%s): <strong>%s</strong>", getMessage(), getCause() == null ? "" : getCause().getMessage());
    }

    @Override
    public boolean isSourceAvailable() {
        return getCause() != null && getCause() instanceof GenericJDBCException;
    }   

    @Override
    public Integer getLineNumber() {
        return 1;
    }

    public List<String> getSource() {
        List<String> sql = new ArrayList<String>();
        if(getCause() != null && getCause() instanceof GenericJDBCException) {
            sql.add(((GenericJDBCException)getCause()).getSQL());
        }
        return sql;
    }

    @Override
    public String getSourceFile() {
        return "SQL Statement";
    }   
    
    
    
}
