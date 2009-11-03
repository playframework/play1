package play.exceptions;

import java.util.Arrays;
import java.util.List;
import org.yaml.snakeyaml.scanner.ScannerException;
import play.vfs.VirtualFile;

public class YAMLException extends PlayException implements SourceAttachment {

    ScannerException e;
    VirtualFile yaml;

    public YAMLException(ScannerException e, VirtualFile yaml) {
        super(e.getMessage() + " (in file " + yaml.relativePath() + " line " + (e.getProblemMark().getLine() + 1) + ", column " + (e.getProblemMark().getColumn() + 1) + ")", e);
        this.e = e;
        this.yaml = yaml;
    }

    @Override
    public String getErrorTitle() {
        return "Malformed YAML";
    }

    @Override
    public String getErrorDescription() {
        if (yaml == null) {
            return "Cannot parse the yaml file: " + e.getProblem();
        }
        return "Cannot parse the <strong>" + yaml.relativePath() + "</strong> file: " + e.getProblem();
    }

    public Integer getLineNumber() {
        return e.getProblemMark().getLine() + 1;
    }

    public List<String> getSource() {
        return Arrays.asList(yaml.contentAsString().split("\n"));
    }

    public String getSourceFile() {
        return yaml.relativePath();
    }

    @Override
    public boolean isSourceAvailable() {
        return yaml != null && e.getProblemMark() != null;
    }
}
