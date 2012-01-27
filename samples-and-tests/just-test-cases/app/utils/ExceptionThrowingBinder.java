package utils;

import java.lang.reflect.*;
import java.lang.annotation.*;
import play.data.binding.*;
import play.exceptions.BinderException;

@Global
public class ExceptionThrowingBinder implements TypeBinder<ExceptionThrowingSubject> {

    public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
        throw new BinderException("Could not create ExceptionThrowingSubject");
    }

}

