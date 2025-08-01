package models;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import play.db.jpa.Model;

@Entity
public class Author extends Model {

    public String name;

    @OneToMany(mappedBy="author")
    public List<Book> books;

    public Date birthday;

}
