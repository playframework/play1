package models
 
import java.util._
import javax.persistence._
 
import play.db.jpa._
import play.data.validation._
 
@Entity
class Comment(
       
       @ManyToOne
       @Required
       var post: Post,
       
       @Required
       var author: String, 
       
       @Lob
       @Required
       @MaxSize(10000)
       var content: String

) extends Model[Comment] {
    
    @Required
    var postedAt = new Date()
    
    override def toString() = {
        if(content.length() > 50) content.substring(0, 50) else content
    }
 
}
