package models;

import play.db.jpa.Model;

public class User extends Model {
    public String name;
    public int age;

    public User() {
        super();
    }

    public User(final String name, final int age) {
        super();

        this.name = name;
        this.age = age;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public static int compareByNameThenAge(User lUser, User rUser) {
        if (lUser.name.equals(rUser.name)) {
            return lUser.age - rUser.age;
        } else {
            return lUser.name.compareTo(rUser.name);
        }
    }
}