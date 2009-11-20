package play.data.validation;

public abstract class Check {

    public CheckWithCheck checkWithCheck;

    public Check() {

    }

    public abstract boolean isSatisfied(Object validatedObject, Object value);

    public void setMessage(String message, String... vars) {
        checkWithCheck.setMessage(message);
        checkWithCheck.variables.clear();
        for (String variable : vars) {
            checkWithCheck.variables.put("var" + (variable.length()), variable);
        }
        checkWithCheck.setVariables();
    }
}
