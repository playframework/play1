package controllers;

import play._;
import play.mvc._;
import play.data.validation._

import models._;

object Application extends ControllerObject {
    
    @Before
    private def check {
        println("Check ...")
    }
    
    def index(@Required name: String) {
        val age = 59 
        var yop = 8
        yop = yop + 3
        println(name)
        render(name, age, yop)
    }
    
    def goJojo() {
        index("Jojo") 
    }
    
    def helloWorld = "Hello world !"
    
    def hello(name: String) = if(name != null) "Hello " + name else "Hello guest"
    
    def nbOfUsers = User.size
    
    @After
    private def log {
        println(new User)
        println(new Company)
        //renderText("Oops")
    }
    
}
