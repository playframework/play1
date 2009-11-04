package play.db.jpa;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

public class StringList extends ArrayList<String> implements UserType {
    
	private static final long serialVersionUID = -5145518193896935839L;

	public StringList() {
        super();
    }
    
    public StringList(StringList s) {
        super(s);
    }

    public int[] sqlTypes() {
        return new int[]{Types.VARCHAR};
    }

    public Class<?> returnedClass() {
        return StringList.class;
    }

    public boolean equals(Object arg0, Object arg1) throws HibernateException {
        return arg0.equals(arg1);
    }

    public int hashCode(Object arg0) throws HibernateException {
        return arg0.hashCode();
    }

    public Object nullSafeGet(ResultSet inResultSet, String[] names, Object o) throws HibernateException, SQLException {
        char separator = getSeparator();
        String val = (String) Hibernate.STRING.nullSafeGet(inResultSet, names[0]);
        StringList stringList = new StringList();
        for(String s : val.split(""+separator)) {
            if(s.length() > 0) {
                stringList.add(s);
            }
        }
        return stringList;
    }

    public void nullSafeSet(PreparedStatement inPreparedStatement, Object o, int i) throws HibernateException, SQLException {
        char separator = getSeparator();
        StringBuffer sb = new StringBuffer();
        for(String s : (StringList)o) {
            if(sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(s);
        }
        inPreparedStatement.setString(i, sb.toString());
    }

    public Object deepCopy(Object o) throws HibernateException {
        if(o == null) {
            return null;
        }
        return new StringList((StringList)o);
    }

    public boolean isMutable() {
        return true;
    }

    public Serializable disassemble(Object o) throws HibernateException {
        return (Serializable)o;
    }

    public Object assemble(Serializable o, Object owner) throws HibernateException {
        return o;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }
    
    public char getSeparator() {
        return '|';
    }
}
