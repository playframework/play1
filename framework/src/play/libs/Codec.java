package play.libs;

import java.util.UUID;

public class Codec {

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
