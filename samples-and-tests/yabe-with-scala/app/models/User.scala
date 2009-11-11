package models
 
import java.util._
import javax.persistence._
 
import play.db.jpa._
import play.data.validation._
import play.db.jpa.QueryFunctions._
 
@Entity
class User extends Model {
 
    @Email
    @Required
    var email: String = _
    
    @Required
    var password: String = _
    
    var fullname: String = _
    
    var isAdmin: Boolean = _
    
    def this(email: String, password: String, fullname: String) {
        this()
        this.email = email
        this.password = password
        this.fullname = fullname
    }
    
    override def toString() = email
 
}

object User {
    
    def connect(email: String, password: String) = {
        find[User]("byEmailAndPassword", email, password).first
    }
    
}