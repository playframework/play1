package play.data.binding.types;

import play.Logger;
import play.data.Upload;
import play.data.binding.Binder;
import play.data.binding.TypeBinder;
import play.db.Model;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Scope.Params;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

public class UploadBinder implements TypeBinder<Model.BinaryField> {

    @SuppressWarnings("unchecked")
    public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            List<Upload> uploads = (List<Upload>) Request.current().args.get("__UPLOADS");
            for (Upload upload : uploads) {
                if (upload.getFieldName().equals(value) && upload.getSize() > 0) {
                    return upload;
                }
            }
            if (Params.current().get(value + "_delete_") != null) {
                return null;
            }
            return Binder.MISSING;
        } catch (Exception e) {
            Logger.error("", e);
            throw new UnexpectedException(e);
        }
    }
}
