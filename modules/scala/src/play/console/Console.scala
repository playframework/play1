package play.console
import scala.tools.nsc.MainGenericRunner
import java.io.File
import play.Play

object Console {
   def main(args : Array[String]) {
     val root = new File(System.getProperty("application.path"));
     Play.init(root, System.getProperty("play.id", ""));
     println("~")
     println("~ Starting up, please be patient")
     println("~ Ctrl+C to stop")
     println("~")
     Play.start()
     //TODO:fix readline support
     //now that the app is ready, launch scala REPL  
     try {	
       MainGenericRunner.main(args)
     } catch {
	case e:Exception=>
     }
     // After the repl exits, kill the scala script
     exit(0)
   }
}
