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

    public void putIn(String bucket) {
        Bucket b = ZDB.getBucket(bucket);
        b.put(this);
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

        public Find findBy(String query, Object... params) {
            return new Find(this, createQuery(query, params));
        }
        
        public Find findAll() {
            return new Find(this, "");
        }

        String createQuery(String query, Object... params) {
            if (!query.contains(":") && !query.contains(" ") && query.trim().length() > 0) {
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

        public static class Find {

            String query;
            ZDBModelBucket bucket;

            public Find(ZDBModelBucket bucket, String query) {
                this.query = query;
                this.bucket = bucket;
            }

            public int count() {
                return ZDB.getBucket(bucket.bucket).count(query, bucket.type);
            }

            public <T extends ZDBModel> T one() {
                return null;
            }

            public <T extends ZDBModel> List<T> all() {
                return (List<T>) ZDB.getBucket(bucket.bucket).search(query, bucket.type, 0, 1);
            }

            public <T extends ZDBModel> List<T> page(int from, int size) {
                return (List<T>) ZDB.getBucket(bucket.bucket).search(query, bucket.type, from, size);
            }
            
            public Find orderBy(String key) {
                return new Find(bucket, query+" order by "+key);
            }
        }
    }
}
