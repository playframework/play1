package play.modules.gae;

import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.PackageMetaData;
import play.classloading.ApplicationClasses.ApplicationClass;

public class PlayModelClassMetaData extends ClassMetaData {

    public PlayModelClassMetaData(ApplicationClass applicationClass) {
        super((PackageMetaData)null, applicationClass.name);
    }
    
}
