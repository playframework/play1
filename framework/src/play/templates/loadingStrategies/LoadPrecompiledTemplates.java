package play.templates.loadingStrategies;

import java.util.ArrayList;
import java.util.List;

import play.Logger;
import play.Play;
import play.templates.BaseTemplate;
import play.templates.GroovyTemplate;
import play.vfs.VirtualFile;

public class LoadPrecompiledTemplates extends AbstractTemplateLoadingStrategy implements TemplateLoadingStrategy {

    @Override
    public BaseTemplate load(VirtualFile file) {
        String source = "source unavailble";
        if (file.exists()) {
            source = file.contentAsString();
        }

        BaseTemplate template = new GroovyTemplate(precompiledName(file), source);
        try {
            template.loadPrecompiled();
            return template;
        } catch (Exception e) {
            Logger.warn("Precompiled template %s not found", file.relativePath());
            return null;
        }

    }

    private static String precompiledName(VirtualFile file) {
        return file.relativePath().replaceAll("\\{(.*)\\}", "from_$1").replace(":", "_").replace("..", "parent");
    }

    @Override
    public boolean needsReloading(BaseTemplate template, VirtualFile file) {
        return false;
    }

    @Override
    public VirtualFile resolveTemplateName(String path) {
        for (VirtualFile vf : getTemplatesPath()) {
            VirtualFile tf = vf.child(path);
            if (tf.exists()) {
                return tf;
            } else {
                String pcfName = precompiledName(tf);
                VirtualFile pcf = VirtualFile.open(Play.applicationPath).child("precompiled/templates" + pcfName);

                return tf;
            }
        }
        return null;
    }
}
