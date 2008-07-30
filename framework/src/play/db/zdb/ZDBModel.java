package play.db.zdb;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zdb.client.Bucket;
import zdb.client.ZDB;
import org.apache.lucene.queryParser.ParseException;

public class ZDBModel {

    /**
     * when ZBModel is loaded from the bucket, keeps the bucket name for later update purposes
     */
    public String bucket;

    public static <T> ZDBModelBucket<T> in(String bucket) {
        throw new UnsupportedOperationException("Not implemented. Check the ZDBEnhancer !");
    }

    public void save() {
        Bucket b = ZDB.getBucket(this.bucket);
        if (b == null) {
            throw new RuntimeException("Invalid bucket " + this.bucket);
        }
        b.put(this);
    }

    public void deleteFrom(String bucket) {
        Bucket b = ZDB.getBucket(bucket);
        b.delete(this);
    }

    public static class ZDBModelBucket<T> {

        String bucket;
        Class<T> type;

        public ZDBModelBucket(Class<T> type, String bucket) {
            this.bucket = bucket;
            this.type = type;
        }

        /**
         * add or update a object in the bucket
         * object id is generated if not provided
         * @param object
         */
        public void save(T object) {
            ZDB.getBucket(bucket).put(object);
        }

        public <T> T findById(String id) {
            ZDBModel result = (ZDBModel) ZDB.getBucket(bucket).get(id, type);
            if (result != null) {
                result.bucket = bucket;
            }
            return (T) result;
        }

        public <T> Find<T> findBy(String query, Object... params) {
            return new Find(this, createQuery(query, params), type);
        }

        public <T> Find<T> findAll() {
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

        public static class Find<T> {

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
                ZDBModel result = (ZDBModel) query.one(null);
                if (result != null) {
                    result.bucket = bucket.bucket;
                }
                return (T) result;
            }

            public <T> List<T> all() {
                List<ZDBModel> result = (List<ZDBModel>) query.all(null);
                for (ZDBModel item : result) {
                    if (item != null) {
                        item.bucket = bucket.bucket;
                    }
                }
                return (List<T>) result;
            }

            public <T> List<T> page(int from, int size) {
                List<ZDBModel> result = (List<ZDBModel>) query.page(from, size, null);
                for (ZDBModel item : result) {
                    if (item != null) {
                        item.bucket = bucket.bucket;
                    }
                }
                return (List<T>) result;
            }

            public Find orderBy(String... key) {
                query = query.orderBy(key);
                return this;
            }
        }
    }
}
