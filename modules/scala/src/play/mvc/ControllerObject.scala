package play.mvc;

import play.scalasupport.wrappers.ControllerWrapper
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport

class ControllerObject extends LocalVariablesSupport { 

    def render(args: Any*) {
        ControllerWrapper.render(args.map(_.asInstanceOf[AnyRef]): _*)
    }

    def renderText(text: Any, args: Any*) {
        ControllerWrapper.renderText(if(text == null) "" else text.toString(), args.map(_.asInstanceOf[AnyRef]): _*)
    }

}


