package play.exceptions;

import java.util.List;

public interface SourceAttachment {

    String getSourceFile();
    List<String> getSource();
    Integer getLineNumber();
}
