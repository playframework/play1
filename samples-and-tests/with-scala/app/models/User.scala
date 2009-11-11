package models
 
import java.util._
import javax.persistence._
 
import play.db.jpa._
 
@Entity
class User(
    var email: String,
    var password: String,
    var fullname: String
) extends Model {
 
    var isAdmin: boolean = false
 
}

object User {
	val size = 98
}