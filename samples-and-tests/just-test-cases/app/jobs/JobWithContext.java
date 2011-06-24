package jobs;

import java.util.*;

import play.*;
import play.jobs.*;

import utils.*;

@Youhou("fromJob")
public class JobWithContext extends Job<String> {

    public String doJobWithResult() {
        return Invoker.InvocationContext.current().toString();
    }
    
}

