package play.libs;

import java.util.List;
import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.server.RealmVerifier;
import play.Logger;
import play.exceptions.PlayException;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Scope.Flash;
import play.mvc.Scope.Params;
import play.mvc.results.Redirect;

public class OpenID {

    static ConsumerManager consumerManager;

    public static boolean verify(String openID, String returnAction) {
        try {
            openID = openID.trim();
            Flash.current().put("openid.discover", openID);
            List discoveries = getConsumerManager().discover(openID);
            DiscoveryInformation discovered = getConsumerManager().associate(discoveries);
            String returnTo =  Request.current().getBase() + Router.reverse(returnAction);
            AuthRequest authRequest = getConsumerManager().authenticate(discovered, returnTo);
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

    public static String getVerifiedID() {
        try {
            String openID = Flash.current().get("openid.discover");
            List discoveries = getConsumerManager().discover(openID);
            DiscoveryInformation discovered = getConsumerManager().associate(discoveries);
            ParameterList openidResp = new ParameterList(Params.current().allSimple());
            VerificationResult verification = getConsumerManager().verify(Request.current().getBase() + Request.current().url, openidResp, discovered);
            Identifier verified = verification.getVerifiedId();
            if(verified != null) {
                return verified.toString();
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

    static ConsumerManager getConsumerManager() throws ConsumerException {
        if (consumerManager == null) {
            consumerManager = new ConsumerManager();
            RealmVerifier realmVerifier = new RealmVerifier();
            realmVerifier.setEnforceRpId(false);
            consumerManager.setRealmVerifier(realmVerifier);
        }
        return consumerManager;
    }
}
