package play.db.jpa;

import org.hibernate.dialect.MySQLInnoDBDialect;

public class MySQLDialect extends MySQLInnoDBDialect {

    @Override
    public String getTableTypeString() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8";
    }
    
}
