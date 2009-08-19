package play.db.jpa;

import org.hibernate.dialect.MySQLInnoDBDialect;

/**
 * InnoDB, UTF-8 dialect for Mysql
 */
public class MySQLDialect extends MySQLInnoDBDialect {

    @Override
    public String getTableTypeString() {
        return " ENGINE=InnoDB DEFAULT CHARSET=utf8";
    }
    
}
