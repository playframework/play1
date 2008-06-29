package play.db.zdb;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zdb.client.Bucket;
import zdb.client.ZDB;

public class ZDBModel {

    public static ZDBModelBucket in(String bucket) {
        throw new UnsupportedOperationException("Not implemented. Check the ZDBEnhancer !");
    }

    public static class ZDBModelBucket {

        String bucket;
        Class<? extends ZDBModel> type;

        public ZDBModelBucket(Class<? extends ZDBModel> type, String bucket) {
            this.bucket = bucket;
            this.type = type;
        }

        public <T extends ZDBModel> T findById(String id) {
            return (T) ZDB.getBucket(bucket).get(id, type);
        }

        public <T extends ZDBModel> List<T> findBy(String query, Object... params) {
            return (List<T>) ZDB.getBucket(bucket).search(createQuery(query, params), type);
        }

        public int count(String query, Object... params) {
            return ZDB.getBucket(bucket).count(query, type);
        }

        public void save(String bucket) {
            Bucket b = ZDB.getBucket(bucket);
            b.put(this);
        }

        String createQuery(String query, Object... params) {
            if (!query.contains(":") && !query.contains(" ")) {
                query = query + ":?";
            }
            Matcher matcher = Pattern.compile(":\\?").matcher(query);
            StringBuffer sb = new StringBuffer();
            int i = 0;
            while (matcher.find() && i < params.length) {
                matcher.appendReplacement(sb, ":\"" + params[i++].toString() + "\"");
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
    }
}
