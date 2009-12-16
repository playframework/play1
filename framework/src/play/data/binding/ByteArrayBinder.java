package play.data.binding;

import java.lang.annotation.Annotation;
import java.util.List;
import play.Logger;
import play.data.Upload;
import play.mvc.Http.Request;

/**
 * Bind byte[] form multipart/form-data request.
 */
public class ByteArrayBinder implements SupportedType<byte[]> {

    public byte[] bind(Annotation[] annotations, String value) {
        List<Upload> uploads = (List<Upload>)Request.current().args.get("__UPLOADS");
        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
                return upload.asBytes();
            }
        }
        return null;
    }
}
