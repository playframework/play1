package play.data.binding.map.types;

import play.data.Upload;
import play.mvc.Http.Request;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Bind byte[][] form multipart/form-data request, so you can have a byte[][] in your controller. This is useful when you have a multiple on
 * your input file.
 */
public class ByteArrayArrayBinder implements SupportedType<byte[][]> {

    public byte[][] bind(Annotation[] annotations, String value) {
        List<Upload> uploads = (List<Upload>)Request.current().args.get("__UPLOADS");
        List<byte[]> byteList = new ArrayList<byte[]>();
        for(Upload upload : uploads) {
            if(upload.getFieldName().equals(value)) {
                byteList.add(upload.asBytes());
            }
        }
        return byteList.toArray(new byte[byteList.size()][]);
    }
}