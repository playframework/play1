package play.templates.loadingStrategies;

import java.util.List;

import play.templates.BaseTemplate;
import play.templates.Template;
import play.vfs.VirtualFile;

public interface TemplateLoadingStrategy {
    VirtualFile resolveTemplateName(String name);

    BaseTemplate load(VirtualFile file);

    boolean needsReloading(BaseTemplate template, VirtualFile file);
    
    void addTemplatePath(VirtualFile path);
    
    List<VirtualFile> getTemplatesPath();

}
