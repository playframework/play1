package models
 
import java.util._
import javax.persistence._
 
import play.db.jpa._
import play.data.validation._
 
@Entity
class Tag private (

    @Required 
    var name:String
    
) extends Model with Comparable[Tag] {
    
    override def toString() = {
        name
    }
    
    override def compareTo(otherTag: Tag) = {
        name.compareTo(otherTag.name)
    }
 
}

object Tag {
    
    def allTags = findAll[Tag]
    
    def findOrCreateByName(name: String) = {
        var tag = find[Tag]("byName", name).first
        if(tag == null) {
            tag = new Tag(name)
        }
        tag
    }
    
    def cloud = {
        find[Tag](
            "select new map(t.name as tag, count(p.id) as pound) from Post p join p.tags as t group by t.name"
        ).fetch
    }
    
}