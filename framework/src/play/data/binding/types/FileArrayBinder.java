package play.data.binding.types;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import play.data.Upload;
import play.data.binding.TypeBinder;
import play.mvc.Http.Request;

/**
 * Bind a file array form multipart/form-data request. This is nearly the same
 * as the FileBinder, maybe some refactoring can be done. This is useful when
 * you have a multiple on your input file.
 */
public class FileArrayBinder implements TypeBinder<File[]> {

    @Override
    @SuppressWarnings("unchecked")
    public File[] bind(String name, Annotation[] annotations, String value,
	    Class actualClass, Type genericType) {
	if (value == null || value.trim().length() == 0) {
	    return null;
	}

	Request req = Request.current();
	if (req != null && req.args != null) {
	    List<Upload> uploads = (List<Upload>) req.args.get("__UPLOADS");
	    List<File> fileArray = new ArrayList<File>();
	    for (Upload upload : uploads) {
		if (upload.getFieldName().equals(value)) {
		    File file = upload.asFile();
		    if (file.length() > 0) {
			fileArray.add(file);
		    }
		}
	    }
	    return fileArray.toArray(new File[fileArray.size()]);
	}
	return null;
    }
}
