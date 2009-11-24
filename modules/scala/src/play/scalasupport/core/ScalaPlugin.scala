package play.scalasupport.core

import play._
import play.test._
import play.vfs.{VirtualFile => VFile}
import play.exceptions._
import play.classloading.ApplicationClasses.ApplicationClass

import scala.tools.nsc._
import scala.tools.nsc.reporters._
import scala.tools.nsc.util._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap

import scala.tools.nsc.io._

import java.util.{List => JList}

import org.scalatest.Suite
import org.scalatest.tools.ScalaTestRunner

class ScalaPlugin extends PlayPlugin {

    override def compileAll(classes: JList[ApplicationClass]) = {
        val sources = ListBuffer[VFile]()
        def scan(path: VFile): Unit = {
            path match {
                case _ if path.isDirectory => path.list foreach scan
                case _ if (path.getName().endsWith(".scala") || path.getName().endsWith(".java")) && !path.getName().startsWith(".") => sources add path
                case _ => 
            }
        }
        Play.javaPath foreach scan
        play.Logger.trace("SCALA compileAll")
        classes.addAll(compile(sources))
    }

    override def runTest(testClass: Class[BaseTest]) = {
        testClass match {
            case suite if classOf[Suite] isAssignableFrom testClass => ScalaTestRunner run suite.asInstanceOf[Class[Suite]]
            case _ => null
        }
    }

    override def onClassesChange(modified: JList[ApplicationClass]) {
        val sources = new java.util.ArrayList[VFile]
        modified foreach { cl: ApplicationClass =>
            var source = cl.javaFile
            if(!(sources contains source)) {
                sources add source
            }
        }
        compile(sources)
    }

    // Compiler

    private var compiler = new ScalaCompiler()

    def compile(sources: JList[VFile]) = {
        play.Logger.trace("SCALA compile %s", sources)
        compiler compile sources.toList
    }

    class ScalaCompiler {

        private val reporter = new Reporter() {

            override def info0(position: Position, msg: String, severity: Severity, force: Boolean) = {
                severity match {
                    case ERROR if position.isDefined => throw new CompilationException(realFiles.get(position.source.file.toString()).get, msg, position.line)
                    case ERROR => throw new CompilationException(msg);
                    case WARNING if position.isDefined => Logger.warn(msg + ", at line " + position.line + " of "+position.source)
                    case WARNING => Logger.warn(msg)
                    case INFO if position.isDefined => Logger.info(msg + ", at line " + position.line + " of "+position.source)
                    case INFO => Logger.info(msg)
                }
            }
            
        }

        // New compiler
        private val realFiles = HashMap[String,VFile]()
        private val virtualDirectory = new VirtualDirectory("(memory)", None)
        private val settings = new Settings()
        settings.debuginfo.level = 3
        settings.outputDirs setSingleOutput virtualDirectory        
        private val compiler = new Global(settings, reporter)

        def compile(sources: List[VFile]) = {
            val c = compiler
            val run = new c.Run()            

            // BatchSources
            realFiles.clear()
            var sourceFiles = sources map { vfile =>
                val name = vfile.relativePath
                realFiles.put(name, vfile)
                new BatchSourceFile(name, vfile.contentAsString)
            }

            // Clear compilation results
            //virtualDirectory.clear

            // Compile
            play.Logger.trace("SCALA Start compiling")
            run compileSources sourceFiles
            play.Logger.trace("SCALA Done ...")

            // Retrieve result
            val classes = new java.util.ArrayList[ApplicationClass]()

            def scan(path: AbstractFile): Unit = {
                path match {
                    case d: VirtualDirectory => path.iterator foreach scan
                    case f: VirtualFile =>
                                val byteCode = play.libs.IO.readContent(path.input)
                                val infos = play.utils.Java.extractInfosFromByteCode(byteCode)
                                var applicationClass = Play.classes.getApplicationClass(infos(0))
                                if(applicationClass == null) {
                                    applicationClass = new ApplicationClass() {

                                        override def compile() = {
                                            javaByteCode
                                        }

                                    }
                                    applicationClass.name = infos(0)
                                    applicationClass.javaFile = realFiles.get(infos(1)).get
                                    applicationClass.javaSource = applicationClass.javaFile.contentAsString
                                    play.Play.classes.add(applicationClass)
                                }
                                applicationClass.compiled(byteCode)
                                classes.add(applicationClass)
                }
            }
            virtualDirectory.iterator foreach scan

            //
            classes
        }

    }

}

