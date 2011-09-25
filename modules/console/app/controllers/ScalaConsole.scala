import scala.collection.mutable._
import play.scalasupport.core.OnTheFly


package object interpreted {
    def println(v: Any) {
        env.Env.out.get += v.toString
    }

    def print(v: Any) {
        println(v)
    }
    def traceToString(aThrowable: Throwable ) = {
          import java.io._
          val result = new StringWriter
          val printWriter = new PrintWriter(result)
          aThrowable.printStackTrace(printWriter)
          result.toString
    }
}

package env {
    object Env {
        val out = new ThreadLocal[ListBuffer[String]]
    }
}

package controllers {

    import play._
    import play.mvc._

    @With(Array(classOf[Secure]))
    @Check(Array("isAdmin"))
    object ScalaConsole extends Controller {

        def index(script: String = "println(\"hello scala!\")") {
              env.Env.out set ListBuffer[String]()
              renderArgs.put("action","/console/scala");
              if(request.method == "POST" && script != null) {
                try {
                synchronized{
                 OnTheFly.eval(script)
                 }
                } catch {
                  case e: Exception => "/console/repl.html".render("error" -> e, "trace" -> interpreted.traceToString(e), "script" -> script)
                }
              }
            "/console/repl.html".render("results" -> env.Env.out.get, "script"->script)
        }

    }


}

