package controllers
 
import play._
import play.mvc._
import play.db.jpa._
import play.data.validation._
 
import models._

@With(Array(classOf[Secure])) 
object Admin extends Controller with Defaults {
    
    @Before
    private def setConnectedUser{
        if(Secure.Security.isConnected()) {
            val user = find[User]("byEmail", Secure.Security.connected()).first
            renderArgs += "user" -> user.fullname
        }
    }
 
    def index {
        val posts = find[Post]("author.email", Secure.Security.connected()).fetch
        render(posts)
    }
    
    def form(id: Long) {
        if(id != 0) {
            val post = findById[Post](id)
            render(post)
        }
        render()
    }
    
    def save(id: Long, title: String, content: String, tags: String) {
        var post: Post = null
        if(id == 0) {
            // Create post
            val author = find[User]("byEmail", Secure.Security.connected()).first;
            post = new Post(author, title, content)
        } else {
            // Retrieve post
            post = findById[Post](id)
            post.title = title
            post.content = content
            post.tags.clear()
        }
        // Set tags list
        tags.split("""\s+""") foreach { tag: String =>
            if(tag.trim().length > 0) {
                post.tags add Tag.findOrCreateByName(tag)
            }
        }
        // Validate
        validation.valid(post)
        if(Validation.hasErrors()) {
            render("@form", post)
        }
        // Save
        post.save()
        index
    }
    
}

// Security

object Security extends Secure.Security {

    private def authentify(username: String, password: String) = {
        User.connect(username, password) != null
    }
    
    private def check(profile: String) = {
        profile match {
            case "admin" => find[User]("byEmail", Secure.Security.connected).first.isAdmin
            case _ => false
        }
    }
    
    private def onDisconnected = Application.index
    
    private def onAuthenticated = Admin.index
    
}

// CRUD

@Check(Array("admin")) @With(Array(classOf[Secure])) object Comments extends CRUD
@Check(Array("admin")) @With(Array(classOf[Secure])) object Posts extends CRUD 
@Check(Array("admin")) @With(Array(classOf[Secure])) object Tags extends CRUD
@Check(Array("admin")) @With(Array(classOf[Secure])) object Users extends CRUD 

