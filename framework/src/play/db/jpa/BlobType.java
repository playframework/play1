package play.db.jpa;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class BlobType implements UserType<Blob> {

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
        return o != o1 && o != null && o1 != null
            && Objects.equals(o.getUUID(), o1.getUUID())
            && Objects.equals(o.type(), o1.type());
    }

    @Override
    public int hashCode(Blob o) throws HibernateException {
        return o.hashCode();
    }

    @Override
    public Blob nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        return new Blob(rs.getString(position));
    }

    @Override
    public void nullSafeSet(PreparedStatement ps, Blob value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value != null) {
            ps.setString(index, encode(value));
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }

    @Override
    public Blob deepCopy(Blob o) throws HibernateException {
        if (o == null) {
            return null;
        }

        return new Blob(o.getUUID(), o.type());
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

    private String encode(Blob o) {
        return o.getUUID() != null ? o.getUUID() + '|' + o.type() : null;
    }

    public static String getUUID(String dbValue) {
       return dbValue.split("[|]", 2)[0];
    }

}
