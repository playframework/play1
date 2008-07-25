package play.db.zdb;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zdb.client.Bucket;
import zdb.client.ZDB;
import org.apache.lucene.queryParser.ParseException;

public class ZDBModel {

    public static ZDBModelBucket in(String bucket) {
        throw new UnsupportedOperationException("Not implemented. Check the ZDBEnhancer !");
    }

    public void putIn(String bucket) {
        Bucket b = ZDB.getBucket(bucket);
        b.put(this);
    }

    public void deleteFrom (String bucket) {
    	Bucket b = ZDB.getBucket(bucket);
    	b.delete(this);
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
            return new Find(this, createQuery(query, params), type);
        }

        public Find findAll() {
            return new Find(this, null, type);
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

        public static class Find<T extends ZDBModel> {

            Bucket.ObjectQuery<T> query;
            ZDBModelBucket bucket;
            Class<T> clazz;

            public Find(ZDBModelBucket bucket, String query, Class<T> clazz) {
                try {
                    this.bucket = bucket;
                    if (query == null || query.trim().length() == 0) {
                        this.query = ZDB.getBucket(bucket.bucket).search(clazz);
                    } else {
                        this.query = ZDB.getBucket(bucket.bucket).search(query, clazz);
                    }
                } catch (ParseException e) {
                    throw new RuntimeException("Invalid query " + query);
                } 
            }

            public int count() {
                return query.count();
            }

            public <T> T one() {
                return (T) query.one(null);
            }

            public <T> List<T> all() {
                return (List<T>) query.all(null);
            }

            public <T> List<T> page(int from, int size) {
                return (List<T>) query.page(from, size,null);
            }

            public Find orderBy(String... key) {
                query = query.orderBy(key);
                return this;
            }
        }
    }
}
