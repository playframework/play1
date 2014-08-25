import java.util.*;

import cn.bran.japid.util.JapidFlags;
import play.db.jpa.JPA;
import play.jobs.*;
import play.test.*;
import models.*;

@OnApplicationStart
public class Bootstrap extends Job {

    public void doJob() {
    	JapidFlags.setLogLevelDebug();
        if(JPA.count(Contact.class) == 0) {
            Fixtures.load("data.yml");
        }
    }
    
}

