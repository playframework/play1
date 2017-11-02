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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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

    private Blob(String coded) {
        if (coded != null && coded.length() > 0 && coded.contains("|")) {
            this.UUID = coded.split("[|]")[0];
            this.type = coded.split("[|]")[1];
        }
    }

    @Override
    public InputStream get() {
        if (exists()) {
            try {
                return new FileInputStream(getFile());
            } catch(Exception e) {
                throw new UnexpectedException(e);
            }
        }
        return null;
    }

    @Override
    public void set(InputStream is, String type) {
        this.UUID = Codec.UUID();
        this.type = type;
        IO.write(is, getFile());
    }

    @Override
    public long length() {
        return getFile().length();
    }

    @Override
    public String type() {
        return type;
    }

    @Override
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

    @Override
    public int[] sqlTypes() {
        return new int[] {Types.VARCHAR};
    }

    @Override
    public Class returnedClass() {
        return Blob.class;
    }

    private static boolean equal(Object a, Object b) {
      return a == b || (a != null && a.equals(b));
    }

    @Override
    public boolean equals(Object o, Object o1) throws HibernateException {
        if(o instanceof Blob && o1 instanceof Blob) {
            return equal(((Blob)o).UUID, ((Blob)o1).UUID) &&
                    equal(((Blob)o).type, ((Blob)o1).type);
        }
        return equal(o, o1);
    }

    @Override
    public int hashCode(Object o) throws HibernateException {
        return o.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        String val = (String) StringType.INSTANCE.nullSafeGet(rs, names[0], session, owner);
        return new Blob(val);
    }

    @Override
    public void nullSafeSet(PreparedStatement ps, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value != null) {
            ps.setString(index, encode((Blob) value));
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }

    private String encode(Blob o) {
        return o.UUID != null ? o.UUID + "|" + o.type : null;
    }

    @Override
    public Object deepCopy(Object o) throws HibernateException {
        if(o == null) {
            return null;
        }
        return new Blob(((Blob)o).UUID, ((Blob)o).type);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object o) throws HibernateException {
        if (o == null) return null;
        return encode((Blob) o);
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        if (cached == null) return null;
        return new Blob((String) cached);
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public static String getUUID(String dbValue) {
       return dbValue.split("[|]")[0];
    }

    public static File getStore() {
        String name = Play.configuration.getProperty("attachments.path", "attachments");
        File store = new File(name);
        if (!store.isAbsolute()) {
            store = Play.getFile(name);
        }
        if (!store.exists()) {
            store.mkdirs();
        }
        return store;
    }
    
}
