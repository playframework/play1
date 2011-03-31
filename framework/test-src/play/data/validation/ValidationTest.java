package play.data.validation;

import org.junit.Test;
import play.i18n.MessagesBuilder;

import static org.fest.assertions.Assertions.assertThat;

public class ValidationTest {

    @Test
    public void verifyError() {

        new MessagesBuilder().build();

        Validation.current.set(new Validation());

        final String field = "f1";

        assertThat(Validation.error(field)).isNull();
        assertThat(Validation.errors(field)).isEmpty();

        final String errorMsg = "My errorMessage";

        Validation.addError(field, errorMsg);

        assertThat( Validation.error(field).message).isEqualTo(errorMsg);
        assertThat(Validation.errors(field)).containsOnly(Validation.error(field));

        // ticket [#109] - add an error with null-key
        Validation.addError(null, errorMsg);
        // make sure this null key does not break stuff
        assertThat( Validation.error(field).message).isEqualTo(errorMsg);
        assertThat(Validation.errors(field)).containsOnly(Validation.error(field));

    }

}
