package play.db.evolutions;

import play.libs.Codec;


public class Evolution implements Comparable<Evolution> {
    public final int revision;
    public final String sql_up;
    public final String sql_down;
    public final String hash;
    public final boolean applyUp;
    
    public final String moduleKey;

    public Evolution(String moduleKey, int revision, String sql_up, String sql_down, boolean applyUp) {
        this.moduleKey = moduleKey;
        this.revision = revision;
        this.sql_down = sql_down;
        this.sql_up = sql_up;
        this.hash = Codec.hexSHA1(sql_up + sql_down);
        this.applyUp = applyUp;
    }

    @Override
    public int compareTo(Evolution o) {
        return this.revision - o.revision;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Evolution) && ((Evolution) obj).revision == this.revision;
    }

    @Override
    public int hashCode() {
        return revision;
    }
}