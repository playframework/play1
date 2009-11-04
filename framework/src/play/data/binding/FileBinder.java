package play.data.binding;

import java.io.File;
import java.util.List;
import play.data.Upload;
import play.mvc.Http.Request;

/**
 * Bind file form multipart/form-data request.
 */
public class FileBinder implements SupportedType<File> {

	@SuppressWarnings("unchecked")
    public File bind(String value) {
        List<Upload> uploads = (List<Upload>)Request.current().args.get("__UPLOADS");
        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
                File file = upload.asFile();
                if(file.length() > 0) {
                    return file;
                } 
                return null;
            }
        }
        return null;
    }
}
