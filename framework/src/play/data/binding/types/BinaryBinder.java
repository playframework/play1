package play.data.binding.types;

import java.lang.annotation.Annotation;
import java.util.List;
import play.data.Upload;
import play.data.binding.Binder;
import play.db.Model;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Scope.Params;

public class BinaryBinder implements SupportedType<Model.BinaryField> {

    @SuppressWarnings("unchecked")
    public Object bind(Annotation[] annotations, String value, Class actualClass) {
        try{
            Model.BinaryField b = (Model.BinaryField)actualClass.newInstance();
            List<Upload> uploads = (List<Upload>)Request.current().args.get("__UPLOADS");
            for(Upload upload : uploads) {
                if(upload.getFieldName().equals(value) && upload.getSize() > 0) {
                    b.set(upload.asStream(), upload.getContentType());
                    return b;
                }
            }
            if(Params.current().get(value+"_delete_") != null) {
                return null;
            }
            return Binder.MISSING;
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
