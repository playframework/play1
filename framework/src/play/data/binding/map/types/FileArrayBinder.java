package play.data.binding.map.types;

import play.data.Upload;
import play.mvc.Http.Request;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Bind a file array form multipart/form-data request. This is nearly the same as the FileBinder, maybe some refactoring can be
 * done. This is useful when you have a multiple on your input file.
 */
public class FileArrayBinder implements SupportedType<File[]> {

    @SuppressWarnings("unchecked")
    public File[] bind(Annotation[] annotations, String value) {
        List<Upload> uploads = (List<Upload>)Request.current().args.get("__UPLOADS");
        List<File> fileArray = new ArrayList<File>();
        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
                File file = upload.asFile();
                if(file.length() > 0) {
                    fileArray.add(file);
                }
            }
        }
        return fileArray.toArray(new File[fileArray.size()]);
    }
}