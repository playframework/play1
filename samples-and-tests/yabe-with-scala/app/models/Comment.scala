package models
 
import java.util._
import javax.persistence._
 
import play.db.jpa._
import play.data.validation._
 
@Entity
class Comment extends Model {
 
    @Required
    var author: String = _
    
    @Required
    var postedAt: Date = _
     
    @Lob
    @Required
    @MaxSize(10000)
    var content: String = _
    
    @ManyToOne
    @Required
    var post: Post = _
    
    def this(post: Post, author: String, content: String) {
        this()
        this.post = post
        this.author = author
        this.content = content
        this.postedAt = new Date()
    }
    
    override def toString() = {
        if(content.length() > 50) content.substring(0, 50) else content
    }
 
}