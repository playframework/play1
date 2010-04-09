package play.data.binding.map.types;

import play.data.Upload;
import play.mvc.Http.Request;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Bind file form multipart/form-data request to an array of Upload object. This is useful when you have a multiple on
 * your input file.
 */
public class UploadArrayBinder implements SupportedType<Upload[]> {

    @SuppressWarnings("unchecked")
    public Upload[] bind(Annotation[] annotations, String value) {
        List<Upload> uploads = (List<Upload>)Request.current().args.get("__UPLOADS");
        List<Upload> uploadArray = new ArrayList<Upload>();

        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
                uploadArray.add(upload);
            }
        }
        return uploadArray.toArray(new Upload[uploadArray.size()]);
    }
}