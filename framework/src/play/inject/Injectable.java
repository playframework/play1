package play.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import play.Play;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.jobs.Job;
import play.mvc.Mailer;

/**
 * if you want to inject beans anywhere,please implement the interface.
 *
 */
public interface Injectable {
}
