package controllers;

import play._;
import play.mvc._;

object Application extends ControllerObject {
    
    def index() {
        render()
    }
    
    def hello(name: String) {
        render(name)
    }
    
    def yop() = hello("Guillaume")
    
}
