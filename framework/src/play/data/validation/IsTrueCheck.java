package play.data.validation;

public class IsTrueCheck extends AbstractAnnotationCheck<IsTrue> {

    static final String mes = "validation.isTrue";

    @Override
    public void configure(IsTrue isTrue) {
        setMessage(isTrue.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, PlayValidationContext context) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            try {
                return Boolean.parseBoolean(value.toString());
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Number) {
            try {
                return ((Number) value).doubleValue() != 0;
            } catch (Exception e) {
                return false;
            }
        }
        if (value instanceof Boolean) {
            try {
                return ((Boolean) value);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
   
}
