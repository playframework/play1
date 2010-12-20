package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class User extends Model {

    public long uid;
    public String access_token;

    public User(long uid) {
        this.uid = uid;
    }

    public static User get(long id) {
        return find("uid", id).first();
    }

    public static User createNew() {
        long uid = (long)Math.floor(Math.random() * 10000);
        User user = new User(uid);
        user.create();
        return user;
    }

}
