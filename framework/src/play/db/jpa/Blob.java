package play.db.jpa;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import play.Play;
import play.db.Model.BinaryField;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.IO;

public class Blob implements BinaryField, UserType<Blob> {

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
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public Class<Blob> returnedClass() {
        return Blob.class;
    }

    @Override
    public boolean equals(Blob o, Blob o1) throws HibernateException {
        if(o instanceof Blob && o1 instanceof Blob) {
            return Objects.equals(o.UUID, o1.UUID) &&
                Objects.equals(o.type, o1.type);
        }
        return Objects.equals(o, o1);
    }

    @Override
    public int hashCode(Blob o) throws HibernateException {
        return o.hashCode();
    }

    @Override
    public Blob nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        String val = rs.getString(position);
        return new Blob(val);
    }

    @Override
    public void nullSafeSet(PreparedStatement ps, Blob value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value != null) {
            ps.setString(index, encode(value));
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }

    private String encode(Blob o) {
        return o.UUID != null ? o.UUID + "|" + o.type : null;
    }

    @Override
    public Blob deepCopy(Blob o) throws HibernateException {
        if(o == null) {
            return null;
        }
        return new Blob(o.UUID, o.type);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Blob o) throws HibernateException {
        if (o == null) return null;
        return encode(o);
    }

    @Override
    public Blob assemble(Serializable cached, Object owner) throws HibernateException {
        if (cached == null) return null;
        return new Blob((String) cached);
    }

    @Override
    public Blob replace(Blob original, Blob target, Object owner) throws HibernateException {
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
