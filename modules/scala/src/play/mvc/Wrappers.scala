package play.mvc;

import scala.xml.NodeSeq
import scala.io.Source

import java.io.InputStream

import play.mvc.Http._
import play.mvc.Scope._
import play.data.validation.Validation
import play.scalasupport.wrappers.ControllerWrapper
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport

class ScalaController extends LocalVariablesSupport with ControllerSupport {

    def request = Request.current()
    def response = Response.current()
    def session = Session.current()
    def flash = Flash.current()
    def params = Params.current()
    def renderArgs = RenderArgs.current()
    def validation = Validation.current()

    // ~~~ Results

    def render(args: Any*) {
        ControllerWrapper.render(args.map(_.asInstanceOf[AnyRef]): _*)
    }

    def renderText(text: Any, args: Any*) {
        ControllerWrapper.renderText(if(text == null) "" else text.toString(), args.map(_.asInstanceOf[AnyRef]): _*)
    }

    def renderXml(xml: String) {
        ControllerWrapper.renderXml(xml)
    }

    def renderXml(xml: NodeSeq) {
        ControllerWrapper.renderXml(xml.toString())
    }

    def renderBinary(stream: Source) {
        //ControllerWrapper.renderBinary(stream)
    }

    def renderBinary(stream: InputStream) {
        ControllerWrapper.renderBinary(stream)
    }

    def renderJSON(json: String) {
        ControllerWrapper.renderJSON(json)
    }

    def unauthorized(realm: String = "") {
        ControllerWrapper.unauthorized(realm)
    }

    def notFound(what: String = "") {
        ControllerWrapper.notFound(what)
    }

    def notFoundIfNull(o: Any) {
        ControllerWrapper.notFoundIfNull(o)
    }

    def ok {
        ControllerWrapper.ok()
    }

    def forbidden {
        ControllerWrapper.forbidden()
    }

    def unauthorized {
        ControllerWrapper.unauthorized("")
    }

}

class RichRenderArgs(val renderArgs: RenderArgs) {

    def +=(variable: Tuple2[String, Any]) {
        renderArgs.put(variable._1, variable._2)
    }

}

class RichSession(val session: Session) {

    def apply(key: String) = session.get(key)

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


