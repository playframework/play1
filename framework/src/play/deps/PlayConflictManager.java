package play.deps;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.plugins.conflict.AbstractConflictManager;
import org.apache.ivy.plugins.conflict.LatestConflictManager;
import org.apache.ivy.plugins.latest.LatestRevisionStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PlayConflictManager extends AbstractConflictManager {

    public LatestConflictManager delegate = new LatestConflictManager(new LatestRevisionStrategy());

    @Override
    public Collection<IvyNode> resolveConflicts(IvyNode in, Collection conflictsUntyped) {

        Collection<IvyNode> conflicts = conflictsUntyped;
        
        // No conflict
        if (conflicts.size() < 2) {
            return conflicts;
        }

        // Force
        for (IvyNode node : conflicts) {
            DependencyDescriptor dd = node.getDependencyDescriptor(in);
            if (dd != null && dd.isForce() && in.getResolvedId().equals(dd.getParentRevisionId())) {
                return Collections.singleton(node);
            }
        }

        boolean foundBuiltInDependency = false;
        for (IvyNode node : conflicts) {
            ModuleRevisionId modRev = node.getResolvedId();
            File jar = new File(System.getProperty("play.path") + "/framework/lib/" + modRev.getName() + "-" + modRev.getRevision() + ".jar");
            if(jar.exists()) {
               foundBuiltInDependency = true;
               break;
            }
        }

        if(!foundBuiltInDependency) {
            return delegate.resolveConflicts(in, conflicts);
        }

        /*
         * Choose the artifact version provided in $PLAY/framework/lib
         * Evict other versions
         */
        List<IvyNode> result = new ArrayList<>();
        for (IvyNode node : conflicts) {
            ModuleRevisionId modRev = node.getResolvedId();
            File jar = new File(System.getProperty("play.path") + "/framework/lib/" + modRev.getName() + "-" + modRev.getRevision() + ".jar");
            if (jar.exists()) {
                result.add(node);
            }
        }

        return result;
    }
}
