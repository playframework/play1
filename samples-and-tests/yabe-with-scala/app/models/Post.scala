package models
 
import java.util._
import javax.persistence._
 
import play.db.jpa._
import play.data.validation._
 
@Entity
class Post(

    @Required
    @ManyToOne
    var author: User,
    
    @Required
    var title: String,
    
    @Lob
    @Required
    @MaxSize(10000)
    var content: String

) extends Model[Post] {
    
    @Required
    var postedAt = new Date()  
    
    @OneToMany(mappedBy="post", cascade=Array(CascadeType.ALL))
    var comments: List[Comment] = new ArrayList[Comment]
    
    @ManyToMany(cascade=Array(CascadeType.PERSIST))
    var tags: Set[Tag] = new TreeSet[Tag]
    
    def addComment(author: String, content: String) = {
        val newComment = new Comment(this, author, content)
        newComment.save  
        comments.add(newComment)
        this
    }
    
    def previous = {
        Post.find("postedAt < ? order by postedAt desc", postedAt).first
    }

    def next = {
        Post.find("postedAt > ? order by postedAt asc", postedAt).first
    }
    
    def tagItWith(name: String) = {
        tags add Tag.findOrCreateByName(name)
        this
    }
    
    override def toString() = {
        title
    }
 
}

object Post extends Model[Post]{
    
    def findTaggedWith(tag: String) = {
        Post.find("select distinct p from Post p join p.tags as t where t.name = ?", tag).fetch
    }
    
    def findTaggedWith(tags: String*) = {
        Nil
    }
    
}
