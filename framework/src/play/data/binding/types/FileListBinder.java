package play.data.binding.types;

import play.data.Upload;
import play.data.binding.TypeBinder;
import play.mvc.Http.Request;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Bind file form multipart/form-data request.
 */
public class FileListBinder implements TypeBinder<List<File>> {

    @SuppressWarnings("unchecked")
    public List<File> bind(String name, Annotation[] annotations, String value, Class actualClass) {
        List<Upload> uploads = (List<Upload>) Request.current().args.get("__UPLOADS");
        List<File> fileArray = new ArrayList<File>();
        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
                File file = upload.asFile();
                if(file.length() > 0) {
                    fileArray.add(file);
                }
            }
        }
        return fileArray;
    }
}