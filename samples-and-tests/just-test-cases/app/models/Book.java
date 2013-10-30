package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.data.validation.Unique;
import play.db.jpa.GenericModel;

@Entity
public class Book extends GenericModel {

    @Id
    @GeneratedValue
    @Column(name="pk_id")
    public Long id;

    public Long getId() {
        return id;
    }

    @Override
    public Object _key() {
        return getId();
    }

    @Unique("author")
    @Column(name="col_title")
    public String title;

    @Unique
    @Column(name="col_isbn")
    public String isbn;

    @ManyToOne()
    public Author author;


}
