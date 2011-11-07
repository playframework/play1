package models;


import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class RegionalArticlePk implements Serializable {

    public String key1;
    public String key2;

    public RegionalArticlePk(String key1, String key2) {
        this.key1 = key1;
        this.key2 = key2;
    }

    @Override
    public String toString() {
        return "RegionalArticlePk{" +
                "key1='" + key1 + '\'' +
                ", key2='" + key2 + '\'' +
                '}';
    }
}

