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

) extends Model {
    
    @Required
    var postedAt = new Date()  
    
    @OneToMany(mappedBy="post", cascade=Array(CascadeType.ALL))
    var comments: List[Comment] = new ArrayList[Comment]
    
    @ManyToMany(cascade=Array(CascadeType.PERSIST))
    var tags: Set[Tag] = new TreeSet[Tag]
    
    def addComment(author: String, content: String) = {
        val newComment = new Comment(this, author, content).save()
        comments.add(newComment)
        this
    }
    
    def previous = {
        find[Post]("postedAt < ? order by postedAt desc", postedAt).first
    }

    def next = {
        find[Post]("postedAt > ? order by postedAt asc", postedAt).first
    }
    
    def tagItWith(name: String) = {
        tags add Tag.findOrCreateByName(name)
        this
    }
    
    override def toString() = {
        title
    }
 
}

object Post {
    
    def findTaggedWith(tag: String) = {
        find[Post]("select distinct p from Post p join p.tags as t where t.name = ?", tag).fetch
    }
    
    def findTaggedWith(tags: String*) = {
        /*find[Po](
            "select distinct p.id from Post p join p.tags as t where t.name in (:tags) group by p.id having count(t.id) = :size"
        ).bind("tags", tags).bind("size", tags.length).fetch();*/
        Nil
    }
    
}