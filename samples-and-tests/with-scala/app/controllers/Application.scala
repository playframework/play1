package controllers;

import play._;
import play.mvc._;

object Application extends ControllerObject {
    
    def index() {
        val a = 25
        render(a)
    }
    
    def yop() {
        renderText("Cossucou")
    }
    
}
