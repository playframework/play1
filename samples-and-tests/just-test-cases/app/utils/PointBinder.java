package utils;

import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import play.data.binding.*;
import java.awt.Point;

@Global
public class PointBinder implements TypeBinder<Point> {

    public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
        String[] values = value.split(",");
        return new Point(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
    }
    
}

