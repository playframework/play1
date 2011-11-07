package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;
import play.data.validation.Unique;

@Entity
public class Book extends Model {
    @Unique("author")
    public String title;

    @Unique
    public String isbn;

    @ManyToOne()
    public Author author;


}
