package play.modules.ecss;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.libs.Utils;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.vfs.VirtualFile;

public class ECssPlugin extends PlayPlugin {

    static Pattern pattern = Pattern.compile("[$]([-A-Za-z1-9_]+)\\s*:\\s*(.*?);", Pattern.MULTILINE);

    @Override
    public boolean serveStatic(VirtualFile file, Request request, Response response) {
        if (file.getName().endsWith(".css")) {
            if (Play.mode == Play.Mode.DEV) {
                response.setHeader("Cache-Control", "no-cache");
                doIt(file, request, response);
            } else {
                long last = file.lastModified();
                String etag = last + "-" + file.hashCode();
                if (!isModified(etag, last, request)) {
                    response.setHeader("Etag", etag);
                    response.status = 304;
                } else {
                    response.setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(last)));
                    response.setHeader("Cache-Control", "max-age=3600");
                    response.setHeader("Etag", etag);
                    doIt(file, request, response);
                }
            }
            return true;
        }
        return false;

    }

    void doIt(VirtualFile file, Request request, Response response) {
        try {
            String css = file.contentAsString();
            Matcher matcher = pattern.matcher(css);
            Map<String, String> vars = new HashMap<String, String>();
            while (matcher.find()) {
                String var = matcher.group(1);
                String value = matcher.group(2);
                vars.put(var, value);
            }
            css = css.replaceAll("[$]([-A-Za-z1-9_]+)\\s*:\\s*(.*?);", "");
            for (String var : vars.keySet()) {
                css = css.replace("$" + var, vars.get(var));
            }
            response.contentType = "text/css";
            response.out.write(css.getBytes("utf-8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isModified(String etag, long last, Request request) {
        if (!(request.headers.containsKey("If-None-Match") && request.headers.containsKey("If-Modified-Since"))) {
            return true;
        } else {
            String browserEtag = request.headers.get("If-None-Match").value();
            if (!browserEtag.equals(etag)) {
                return true;
            } else {
                try {
                    Date browserDate = Utils.getHttpDateFormatter().parse(request.headers.get("If-Modified-Since").value());
                    if (browserDate.getTime() >= last) {
                        return false;
                    }
                } catch (ParseException ex) {
                    Logger.error("Can't parse date", ex);
                }
                return true;
            }
        }
    }
}
