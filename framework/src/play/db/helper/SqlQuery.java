package play.db.helper;

import java.util.ArrayList;
import java.util.List;

public abstract class SqlQuery {

    protected List<Object> params;

    protected SqlQuery() {
        params = new ArrayList<Object>();
    }

    public SqlQuery param(Object obj) { params.add(obj); return this; }
    public SqlQuery params(Object ... objs) { for (Object obj : objs) params.add(obj); return this; }
    public List<Object> getParams() { return params; }

    public static class Concat {
        private String prefix, separator, suffix;
        private String defaultValue;
        private String expr;

        public Concat(String prefix, String separator, String suffix) {
            this.prefix = prefix;
            this.separator = separator;
            this.suffix = suffix;
            this.defaultValue = "";
            this.expr = "";
        }

        public Concat(String prefix, String separator) {
            this(prefix, separator, "");
        }

        public Concat defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Concat prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Concat separator(String separator) {
            this.separator = separator;
            return this;
        }

        public Concat append(Object obj) {
            final String text;
            if (obj != null && obj.toString().length() > 0) text = obj.toString();
            else text = defaultValue;

            if (text != null) {
                if (expr.length() > 0) {
                    if (separator == null) throw new NullPointerException();
                    expr += separator;
                }
                expr += text;
            }
            return this;
        }

        public Concat add(String ... texts) {
            for (String text : texts) append(text);
            return this;
        }

        public boolean isEmpty() {
            return expr.length()<=0;
        }

        @Override
        public String toString() {
            if (isEmpty()) return "";
            if (prefix == null || suffix == null) throw new NullPointerException();
            return prefix + expr + suffix;
        }
    }

    public static String quote(String str) {
        return "'" + str.replace("'","\\'") + "'";
    }

    public static String inlineParam(Object param) {
        if (param == null) return "NULL";

        String str;
        if (param instanceof String) str = quote(param.toString());
        else if (param instanceof List<?>) {
            Concat list = new Concat("(", ",");
            for (Object p : (List<?>)param) list.append(inlineParam(p));
            str = list.toString();
            if (str.length()>0) str += ")";
        } else if (param instanceof Object[]) {
            Concat list = new Concat("(", ",");
            for (Object p : (Object[])param) list.append(inlineParam(p));
            str = list.toString();
            if (str.length()>0) str += ")";
        } else str = param.toString();
        return str;
    }

}
