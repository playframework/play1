package play.data.validation;

public abstract class Check {

    public CheckWithCheck checkWithCheck;

    public Check() {

    }

    public abstract boolean isSatisfied(Object validatedObject, Object value);

    public void setMessage(String message, String... vars) {
        checkWithCheck.setMessage(message);
        checkWithCheck.variables.clear();
        for (int i = 0; i < vars.length; i++) {
            checkWithCheck.variables.put("var" + i, vars[i]);
        }
        checkWithCheck.setVariables();
    }

    public CheckWithCheck getCheckWithCheck() {
        return this.checkWithCheck;
    }

}
