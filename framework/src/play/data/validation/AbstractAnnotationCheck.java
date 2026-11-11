package play.data.validation;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import play.exceptions.UnexpectedException;

/**
 * Base class for Play validation checks, replacing the oVal
 * {@code net.sf.oval.configuration.annotation.AbstractAnnotationCheck}.
 *
 * <p>A check is configured from its annotation via {@link #configure(Annotation)}
 * and evaluated via {@link #isSatisfied(Object, Object, PlayValidationContext)}. The message
 * key reported on failure defaults to the static {@code mes} field of the check and
 * can be overridden with {@link #setMessage(String)}.</p>
 */
public abstract class AbstractAnnotationCheck<A extends Annotation> {

    /**
     * Override of the default message key, set from the annotation message or programmatically.
     */
    private String message;

    private Map<String, String> messageVariables;

    /**
     * Configures this check from its annotation.
     */
    public void configure(A annotation) {
    }

    /**
     * Evaluates the check.
     *
     * @param validatedObject the object owning the validated value (may be {@code null})
     * @param value the value to validate (may be {@code null})
     * @param context where the check is applied: field or method parameter, or {@code null}
     * @return true if the value satisfies the check
     */
    public abstract boolean isSatisfied(Object validatedObject, Object value, PlayValidationContext context);

    /**
     * Sets the message key reported when this check is not satisfied.
     */
    public void setMessage(String message) {
        this.message = message;
        requireMessageVariablesRecreation();
    }

    /**
     * @return The message key of this check: the message set via the annotation or
     *         programmatically, else the static {@code mes} field of the check class.
     */
    public String getMessageKey() {
        if (message != null) {
            return message;
        }
        try {
            return (String) getClass().getDeclaredField("mes").get(null);
        } catch (Exception e) {
            throw new UnexpectedException("Check " + getClass().getName() + " has no static mes field", e);
        }
    }

    /**
     * @return The message key of this check ({@link #getMessageKey()}).
     */
    public String getMessage() {
        return getMessageKey();
    }

    /**
     * Message variables exposed to the error message ({@code $2}, {@code $3}, ...).
     * Cached; call {@link #requireMessageVariablesRecreation()} after mutating state.
     */
    public Map<String, String> getMessageVariables() {
        if (messageVariables == null) {
            // LinkedHashMap preserves the order of the source map (oVal returned the
            // createMessageVariables() map itself); a HashMap would reorder entries by
            // bucket hash and shuffle the message parameters ($2, $3, ...)
            messageVariables = new LinkedHashMap<>(createMessageVariables());
        }
        return messageVariables;
    }

    /**
     * Forces {@link #createMessageVariables()} to be recomputed on next access.
     */
    protected void requireMessageVariablesRecreation() {
        messageVariables = null;
    }

    /**
     * Builds the message variables of this check.
     */
    protected Map<String, String> createMessageVariables() {
        return new HashMap<>();
    }

    /**
     * Severity of the errors produced by this check.
     */
    public int getSeverity() {
        return 0;
    }
}
