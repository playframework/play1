package play

import play.mvc._
import play.mvc.Scope._

class RichRenderArgs(val renderArgs: RenderArgs) {

    def +=(variable: Tuple2[String, Any]) {
        renderArgs.put(variable._1, variable._2)
    }

}

object Scala {

    implicit def richRenderArgs(x: RenderArgs) = new RichRenderArgs(x)

}
