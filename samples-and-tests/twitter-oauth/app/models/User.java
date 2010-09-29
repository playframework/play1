package models;

import javax.persistence.Entity;

import play.db.jpa.Model;
import play.libs.OAuth.TokenPair;

@Entity
public class User extends Model {

    public String username;
    public String token;
    public String secret;

    public User(String username) {
        this.username = username;
    }

    public TokenPair getTokenPair() {
        return new TokenPair(token, secret);
    }

    public void setTokenPair(TokenPair tokens) {
        this.token = tokens.token;
        this.secret = tokens.secret;
        this.save();
    }

    public static User findOrCreate(String username) {
        User user = User.find("username", username).first();
        if (user == null) {
            user = new User(username);
        }
        return user;
    }

}
