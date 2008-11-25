package play.libs;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;
import org.openid4java.association.Association;
import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerAssociationStore;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.NonceVerifier;
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

import org.openid4java.util.InternetDateFormat;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.exceptions.PlayException;
import play.mvc.Router;
import play.mvc.Http.Request;
import play.mvc.Scope.Flash;
import play.mvc.Scope.Params;
import play.mvc.results.Redirect;

public class OpenID {

    static Map<String, String> attributesURI = new HashMap<String, String>();
    static Map<String, Boolean> attributesMandatory = new HashMap<String, Boolean>();
    

    static {
        for (Object oKey : Play.configuration.keySet()) {
            String key = (String) oKey;
            if (key.startsWith("openid.attribute")) {
                String alias = key.substring(("openid.attribute".length()));
                attributesURI.put(alias, Play.configuration.getProperty(key));
                attributesMandatory.put(alias, false);
            }
            if (key.startsWith("openid.mandatory.attribute")) {
                String alias = key.substring(("openid.mandatory.attribute".length()));
                attributesURI.put(alias, Play.configuration.getProperty(key));
                attributesMandatory.put(alias, true);
            }
        }
        // If nothing in config, 3 optional attributes.
        if (attributesURI.size() == 0) {
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
            String returnTo = Request.current().getBase() + Router.reverse(returnAction);
            AuthRequest authRequest = getConsumerManager().authenticate(discovered, returnTo);
            FetchRequest fetch = FetchRequest.createFetchRequest();
            for (String aliasKey : attributesURI.keySet()) {
                fetch.addAttribute(aliasKey, attributesURI.get(aliasKey), attributesMandatory.get(aliasKey));
            }
            authRequest.addExtension(fetch);
            String url = authRequest.getDestinationUrl(true);
            throw new Redirect(url);
        } catch (PlayException e) {
            throw e;
        } catch (ConsumerException e) {
            Logger.error(e, "OpenID cannot verify %s", openID);
        } catch (MessageException e) {
            Logger.error(e, "OpenID cannot verify %s", openID);
        } catch (DiscoveryException e) {
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
            if (verified != null && !verified.equals("null")) {
                UserInfo userInfo = new UserInfo();
                userInfo.id = verified.toString();
                AuthSuccess authSuccess = (AuthSuccess) verification.getAuthResponse();
                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
                    FetchResponse fetchResp = (FetchResponse) authSuccess.getExtension(AxMessage.OPENID_NS_AX);
                    Map<String, List<String>> attributes = (Map<String, List<String>>) fetchResp.getAttributes();
                    for (String key : attributes.keySet()) {
                        userInfo.extensions.put(key, attributes.get(key).get(0));
                    }
                }
                return userInfo;
            }
        } catch (PlayException e) {
            throw e;
        } catch (ConsumerException e) {
            Logger.error(e, "OpenID error");
        } catch (MessageException e) {
            Logger.error(e, "OpenID error");
        } catch (DiscoveryException e) {
            Logger.error(e, "OpenID error");
        } catch (AssociationException e) {
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
            consumerManager.setNonceVerifier(new CacheNonceVerifier(1800)); // 1/2h
            consumerManager.setAssociations(new CacheAssociationStore());
        }
        return consumerManager;
    }

    public static class CacheAssociationStore implements ConsumerAssociationStore {

        @SuppressWarnings("unchecked")
        public void save(String opUrl, Association association) {
            int maxAge = (int) ((association.getExpiry().getTime() - new Date().getTime()) / 1000);
            if (maxAge <= 0) {
                maxAge = 10;
            }
            Cache.add(opUrl + "#" + association.getHandle(), association, maxAge + "s");
            Set<String> handles = (Set<String>) Cache.get(opUrl + "#_handles");
            if (handles == null) {
                handles = new HashSet<String>();
                Cache.add(opUrl + "#_handles", handles);
            }
            handles.add(association.getHandle());
            Cache.replace(opUrl + "#_handles", handles);
        }

        @SuppressWarnings("unchecked")
        public Association load(String opUrl, String handle) {
            Association assoc = (Association) Cache.get(opUrl + "#" + handle);
            if (assoc == null) {
                Set<String> handles = (Set<String>) Cache.get(opUrl + "#_handles");
                if (handles != null) {
                    handles.remove(handle);
                    Cache.replace(opUrl + "#_handles", handles);
                }
            }
            return assoc;
        }

        @SuppressWarnings("unchecked")
        public Association load(String opUrl) {
            Association latest = null;
            Set<String> handles = (Set<String>) Cache.get(opUrl + "#_handles");
            Set<String> toRemove = new HashSet<String>();
            if (handles != null) {
                for (String handle : handles) {
                    Association association = (Association) Cache.get(opUrl + "#" + handle);
                    if (association != null) {
                        if (latest == null || latest.getExpiry().before(association.getExpiry())) {
                            latest = association;
                        }
                    } else {
                        toRemove.add(handle);
                    }
                }
                if (!toRemove.isEmpty()) {
                    handles.removeAll(toRemove);
                    Cache.replace(opUrl + "#_handles", handles);
                }
            }
            return (latest);
        }

        @SuppressWarnings("unchecked")
        public void remove(String opUrl, String handle) {
            Cache.delete(opUrl + "#" + handle);
            Set<String> handles = (Set<String>) Cache.get(opUrl + "#_handles");
            if (handles != null) {
                if (handles.contains(handle)) {
                    handles.remove(handle);
                    Cache.replace(opUrl + "#_handles", handles);

                }
            }
        }
    }

    public static class CacheNonceVerifier implements NonceVerifier {

        protected static InternetDateFormat _dateFormat = new InternetDateFormat();
        protected int _maxAge;

        /**
         * @param maxAge
         *          maximum token age in seconds
         */
        protected CacheNonceVerifier(int maxAge) {
            _maxAge = maxAge;
        }

        public int getMaxAge() {
            return _maxAge;
        }

        /**
         * Checks if nonce date is valid and if it is in the max age boudary. Other
         * checks are delegated to {@link #seen(java.util.Date, String, String)}
         */
        public int seen(String opUrl, String nonce) {
            Date now = new Date();
            try {
                Date nonceDate = _dateFormat.parse(nonce);
                if (isTooOld(now, nonceDate)) {
                    return (TOO_OLD);
                }
                return (seen(now, opUrl, nonce));
            } catch (ParseException e) {
                return (INVALID_TIMESTAMP);
            }
        }

        /**
         * Subclasses should implement this method and check if the nonce was seen
         * before. The nonce timestamp was verified at this point, it is valid and
         * it is in the max age boudary.
         *
         * @param now
         *          The timestamp used to check the max age boudary.
         */
        protected int seen(Date now, String opUrl, String nonce) {
            String key = opUrl + '#' + nonce;
            if (Cache.get(key) != null) {
                return (SEEN);
            }
            Cache.add(key, "true", _maxAge + "s");
            return (OK);
        }

        protected boolean isTooOld(Date now, Date nonce) {
            long age = now.getTime() - nonce.getTime();
            return (age > _maxAge * 1000);
        }
    }
}
