package models;

import javax.persistence.Entity;
import javax.persistence.FieldResult;

import play.db.jpa.Model;
import play.modules.search.Field;
import play.modules.search.Indexed;

@Entity
@Indexed
public class Book extends Model {
    
    public Book(String title, String author, String content, int shelfNumber) {
        this.title = title;
        this.author = author;
        this.content = content;
        this.shelfNumber = shelfNumber;
    }
    
    public Book() {}
    @Field
    public String title;
    @Field
    public String author;
    @Field
    public String content;
    @Field
    public int shelfNumber;
}
