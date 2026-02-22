package play.db.jpa;

/**
 * InnoDB is the default engine in MySQL 5.5+. This dialect exists for
 * compatibility with configurations that reference play.db.jpa.MySQLDialect.
 */
public class MySQLDialect extends org.hibernate.dialect.MySQLDialect {

}
