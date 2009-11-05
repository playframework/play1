package controllers;

import scala.xml._

import play._;
import play.mvc._;
import play.data.validation._
import play.libs._

import models._;

object Application extends Actions {
    
    @Before
    private def check {
        println("Check ...")
    }
    
    def index(@Required name: String) {
        println(request.path)
        val age = 59 
        var yop = 8
        yop = yop + 3
        println(name)
        render(name, age, yop)
    }
    
    def goJojo() {
        index("Jojo") 
    }
    
    def api = renderXml(<items><item id="3">Yop</item></items>) 
    
    def yop = render("@index") 
    
    def helloWorld = <h1>Hello world!</h1>
    
    def hello(name: String) = <h1>Hello { if(name != null) name else "Guest" }!</h1>
    
    def nbOfUsers = User.size
    
    def captcha = Images.captcha
    
    @After
    private def log {
        println(new User("tom@gmail.com", "secret", "Tom"))
        println(new Company)
        //renderText("Oops")
    }
    
}
