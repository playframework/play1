package play.data.validation;

import java.lang.reflect.Field;

/**
 * {@link PlayValidationContext} describing a field of the validated object.
 */
public final class FieldValidationContext implements PlayValidationContext {

    private final Field field;

    FieldValidationContext(Field field) {
        this.field = field;
    }

    public Field getField() {
        return field;
    }
}
