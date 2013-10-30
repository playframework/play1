package play.deps;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.plugins.conflict.AbstractConflictManager;
import org.apache.ivy.plugins.conflict.LatestConflictManager;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;

public class PlayConflictManager extends AbstractConflictManager {

    public LatestConflictManager deleguate = new LatestConflictManager(new LatestRevisionStrategy());

    public Collection resolveConflicts(IvyNode in, Collection conflicts) {

        // No conflict
        if (conflicts.size() < 2) {
            return conflicts;
        }

        // Force
        for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            DependencyDescriptor dd = node.getDependencyDescriptor(in);
            if (dd != null && dd.isForce() && in.getResolvedId().equals(dd.getParentRevisionId())) {
                return Collections.singleton(node);
            }
        }

        boolean foundBuiltInDependency = false;
        for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            ModuleRevisionId modRev = node.getResolvedId();
            File jar = new File(System.getProperty("play.path") + "/framework/lib/" + modRev.getName() + "-" + modRev.getRevision() + ".jar");
            if(jar.exists()) {
               foundBuiltInDependency = true;
               break;
            }
        }

        if(!foundBuiltInDependency) {
            return deleguate.resolveConflicts(in, conflicts);
        }

        /**
         * Choose the artifact version provided in $PLAY/framework/lib
         * Evict other versions
         */
        List<IvyNode> result = new ArrayList<IvyNode>();
        for (Iterator iter = conflicts.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode) iter.next();
            ModuleRevisionId modRev = node.getResolvedId();
            File jar = new File(System.getProperty("play.path") + "/framework/lib/" + modRev.getName() + "-" + modRev.getRevision() + ".jar");
            if (jar.exists()) {
                result.add(node);
            }
        }

        return result;
    }
}
