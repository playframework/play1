package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Nose extends Model {
    
    @Column(nullable = false)
    public String title;
    
    public Nose delete() {
        Face face = Face.find("nose", this).first();
        face.nose = null;
        face.save();
        return super.delete();
    }
    
}

