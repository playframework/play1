import play.jobs._
import play.test._
import play.db.jpa.QueryFunctions._
 
import models._
 
@OnApplicationStart
class Bootstrap extends Job {
 
    override def doJob {
        if(count[User] == 0) {
            Fixtures load "initial-data.yml"
        }
    }
 
}