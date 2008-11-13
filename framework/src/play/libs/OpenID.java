package play.libs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryNonceVerifier;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.server.RealmVerifier;

import play.Logger;
import play.Play;
import play.exceptions.PlayException;
import play.mvc.Router;
import play.mvc.Http.Request;
import play.mvc.Scope.Flash;
import play.mvc.Scope.Params;
import play.mvc.results.Redirect;

public class OpenID {

	static Map<String, String> attributesURI = new HashMap<String, String> ();
	static Map<String, Boolean> attributesMandatory = new HashMap<String, Boolean> ();
	
	static {
		for (Object oKey : Play.configuration.keySet()) {
			String key = (String) oKey;
			if (key.startsWith("openid.attribute")) {
				String alias = key.substring(("openid.attribute".length()));
				attributesURI.put(alias,Play.configuration.getProperty(key));
				attributesMandatory.put(alias,false);
			}
			if (key.startsWith("openid.mandatory.attribute")) {
				String alias = key.substring(("openid.mandatory.attribute".length()));
				attributesURI.put(alias,Play.configuration.getProperty(key));
				attributesMandatory.put(alias,true);
			}
		}
		// If nothing in config, 3 optional attributes.
		if (attributesURI.size()==0) {
			attributesURI.put("FirstName", "http://schema.openid.net/namePerson/first");
			attributesURI.put("LastName", "http://schema.openid.net/namePerson/last");
			attributesURI.put("Email", "http://schema.openid.net/namePerson/email");
			attributesMandatory.put("FirstName", false);
			attributesMandatory.put("LastName", false);
			attributesMandatory.put("Email", false);
		}
	}
	
    static ConsumerManager consumerManager;

    public static boolean verify(String openID, String returnAction) {
        try {
            openID = openID.trim();
            Flash.current().put("openid.discover", openID);
            List discoveries = getConsumerManager().discover(openID);
            DiscoveryInformation discovered = getConsumerManager().associate(discoveries);
            String returnTo =  Request.current().getBase() + Router.reverse(returnAction);
            AuthRequest authRequest = getConsumerManager().authenticate(discovered, returnTo);
            FetchRequest fetch = FetchRequest.createFetchRequest();
            for (String aliasKey : attributesURI.keySet()) {
            	fetch.addAttribute(aliasKey, attributesURI.get(aliasKey), attributesMandatory.get(aliasKey));
			}
            authRequest.addExtension(fetch);
            String url = authRequest.getDestinationUrl(true);
            throw new Redirect(url);
        } catch(PlayException e) {
            throw e;
        } catch(ConsumerException e) {
            Logger.error(e, "OpenID cannot verify %s", openID);
        } catch(MessageException e) {
            Logger.error(e, "OpenID cannot verify %s", openID);
        } catch(DiscoveryException e) {
            Logger.error(e, "OpenID cannot verify %s", openID);
        }
        return false;
    } 

    public static boolean verify(String openID) {
        return verify(openID, Request.current().action);
    }

    public static UserInfo getVerifiedID() {
        try {
            String openID = Flash.current().get("openid.discover");
            List discoveries = getConsumerManager().discover(openID);
            DiscoveryInformation discovered = getConsumerManager().associate(discoveries);
            ParameterList openidResp = new ParameterList(Params.current().allSimple());
            VerificationResult verification = getConsumerManager().verify(Request.current().getBase() + Request.current().url, openidResp, discovered);
            Identifier verified = verification.getVerifiedId();
            if(verified != null && !verified.equals("null")) {
            	UserInfo userInfo = new UserInfo ();
            	userInfo.id=verified.toString();
            	AuthSuccess authSuccess = (AuthSuccess) verification.getAuthResponse();
            	if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
            		FetchResponse fetchResp = (FetchResponse) authSuccess.getExtension(AxMessage.OPENID_NS_AX);
            		Map<String,List<String>> attributes = (Map<String,List<String>>)fetchResp.getAttributes();
            		for (String key : attributes.keySet()) {
						userInfo.extensions.put(key,attributes.get(key).get(0));
					}
            	}
                return userInfo;
            }            
        } catch(PlayException e) {
            throw e;
        } catch(ConsumerException e) {
            Logger.error(e, "OpenID error");
        } catch(MessageException e) {
            Logger.error(e, "OpenID error");
        } catch(DiscoveryException e) {
            Logger.error(e, "OpenID error");
        } catch(AssociationException e) {
            Logger.error(e, "OpenID error");
        }
        return null;
    }

    public static class UserInfo {
    	public String id;
    	public Map<String, String> extensions = new HashMap<String, String>();
    }
    
    static ConsumerManager getConsumerManager() throws ConsumerException {
        if (consumerManager == null) {
            consumerManager = new ConsumerManager();
            RealmVerifier realmVerifier = new RealmVerifier();
            realmVerifier.setEnforceRpId(false);
            consumerManager.setRealmVerifier(realmVerifier);
            consumerManager.setNonceVerifier(new InMemoryNonceVerifier(1800)); // 1/2 h
        }
        return consumerManager;
    }
}
