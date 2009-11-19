import play.mvc.Scope._
import play.mvc.Http._

package play {

    package object mvc {

        // -- IMPLICITS

        implicit def richRenderArgs(x: RenderArgs) = new RichRenderArgs(x)
        implicit def richResponse(x: Response) = new RichResponse(x)

        // -- HELPERS

        def header(name: String, value: String) = new Header(name, value)
        def header(h: Tuple2[String, String]) = new Header(h._1, h._2)

        // -- STATUS CODE

        val OK              = 200
        val CREATED         = 201
        val ACCEPTED        = 202
        val NO_CONTENT      = 204
        val FORBIDDEN       = 403
        val NOT_FOUND       = 404
        val ERROR           = 500

    }

}

package play.mvc {

    class RichRenderArgs(val renderArgs: RenderArgs) {

        def +=(variable: Tuple2[String, Any]) {
            renderArgs.put(variable._1, variable._2)
        }

    }

    class RichResponse(val response: Response) {

        val ContentTypeRE = """[-a-zA-Z]+/[-a-zA-Z]+""".r

        def <<<(x: String) {
            x match {
                case ContentTypeRE() => response.contentType = x
                case _ => response.print(x)
            }
        }

        def <<<(header: Header) {
            response.setHeader(header.name, header.value())
        }

        def <<<(header: Tuple2[String, String]) {
            response.setHeader(header._1, header._2)
        }

        def <<<(status: Int) {
            response.status = status
        }

        def <<<(xml: scala.xml.NodeSeq) {
            response.print(xml)
        }

    }

}