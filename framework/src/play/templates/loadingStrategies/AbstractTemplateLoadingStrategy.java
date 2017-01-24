package play.templates.loadingStrategies;

import java.util.ArrayList;
import java.util.List;

import play.vfs.VirtualFile;

public abstract class AbstractTemplateLoadingStrategy {

    private List<VirtualFile> templatesPath = new ArrayList<VirtualFile>();

    public void addTemplatePath(VirtualFile path) {
        templatesPath.add(path);
    }

    public List<VirtualFile> getTemplatesPath() {
        return new ArrayList<VirtualFile>(templatesPath);
    }


}