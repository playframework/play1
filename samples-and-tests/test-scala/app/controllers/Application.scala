package controllers

import play._
import play.mvc._

object Application extends Controller with Secure {
    
    def index(name: String = "Guest") = render(name)
    
}

trait Secure extends Controller {
    
    @Before
    def check {
        request.user match {
            case user: String => renderArgs += ("connected" -> user)
            case _ => unauthorized
        }
    }
    
}