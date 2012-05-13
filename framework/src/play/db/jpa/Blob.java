package play.db.jpa;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

import play.Play;
import play.db.Model.BinaryField;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.IO;

public class Blob implements BinaryField, UserType {

    private String UUID;
    private String type;
    private File file;

    public Blob() {}

    private Blob(String UUID, String type) {
        this.UUID = UUID;
        this.type = type;
    }
    
    public InputStream get() {
        if(exists()) {
            try {
                return new FileInputStream(getFile());
            } catch(Exception e) {
                throw new UnexpectedException(e);
            }
        }
        return null;
    }
    
    public void set(InputStream is, String type) {
        this.UUID = Codec.UUID();
        this.type = type;
        IO.write(is, getFile());
    }

    public long length() {
        return getFile().length();
    }

    public String type() {
        return type;
    }

    public boolean exists() {
        return UUID != null && getFile().exists();
    }

    public File getFile() {
        if(file == null) {
            file = new File(getStore(), UUID);
        }
        return file;
    }
    
    public String getUUID()  {
        return UUID;
    }

    //

    public int[] sqlTypes() {
        return new int[] {Types.VARCHAR};
    }

    public Class returnedClass() {
        return Blob.class;
    }

    private static boolean equal(Object a, Object b) {
      return a == b || (a != null && a.equals(b));
    }

    public boolean equals(Object o, Object o1) throws HibernateException {
        if(o instanceof Blob && o1 instanceof Blob) {
            return equal(((Blob)o).UUID, ((Blob)o1).UUID) &&
                    equal(((Blob)o).type, ((Blob)o1).type);
        }
        return equal(o, o1);
    }

    public int hashCode(Object o) throws HibernateException {
        return o.hashCode();
    }

    public Object nullSafeGet(ResultSet resultSet, String[] names, Object o) throws HibernateException, SQLException {
       	String val = (String) StringType.INSTANCE.get(resultSet, names[0]);

        if(val == null || val.length() == 0 || !val.contains("|")) {
            return new Blob();
        }
        return new Blob(val.split("[|]")[0], val.split("[|]")[1]);
    }

    public void nullSafeSet(PreparedStatement ps, Object o, int i) throws HibernateException, SQLException {
         if(o != null) {
            ps.setString(i, ((Blob)o).UUID + "|" + ((Blob)o).type);
        } else {
            ps.setNull(i, Types.VARCHAR);
        }
    }

    public Object deepCopy(Object o) throws HibernateException {
        if(o == null) {
            return null;
        }
        return new Blob(((Blob)o).UUID, ((Blob)o).type);
    }

    public boolean isMutable() {
        return true;
    }

    public Serializable disassemble(Object o) throws HibernateException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object assemble(Serializable srlzbl, Object o) throws HibernateException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object replace(Object o, Object o1, Object o2) throws HibernateException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //

    public static String getUUID(String dbValue) {
       return dbValue.split("[|]")[0];
    }

    public static File getStore() {
        String name = Play.configuration.getProperty("attachments.path", "attachments");
        File store = null;
        if(new File(name).isAbsolute()) {
            store = new File(name);
        } else {
            store = Play.getFile(name);
        }
        if(!store.exists()) {
            store.mkdirs();
        }
        return store;
    }
    
}
