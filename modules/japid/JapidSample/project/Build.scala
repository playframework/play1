import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "japidsample"
    val appVersion      = "0.8.0"

    val appDependencies = Seq(
        javaCore
      // Add your project dependencies here,
    )

    val main = play.Project(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    )

}
