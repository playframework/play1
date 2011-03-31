package play.utils;

import play.mvc.Http;

public class FakeRequestCreator {

    public static Http.Request createFakeRequestFromBaseUrl(String baseUrl) {

        String path = null;
        String host = null;
        int port = 0;
        boolean secure = false;

        try{

            if( baseUrl.startsWith("http://")) {
                baseUrl = baseUrl.substring(7);
            } else if( baseUrl.startsWith("https://")) {
                secure = true;
                baseUrl = baseUrl.substring(8);

            } else {
                throw new Exception("invalid start of url: " + baseUrl);
            }

            int i = baseUrl.indexOf('/');
            if( i > 0 ) {
                host = baseUrl.substring(0,i);
                path = baseUrl.substring(i);
            } else {
                host = baseUrl;
                path = "/";
            }

            //check for port in host
            String[] hostParts = host.split(":");
            if( hostParts.length == 1) {
                if( secure ) {
                    port = 443;
                } else {
                    port = 80;
                }
            } else if(hostParts.length == 2) {
                host = hostParts[0];
                port = Integer.parseInt( hostParts[1] );
            } else {
                throw new Exception("too many : in hostname: " + host);
            }

        }catch(Exception e) {
            throw new RuntimeException("Error parsing baseUrl: " + baseUrl,e);
        }

        return Http.Request.createRequest(
                null,
                "GET",
                path,
                "",
                null,
                null,
                baseUrl,
                host,
                false,
                port,
                host,
                secure,
                null,
                null);
    }
}
