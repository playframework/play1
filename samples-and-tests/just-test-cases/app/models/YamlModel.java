package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class YamlModel extends GenericModel {

    @Id
    public Long id;
    public String name;
    public byte[] binaryData;
    
}