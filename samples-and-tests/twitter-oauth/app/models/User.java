package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class User extends Model {

    public String username;
    public String token;
    public String secret;

    public User(String username) {
        this.username = username;
    }

    public static User findOrCreate(String username) {
        User user = User.find("username", username).first();
        if (user == null) {
            user = new User(username);
        }
        return user;
    }

}
