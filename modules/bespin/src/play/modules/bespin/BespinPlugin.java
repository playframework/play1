package play.modules.bespin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import play.Play;
import play.PlayPlugin;
import play.libs.IO;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class BespinPlugin extends PlayPlugin {

    @Override
    public boolean rawInvocation(Request request, Response response) {
        try {
            // -- /bespin
            if(request.path.equals("/bespin")) {
                response.status = 302;
                response.setHeader("Location", "/public/bespin/dashboard.html");
                return true;
            } 
            // -- /bespin/settings
            if(request.path.equals("/bespin/settings/")) {
		response.status = 200;
                response.out.write("{\"_username\": \"guillaume.bort\", \"collaborate\": \"off\", \"syntax\": \"auto\", \"autocomplete\": \"off\", \"fontsize\": \"8\", \"tabsize\": \"4\", \"keybindings\": \"emacs\"".getBytes("utf-8"));
                return true;
            }
            // -- /bespin/register/userinfo
            if(request.path.equals("/bespin/register/userinfo/")) {
		response.status = 200;
                response.out.write("{\"username\": \"Guillaume\"}".getBytes("utf-8"));
                return true;
            }            
            // -- /bespin/file/list
            if(request.path.startsWith("/bespin/file/list/")) {
                String root = request.path.substring("/bespin/file/list".length());
                List<File> fileList = Arrays.asList(Play.getFile(root).listFiles());
		response.status = 200;
                response.out.write(list(fileList).getBytes("utf-8"));
                return true;
            }
            // -- /bespin/register/listopen
            if(request.path.equals("/bespin/file/listopen/")) {
		response.status = 200;
                response.out.write("{}".getBytes("utf-8"));
                return true;
            }  
            // -- /bespin/register/listopen
            if(request.path.startsWith("/bespin/file/at/")) {
                String file = request.path.substring("/bespin/file/al".length());
                if(request.method.equals("GET")) {
                    response.status = 200;
                    response.out.write(IO.readContentAsString(Play.getFile(file)).replace("\t", "    ").getBytes("utf-8"));
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int r = -1;
                    while((r = request.body.read()) != -1) {
                        baos.write(r);
                    }
                    IO.writeContent(new String(baos.toByteArray(), "utf-8"), Play.getFile(file));
                }
                return true;
            } 
            return false;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    static String list(List<File> list) {
        StringBuffer buf = new StringBuffer("[");
        for(File file : list) {
            if(file.isDirectory()) {
                buf.append("{\"name\": \"" + file.getName()+"/\"},");
            } else {
                buf.append("{\"name\": \"" + file.getName()+"\", \"size\": " + file.length() + "},");
            }
        }
        buf.append("]");
        return buf.toString();
    }
    
}
