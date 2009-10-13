package play.mvc;

class ControllerObject extends play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport { 
    
    def render(args: Any*) {
        Controller.render(args.map(_.asInstanceOf[AnyRef]): _*)
    }

    def renderText(text: Any, args: Any*) {
        Controller.renderText(if(text == null) "" else text.toString(), args.map(_.asInstanceOf[AnyRef]): _*)
    }

}


