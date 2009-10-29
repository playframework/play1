package play.mvc;

import play.scalasupport.wrappers.ControllerWrapper
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport

class ControllerObject extends LocalVariablesSupport with ControllerSupport { 

    def render(args: Any*) {
        ControllerWrapper.render(args.map(_.asInstanceOf[AnyRef]): _*)
    }

    def renderText(text: Any, args: Any*) {
        ControllerWrapper.renderText(if(text == null) "" else text.toString(), args.map(_.asInstanceOf[AnyRef]): _*)
    }

}


