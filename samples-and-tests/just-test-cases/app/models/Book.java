package models;

import javax.persistence.Entity;
import javax.persistence.FieldResult;

import play.db.jpa.Model;

@Entity
public class Book extends Model {

    public Book(String title, String author, String content, int shelfNumber) {
        this.title = title;
        this.author = author;
        this.content = content;
        this.shelfNumber = shelfNumber;
    }

    public Book() {}

    public String title;
    public String author;
    public String content;
    public int shelfNumber;
}
