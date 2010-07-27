package play.db;

import java.lang.reflect.Field;
import java.util.List;

public class ModelProperty {

    public String name;
    public Class<?> type;
    public Field field;
    public boolean isRelation;
    public boolean isMultiple;
    public List<Object> choices;
    public String relation;
    
}
