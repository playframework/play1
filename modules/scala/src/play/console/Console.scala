package play.console
import scala.tools.nsc.MainGenericRunner
import java.io.File
import play.Play
import play.db.jpa.JPAPlugin
object Console {
   def main(args : Array[String]) {
     val root = new File(System.getProperty("application.path"));
     Play.init(root, System.getProperty("play.id", ""));
     println("~")
     println("~ Starting up, please be patient")
     println("~ Ctrl+D to stop")
     println("~")
     Play.start()
     JPAPlugin.startTx(false)  
     //TODO:fix readline support
     //now that the app is ready, launch scala REPL  
     try {	
       MainGenericRunner.main(args)
     } catch {
	case e:Exception=>
     }
     // After the repl exits, kill the scala script
     JPAPlugin.closeTx(false)
     exit(0)
   }
}
