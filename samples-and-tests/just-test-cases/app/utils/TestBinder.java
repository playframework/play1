package utils;

import java.util.*;
import java.lang.annotation.*;
import play.data.binding.*;

public class TestBinder implements TypeBinder<String> {

    public Object bind(String name, Annotation[] annotations, String value, Class actualClass) {
        return "--" + value + "--";
    }
    
}

