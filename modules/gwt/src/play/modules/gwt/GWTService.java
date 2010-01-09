package play.modules.gwt;

import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import com.google.gwt.user.server.rpc.SerializationPolicyProvider;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Router.Route;
import play.mvc.results.RenderStatic;

public class GWTService implements SerializationPolicyProvider {

    public final SerializationPolicy getSerializationPolicy(String moduleBaseURL, String strongName) {
        return doGetSerializationPolicy(moduleBaseURL, strongName);
    }

    protected SerializationPolicy doGetSerializationPolicy(String moduleBaseURL, String strongName) {

        // Find the module path
        String modulePath = null;
        if (moduleBaseURL != null) {
            try {
                modulePath = new URL(moduleBaseURL).getPath();
            } catch (MalformedURLException ex) {
                throw new UnexpectedException("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            }
        }

        SerializationPolicy serializationPolicy = null;

        String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(modulePath + strongName);

        // Ok. Will need to route this path as it's an URL
        for (Route route : Router.routes) {
            try {
                route.matches("GET", serializationPolicyFilePath);
            } catch(Throwable t) {                                          
                if(t instanceof RenderStatic) {
                    serializationPolicyFilePath = ((RenderStatic)t).file;
                    break;
                }
            }
        }

        // Open the RPC resource file read its contents.
        InputStream is = Play.getVirtualFile(serializationPolicyFilePath).inputstream();
        try {
            if (is != null) {
                try {
                    serializationPolicy = SerializationPolicyLoader.loadFromStream(is, null);
                } catch (Exception e) {
                    throw new UnexpectedException("ERROR: Failed to parse the policy file '" + serializationPolicyFilePath + "'", e);
                } 
            } else {
                throw new UnexpectedException("ERROR: The serialization policy file '" + serializationPolicyFilePath + "' was not found; did you forget to include it in this deployment?");
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return serializationPolicy;
    }

    public String invoke() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Play.classloader);
            String payload = IO.readContentAsString(Request.current().body);
            RPCRequest rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
            return RPC.invokeAndEncodeResponse(this, rpcRequest.getMethod(), rpcRequest.getParameters(), rpcRequest.getSerializationPolicy());
        } catch (Exception ex) {
            Logger.error(ex, "An IncompatibleRemoteServiceException was thrown while processing this call.");
            try {
                return RPC.encodeResponseForFailure(null, ex);
            } catch (Exception exx) {
                exx.printStackTrace();
                return null;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
