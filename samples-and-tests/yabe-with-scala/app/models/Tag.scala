package models
 
import java.util._
import javax.persistence._
 
import play.db.jpa._
import play.data.validation._
 
@Entity
class Tag private (

    @Required 
    var name:String
    
) extends Model[Tag] with Comparable[Tag] {
    
    override def toString() = {
        name
    }
    
    override def compareTo(otherTag: Tag) = {
        name.compareTo(otherTag.name)
    }
 
}

object Tag extends Model[Tag]{
    
    def allTags = Tag.findAll
    
    def findOrCreateByName(name: String) = {
        var tag = Tag.find("byName", name).first
        if(tag == null) {
            tag = new Tag(name)
        }
        tag
    }
    
    def cloud = {
        Tag.find(
            "select new map(t.name as tag, count(p.id) as pound) from Post p join p.tags as t group by t.name"
        ).fetch
    }
    
}
