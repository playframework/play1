package play.templates.loadingStrategies;

import play.Play;
import play.templates.BaseTemplate;
import play.templates.GroovyTemplateCompiler;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

public class LoadTemplatesFromSource extends AbstractTemplateLoadingStrategy implements TemplateLoadingStrategy {

    @Override
    public BaseTemplate load(VirtualFile file) {
        BaseTemplate template = null;
        if (file.exists()) {
            return new GroovyTemplateCompiler().compile(file);
        } else {
            return null;
        }
    }

    @Override
    /*
     * Don't bother to include paths that don't exist in the paths list
     */
    public void addTemplatePath(VirtualFile path) {
        if (path.exists()) {
            super.addTemplatePath(path);
        }
    }

    @Override
    public boolean needsReloading(BaseTemplate template, VirtualFile file) {
        return Play.mode == Play.Mode.DEV && template.timestamp < file.lastModified();
    }

    @Override
    public VirtualFile resolveTemplateName(String path) {
        for (VirtualFile vf : getTemplatesPath()) {
            if (vf == null) {
                continue;
            }
            VirtualFile tf = vf.child(path);
            if (tf.exists()) {
                return tf;
            }
        }

        // If all else fails, try and find the template in all of the Play roots (i.e. app root,
        // module roots, plugin roots, etc)
        VirtualFile tf = Play.getVirtualFile(path);
        if (tf != null && tf.exists()) {
            return tf;
        }

        return null;
    }

}
