package utils;
import java.util.List;
import play.db.jpa.*;
import play.db.jpa.GenericModel.JPAQuery;


public class InterpreterHelper {
  public static ThreadLocal<List<String>> out = new ThreadLocal<List<String>>();
  private static JPQL underlying = new JPQL();
	public static void println(Object line) {
		out.get().add(line.toString());
	}
	public static void print(Object line) {
		println(line);
	}
	public static List findAll(Class kl) {
		return underlying.findAll(kl.getName().substring(kl.getName().lastIndexOf(".")+1));
	}
	public static long count(Class kl) {
		return underlying.count(kl.getName().substring(kl.getName().lastIndexOf(".")+1));
	}

	public static long count(Class kl, String query, Object[] params) {
		return underlying.count(kl.getName().substring(kl.getName().lastIndexOf(".")+1),query,params);
	}

	public static JPABase findById(Class kl, Object id) throws Exception  {
		return underlying.findById(kl.getName().substring(kl.getName().lastIndexOf(".")+1),id);
	}

	public static List findBy(Class kl, String query, Object[] params) {
		return underlying.findBy(kl.getName().substring(kl.getName().lastIndexOf(".")+1),query,params);
	}

	public static JPAQuery find(Class kl, String query, Object[] params) {
		return underlying.find(kl.getName().substring(kl.getName().lastIndexOf(".")+1),query,params);
	}

	public static JPAQuery find(Class kl) {
		return underlying.find(kl.getName().substring(kl.getName().lastIndexOf(".")+1));
	}

	public static JPABase findOneBy(Class kl, String query, Object[] params) {
		return underlying.findOneBy(kl.getName().substring(kl.getName().lastIndexOf(".")+1),query,params);
	}
	public static int delete(Class kl, String query, Object[] params) {
		return underlying.delete(kl.getName().substring(kl.getName().lastIndexOf(".")+1),query,params);
	}

	public static int deleteAll(Class kl) {
		return underlying.deleteAll(kl.getName().substring(kl.getName().lastIndexOf(".")+1));
	}
}
