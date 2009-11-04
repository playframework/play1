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

class Actions extends LocalVariablesSupport with ControllerSupport {

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

    def ok() {
        ControllerWrapper.ok()
    }

}


