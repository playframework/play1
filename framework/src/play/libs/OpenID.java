package play.libs;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import play.Logger;
import play.exceptions.PlayException;
import play.libs.WS.HttpResponse;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Scope.Params;
import play.mvc.results.Redirect;

public class OpenID {

    private OpenID(String id) {
        this.id = id;
        this.returnAction = this.realmAction = Request.current().action;
    }

    // ~~~ API
    String id;
    String returnAction;
    String realmAction;
    List<String> sregRequired = new ArrayList<>();
    List<String> sregOptional = new ArrayList<>();
    Map<String, String> axRequired = new HashMap<>();
    Map<String, String> axOptional = new HashMap<>();

    public OpenID returnTo(String action) {
        this.returnAction = action;
        return this;
    }

    public OpenID forRealm(String action) {
        this.realmAction = action;
        return this;
    }

    public OpenID required(String alias) {
        this.sregRequired.add(alias);
        return this;
    }

    public OpenID required(String alias, String schema) {
        this.axRequired.put(alias, schema);
        return this;
    }

    public OpenID optional(String alias) {
        this.sregOptional.add(alias);
        return this;
    }

    public OpenID optional(String alias, String schema) {
        this.axOptional.put(alias, schema);
        return this;
    }

    public boolean verify() {
        try {
            // Normalize
            String claimedId = normalize(id);
            String server = null;
            String delegate = null;

            // Discover
            HttpResponse response = WS.url(claimedId).get();

            // Try HTML (I know it's bad)
            String html = response.getString();
            server = discoverServer(html);

            if (server == null) {

                // Try YADIS
                Document xrds = null;

                if (response.getContentType().contains("application/xrds+xml")) {
                    xrds = getXml(html, response.getEncoding());
                } else if (response.getHeader("X-XRDS-Location") != null) {
                    xrds = WS.url(response.getHeader("X-XRDS-Location")).get().getXml();
                } else {
                    return false;
                }

                // Ok we have the XRDS file
                server = XPath.selectText("//Type[text()='http://specs.openid.net/auth/2.0/server']/following-sibling::URI/text()", xrds);
                claimedId = XPath.selectText("//Type[text()='http://specs.openid.net/auth/2.0/signon']/following-sibling::LocalID/text()",
                        xrds);
                if (claimedId == null) {
                    claimedId = "http://specs.openid.net/auth/2.0/identifier_select";
                } else {
                    server = XPath.selectText("//Type[text()='http://specs.openid.net/auth/2.0/signon']/following-sibling::URI/text()",
                            xrds);
                }

                if (server == null) {
                    return false;
                }

            } else {

                // Delegate
                Matcher openid2Localid = Pattern.compile("<link[^>]+openid2[.]local_id[^>]+>", Pattern.CASE_INSENSITIVE).matcher(html);
                Matcher openidDelegate = Pattern.compile("<link[^>]+openid[.]delegate[^>]+>", Pattern.CASE_INSENSITIVE).matcher(html);
                if (openid2Localid.find()) {
                    delegate = extractHref(openid2Localid.group());
                } else if (openidDelegate.find()) {
                    delegate = extractHref(openidDelegate.group());
                }

            }

            // Redirect
            String url = server;
            if (!server.contains("?")) {
                url += "?";
            }
            if (!url.endsWith("?") && !url.endsWith("&")) {
                url += "&";
            }

            url += "openid.ns=" + URLEncoder.encode("http://specs.openid.net/auth/2.0", "UTF-8");
            url += "&openid.mode=checkid_setup";
            url += "&openid.claimed_id=" + URLEncoder.encode(claimedId, "utf8");
            url += "&openid.identity=" + URLEncoder.encode(delegate == null ? claimedId : delegate, "utf8");

            if (returnAction != null && (returnAction.startsWith("http://") || returnAction.startsWith("https://"))) {
                url += "&openid.return_to=" + URLEncoder.encode(returnAction, "utf8");
            } else {
                url += "&openid.return_to=" + URLEncoder.encode(Request.current().getBase() + Router.reverse(returnAction), "utf8");
            }
            if (realmAction != null && (realmAction.startsWith("http://") || realmAction.startsWith("https://"))) {
                url += "&openid.realm=" + URLEncoder.encode(realmAction, "utf8");
            } else {
                url += "&openid.realm=" + URLEncoder.encode(Request.current().getBase() + Router.reverse(realmAction), "utf8");
            }

            if (!sregOptional.isEmpty() || !sregRequired.isEmpty()) {
                url += "&openid.ns.sreg=" + URLEncoder.encode("http://openid.net/extensions/sreg/1.1", "UTF-8");
            }
            String sregO = "";
            for (String a : sregOptional) {
                sregO += URLEncoder.encode(a, "UTF-8") + ",";
            }
            if (!StringUtils.isEmpty(sregO)) {
                url += "&openid.sreg.optional=" + sregO.substring(0, sregO.length() - 1);
            }
            String sregR = "";
            for (String a : sregRequired) {
                sregR += URLEncoder.encode(a, "UTF-8") + ",";
            }
            if (!StringUtils.isEmpty(sregR)) {
                url += "&openid.sreg.required=" + sregR.substring(0, sregR.length() - 1);
            }

            if (!axRequired.isEmpty() || !axOptional.isEmpty()) {
                url += "&openid.ns.ax=http%3A%2F%2Fopenid.net%2Fsrv%2Fax%2F1.0";
                url += "&openid.ax.mode=fetch_request";
                for (String a : axOptional.keySet()) {
                    url += "&openid.ax.type." + a + "=" + URLEncoder.encode(axOptional.get(a), "UTF-8");
                }
                for (String a : axRequired.keySet()) {
                    url += "&openid.ax.type." + a + "=" + URLEncoder.encode(axRequired.get(a), "UTF-8");
                }
                if (!axRequired.isEmpty()) {
                    String r = "";
                    for (String a : axRequired.keySet()) {
                        r += "," + a;
                    }
                    r = r.substring(1);
                    url += "&openid.ax.required=" + r;
                }
                if (!axOptional.isEmpty()) {
                    String r = "";
                    for (String a : axOptional.keySet()) {
                        r += "," + a;
                    }
                    r = r.substring(1);
                    url += "&openid.ax.if_available=" + r;
                }
            }

            if (Logger.isTraceEnabled()) {
                // Debug
                Logger.trace("Send request %s", url);
            }

            throw new Redirect(url);
        } catch (Redirect | PlayException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    // ~~~~ Main API
    public static OpenID id(String id) {
        return new OpenID(id);
    }

    /**
     * Normalize the given openid as a standard openid
     * 
     * @param openID
     *            the given openid
     * @return The normalize openID
     */
    public static String normalize(String openID) {
        openID = openID.trim();
        if (!openID.startsWith("http://") && !openID.startsWith("https://")) {
            openID = "http://" + openID;
        }
        try {
            URI url = new URI(openID);
            String frag = url.getRawFragment();
            if (frag != null && frag.length() > 0) {
                openID = openID.replace("#" + frag, "");
            }
            if (url.getPath().equals("")) {
                openID += "/";
            }
            openID = new URI(openID).toString();
        } catch (Exception e) {
            throw new RuntimeException(openID + " is not a valid URL", e);
        }
        return openID;
    }

    /**
     * Is the current request an authentication response from the OP ?
     * 
     * @return true if the current request an authentication response
     */
    public static boolean isAuthenticationResponse() {
        return Params.current().get("openid.mode") != null;
    }

    /**
     * Retrieve the verified OpenID
     * 
     * @return A UserInfo object
     */
    public static UserInfo getVerifiedID() {
        try {
            String mode = Params.current().get("openid.mode");

            // Check authentication
            if (mode != null && mode.equals("id_res")) {

                // id
                String id = Params.current().get("openid.claimed_id");
                if (id == null) {
                    id = Params.current().get("openid.identity");
                }

                id = normalize(id);

                // server
                String server = Params.current().get("openid.op_endpoint");
                if (server == null) {
                    server = discoverServer(id);
                }

                String fields = Request.current().querystring.replace("openid.mode=id_res", "openid.mode=check_authentication");
                WS.HttpResponse response = WS.url(server).mimeType("application/x-www-form-urlencoded").body(fields).post();
                if (response.getStatus() == 200 && response.getString().contains("is_valid:true")) {
                    UserInfo userInfo = new UserInfo();
                    userInfo.id = id;
                    Pattern patternAX = Pattern.compile("^openid[.].+[.]value[.]([^.]+)([.]\\d+)?$");
                    Pattern patternSREG = Pattern.compile("^openid[.]sreg[.]([^.]+)$");
                    for (String p : Params.current().allSimple().keySet()) {
                        Matcher m = patternAX.matcher(p);
                        if (m.matches()) {
                            String alias = m.group(1);
                            userInfo.extensions.put(alias, Params.current().get(p));
                        }
                        m = patternSREG.matcher(p);
                        if (m.matches()) {
                            String alias = m.group(1);
                            userInfo.extensions.put(alias, Params.current().get(p));
                        }
                    }
                    return userInfo;
                } else {
                    return null;
                }

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    // ~~~~ Utils
    static String extractHref(String link) {
        Matcher m = Pattern.compile("href=\"([^\"]*)\"").matcher(link);
        if (m.find()) {
            return m.group(1).trim();
        }
        m = Pattern.compile("href=\'([^\']*)\'").matcher(link);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public static String discoverServer(String openid) {
        if (openid.startsWith("http")) {
            openid = WS.url(openid).get().getString();
        }
        Matcher openid2Provider = Pattern.compile("<link[^>]+openid2[.]provider[^>]+>", Pattern.CASE_INSENSITIVE).matcher(openid);
        Matcher openidServer = Pattern.compile("<link[^>]+openid[.]server[^>]+>", Pattern.CASE_INSENSITIVE).matcher(openid);
        String server = null;
        if (openid2Provider.find()) {
            server = extractHref(openid2Provider.group());
        } else if (openidServer.find()) {
            server = extractHref(openidServer.group());
        }
        return server;
    }

    private static Document getXml(String response, String encoding) {
        try {
            InputSource source = new InputSource(new StringReader(response));
            source.setEncoding(encoding);
            DocumentBuilder builder = XML.newDocumentBuilder();
            return builder.parse(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ~~~~ Result class
    public static class UserInfo {

        /**
         * OpenID
         */
        public String id;
        /**
         * Extensions values
         */
        public Map<String, String> extensions = new HashMap<>();

        @Override
        public String toString() {
            return id + " " + extensions;
        }
    }
}
