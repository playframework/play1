package play.exceptions;

import play.Play;

import java.util.concurrent.atomic.AtomicLong;

import play.Logger;
import org.apache.commons.mail.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;

/**
 * The super class for all Play! exceptions
 */
public abstract class PlayException extends RuntimeException {

    static AtomicLong atomicLong = new AtomicLong(System.currentTimeMillis());
    String id;

    void reportError(String stackTrace){
    	Properties p  = Play.configuration;
    	String errorMonitorigType = p.getProperty("errormonitoring.type","none");
    	//Logger.error("Error Monitoring: "+ errorMonitorigType);
    	if(errorMonitorigType.equalsIgnoreCase("email")){
    		Logger.error("Sending error monitoring email "+getId());
        	try{
    			SimpleEmail emailer = new SimpleEmail();
    			emailer.setHostName(p.getProperty("errormonitoring.smtphost","smtp.gmail.com"));
    			emailer.setAuthentication(p.getProperty("errormonitoring.user",""),p.getProperty("errormonitoring.password",""));
    			emailer.setSSL((p.getProperty("errormonitoring.emailssl","true").equalsIgnoreCase("true")));
    			emailer.setSslSmtpPort(p.getProperty("errormonitoring.port","465"));
    			emailer.setMsg("Error "+getId()+"\n"+getErrorTitle()+"\n"+getErrorDescription()+"\n\n"+stackTrace);
    			emailer.setFrom(p.getProperty("errormonitoring.emailfrom",""));
    			emailer.addTo(p.getProperty("errormonitoring.emailto",""));
    			emailer.setSubject(p.getProperty("application.name","Play!")+" Error " + getId());
    			emailer.send();
        	}
        	catch (EmailException e){
        		Logger.error(e, "Failed to send error monitoring email "+getId());
        	}
    		
    		
    	}
    }

    public PlayException() {
        setId();
    }

    public PlayException(String message) {
        super(message);
        setId();
    }

    public PlayException(String message, Throwable cause) {
        super(message, cause);
        setId();
        if (cause != null){
        	ByteArrayOutputStream o = new ByteArrayOutputStream();
        	PrintStream s = new PrintStream(o);
        	cause.printStackTrace(s);
        	reportError(o.toString());
        }
        else {
        	reportError("No Stack Trace");
        }

    }

    void setId() {
        long nid = atomicLong.incrementAndGet();
        id = Long.toString(nid, 26);
    }

    public abstract String getErrorTitle();

    public abstract String getErrorDescription();

    public boolean isSourceAvailable() {
        return this instanceof SourceAttachment;
    }

    public Integer getLineNumber() {
        return -1;
    }

    public String getSourceFile() {
        return "";
    }

    public String getId() {
        return id;
    }

    /**
     * Get the stack trace element
     * @deprecated since 1.3.0
     * @param cause
     * @return the stack trace element
     */
    @Deprecated
    public static StackTraceElement getInterestingStrackTraceElement(Throwable cause) {
      return getInterestingStackTraceElement(cause);
    }

    public static StackTraceElement getInterestingStackTraceElement(Throwable cause) {
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getLineNumber() > 0 && Play.classes.hasClass(stackTraceElement.getClassName())) {
                return stackTraceElement;
            }
        }
        return null;
    }

    public String getMoreHTML() {
        return null;
    }
}