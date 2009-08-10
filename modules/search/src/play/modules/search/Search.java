package play.modules.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.FSDirectory;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.jpa.JPA;
import play.db.jpa.JPAModel;
import play.db.jpa.JPASupport;
import play.exceptions.UnexpectedException;
/**
 * Very basic tool to basic search on your JPA objects.
 * 
 * On a JPAModel subclass, add the @Indexed annotation 
 * on your class, and the @Field annotation on your 
 * field members
 * 
 * Each time you save, update or delete your class, the 
 * corresponding index is updated
 * 
 * use the search method to query an index.
 * 
 * Samples in samples-and-tests/app/controllers/JPASearch.java
 */
public class Search {

    private static Map<String, IndexWriter> indexWriters = new HashMap<String, IndexWriter>();
    private static Map<String, IndexSearcher> indexReaders = new HashMap<String, IndexSearcher>();
    
    public static String DATA_PATH;
    private static String ANALYSER_CLASS;
    
    static {
        ANALYSER_CLASS = Play.configuration.getProperty("play.search.analyser", "org.apache.lucene.analysis.standard.StandardAnalyzer");
        if (Play.configuration.containsKey("play.search.path"))
            DATA_PATH = Play.configuration.getProperty("play.search.path");
        else
            DATA_PATH = Play.applicationPath.getAbsolutePath()+"/data/search/";
        Logger.trace("Search module repository is in "+DATA_PATH);
        if(Play.configuration.containsKey("play.search.reindex")) {
            Logger.info ("Reindexing ...");
            try {
                reindex();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static Analyzer getAnalyser() {
        try {
            Class clazz = Class.forName(ANALYSER_CLASS);
            return (Analyzer) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class SearchException extends RuntimeException {
        public SearchException(String message, Throwable cause) {
            super(message, cause);
        }
        public SearchException(Throwable cause) {
            super (cause);
        }
        public SearchException (String message) {
            super (message);
        }
    }
    
    public static class QueryResult {
        public String id;
        public float score;
        public JPAModel object;
    }
    
    public static class Query {
        private Class clazz;
        private String query;
        private String[] order = new String[0];
        private int offset=0;
        private int pageSize=10;
        private boolean reverse = false;
        
        protected Query (String query, Class clazz) {
            this.query=query;
            this.clazz=clazz;
        }
        public Query page (int offset, int pageSize) {
            this.offset=offset;
            this.pageSize=pageSize;
            return this;
        }
        
        public Query reverse () {
            this.reverse=true;
            return this;
        }
        
        public Query orderBy (String ... order) {
            this.order=order;
            return this;
        }
        
        private Sort getSort () {
            Sort sort = new Sort ();
            if (order.length>0) {
                if (reverse) {
                    if (order.length!=1)
                        throw new SearchException ("reverse can be used while sorting only one field with oderBy");
                    else
                        sort.setSort(order[0], reverse);
                } else
                    sort.setSort(order);
            }
            return sort;
        }
        
        /**
         * Executes the query and return directly JPAModel objects
         * (No score information)
         * @return
         */
        public <T extends JPASupport> List<T> fetch () throws SearchException{
            try {
                List<QueryResult> results = executeQuery(true);
                List<JPAModel> objects = new ArrayList<JPAModel>();
                for (QueryResult queryResult : results) {
                    objects.add(queryResult.object);
                }
                return (List)objects;
            } catch (Exception e) {
                throw new UnexpectedException (e);
            }
        }
        
        /**
         * Executes the lucene query against the index. You get QueryResults.
         * @param fetch load the corresponding JPAModel objects in the QueryResult Object
         * @return
         */
        public List<QueryResult> executeQuery (boolean fetch) throws SearchException {
            try {
                org.apache.lucene.search.Query luceneQuery = new QueryParser("_docID", getAnalyser()).parse(query);
                Hits hits = getIndexReader(clazz.getName()).search(luceneQuery,getSort());
                List<QueryResult> results = new ArrayList<QueryResult>();
                if( hits == null )
                    return results;
                
                int l = hits.length();
                if (offset > l) {
                    return results;
                }
                
                for (int i = offset; i < (offset + pageSize > l ? l : offset + pageSize); i++) {
                    QueryResult qresult = new QueryResult();
                    qresult.score = hits.score(i);
                    qresult.id = hits.doc(i).get("_docID");
                    if (fetch) {
                        qresult.object=(JPAModel) JPA.em().find(clazz,Long.parseLong(qresult.id));
                        if (qresult.object==null) throw new SearchException ("Please re-index");
                    }
                    results.add(qresult);
                }
                return results;
            } catch (ParseException e) {
                throw new SearchException (e);
            } catch (Exception e) {
                throw new UnexpectedException (e);
            }
        }
    }
    
    public static Query search (String query, Class clazz) {
        return new Query (query,clazz);
    }
    
    public static void unIndex (Object object) {
        try {
            if (! (object instanceof JPAModel))
                return;
            JPAModel jpaModel = (JPAModel) object;
            String index = object.getClass().getName();
            indexWriters.get(index).deleteDocuments(new Term("_docID", jpaModel.id+""));
        } catch (Exception e) {
            throw new UnexpectedException (e);
        }
    }
    
    public static void index (Object object) {
        try {
            if (! (object instanceof JPAModel))
                return;
            JPAModel jpaModel = (JPAModel) object;
            String index = object.getClass().getName();
            Document document = toDocument(object);
            if (document==null)
                return;
            getIndexWriter(index).deleteDocuments(new Term("_docID", jpaModel.id+""));
            getIndexWriter(index).addDocument(document);
            getIndexWriter(index).flush();
        } catch (Exception e) {
            throw new UnexpectedException (e);
        }
    }
    
    private static Document toDocument (Object object) throws Exception {
        Indexed indexed = object.getClass().getAnnotation(Indexed.class);
        if (indexed==null)
            return null;
        if (! (object instanceof JPASupport))
            return null;
        JPAModel jpaModel = (JPAModel) object;
        Document document = new Document ();
        document.add(new Field("_docID", jpaModel.id+"", Field.Store.YES, Field.Index.UN_TOKENIZED));
        for (java.lang.reflect.Field field : object.getClass().getFields()) {
            play.modules.search.Field index = field.getAnnotation(play.modules.search.Field.class);
            if (index==null) continue;
            if (field.getType().isArray()) continue;
            if (field.getType().isAssignableFrom(Collection.class)) continue;
            
            String name = field.getName();
            String value = valueOf(object, field);
            document.add(new Field(name, value, index.stored()?Field.Store.YES:Field.Store.NO, index.tokenize() ? Field.Index.TOKENIZED:Field.Index.UN_TOKENIZED));
        }
        return document;
    }
    
    private static String valueOf (Object object, java.lang.reflect.Field field) throws Exception {
        if (field.getType().equals(String.class))
            return (String)field.get(object);
        return ""+field.get(object);
    }
    
    public static IndexSearcher getIndexReader(String name) {
        try {
            if (!indexReaders.containsKey(name) ) {
                synchronized (Search.class) {
                    if (indexReaders.containsKey(name))
                        indexReaders.get(name).close();
                    IndexSearcher old = indexReaders.get(name);
                    if (old!=null) old.close();
                    File root = new File(DATA_PATH, name);
                    if (root.exists()) {
                        IndexSearcher reader = new IndexSearcher(FSDirectory.getDirectory(root));
                        indexReaders.put(name, reader);
                    }
                }
            }
            return indexReaders.get(name);
        } catch (Exception e) {
            throw new UnexpectedException("Cannot open index", e);
        }
    }
    
    private static IndexWriter getIndexWriter (String name) {
        try {
            if (!indexWriters.containsKey(name)) {
                synchronized (Search.class) {
                    File root = new File(DATA_PATH, name);
                    if (!root.exists())
                        root.mkdirs();
                    if (new File(root, "write.lock").exists())
                        new File(root, "write.lock").delete();
                    IndexWriter writer = new IndexWriter(FSDirectory.getDirectory(root), true, getAnalyser());
                    indexWriters.put(name, writer);
                }
            }
            return indexWriters.get(name);
        } catch (Exception e) {
            throw new UnexpectedException (e);
        }
    }
    
    public static void reindex ()  throws Exception {
        shutdown();
        File fl = new File (DATA_PATH);
        FileUtils.deleteDirectory(fl);
        fl.mkdirs();
        List<ApplicationClass> classes = Play.classes.getAnnotatedClasses(Indexed.class);
        for (ApplicationClass applicationClass : classes) {
            List<JPAModel> objects = (List<JPAModel>) JPA.em().createQuery("select e from "+applicationClass.getClass().getName()).getResultList();
            for (JPAModel model : objects) {
                index(model);
            }
        }
    }
    
    public static void shutdown() throws Exception {
        for (IndexWriter writer : indexWriters.values()) {
            writer.close();
        }
        for (IndexSearcher searcher : indexReaders.values()) {
            searcher.close();
        }
        indexWriters.clear();
        indexReaders.clear();
    }
}
