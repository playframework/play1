package controllers 

import play._
import play.mvc._
import play.db.jpa._
import play.data.validation._
import play.libs._

import models._

object Application extends Actions {
    
    @Before
    private def check {
        println("Check ... " + configuration("yop", "nf") )
        renderArgs += ("kiki" -> 9)
    }
    
    def test {        
        response <<< OK
        response <<< "text/plain"
        response <<< ("X-Test" -> "Yop")
        response <<< """|Hello World
                        |
                        |My Name is Guillaume""".stripMargin        
    }
    
    def test2 {        
        response <<< NOT_FOUND
        response <<< "text/html"
        response <<< <h1>Not found, sorry</h1>        
    }
    
    def index(@Min(10) nimp: Int = 5, @Required name: String = "Guillaume") {
        println(nimp)
        println(request.path)
        val age = 59 
        var yop = 8
        yop = yop + 3
        println(name)
        
        info("Yop %d", 9)
        
        val users = find[User]("byPassword", "88style").fetch
        
        response <<< OK
        response <<< "YOUHOU" 
        response <<< "X-Yop" -> "hope"
        
        render(name, age, yop, users)
    }
    
    def addOne() {
        val user = new User("guillaume@gmail.com", "88style", "Guillaume")
        user.save()
        index()
    }
    
    def goJojo() {
        index(name="Jojo") 
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
