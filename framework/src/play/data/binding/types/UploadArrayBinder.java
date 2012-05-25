package play.data.binding.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import play.data.Upload;
import play.data.binding.TypeBinder;
import play.db.Model;
import play.mvc.Http.Request;

/**
 * Bind file form multipart/form-data request to an array of Upload object. This
 * is useful when you have a multiple on your input file.
 */
public class UploadArrayBinder implements TypeBinder<Model.BinaryField[]> {

    @Override
    @SuppressWarnings("unchecked")
    public Upload[] bind(String name, Annotation[] annotations, String value,
	    Class actualClass, Type genericType) {
	if (value == null || value.trim().length() == 0) {
	    return null;
	}

	Request req = Request.current();
	if (req != null && req.args != null) {
	    List<Upload> uploads = (List<Upload>) req.args.get("__UPLOADS");
	    List<Upload> uploadArray = new ArrayList<Upload>();

	    for (Upload upload : uploads) {
		if (upload.getFieldName().equals(value)) {
		    uploadArray.add(upload);
		}
	    }
	    return uploadArray.toArray(new Upload[uploadArray.size()]);
	}
	return null;
    }
}
