package play.data.binding.types;

import play.data.binding.TypeBinder;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import play.data.Upload;
import play.mvc.Http.Request;

/**
 * Bind file form multipart/form-data request.
 */
public class FileBinder implements TypeBinder<File> {

    @SuppressWarnings("unchecked")
    public File bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        List<Upload> uploads = (List<Upload>) Request.current().args.get("__UPLOADS");
        for (Upload upload : uploads) {
            if (upload.getFieldName().equals(value)) {
                File file = upload.asFile();
                if (file.length() > 0) {
                    return file;
                }
                return null;
            }
        }
        return null;
    }
}
