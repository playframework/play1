import os, os.path
import shutil
import subprocess
import re

from play.utils import *

COMMANDS = ['javadoc', 'jd']

DEFAULT_API_VERSION = "1.3.0"

HELP = {
    'javadoc': 'Generate your application Javadoc'
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    play_env = kargs.get("env")

    if not os.environ.has_key('JAVA_HOME'):
        javadoc_path = "javadoc"
    else:
        javadoc_path = os.path.normpath("%s/bin/javadoc" % os.environ['JAVA_HOME'])
        
    outdir = os.path.join(app.path, 'javadoc')
    # Clean current javadoc directory
    if (os.path.isdir(outdir)):
        shutil.rmtree(outdir)
    sout = open(os.path.join(app.log_path(), 'javadoc.log'), 'w')
    serr = open(os.path.join(app.log_path(), 'javadoc.err'), 'w')

    # Create Javadoc directory    
    if not os.path.exists(outdir):
        os.makedirs(outdir)      
    defineJavadocOptions(app, outdir, args)
    defineJavadocFiles(app, outdir)
    javadoc_cmd = [javadoc_path, '@'+os.path.join(outdir,'javadocOptions'), '@'+os.path.join(outdir,'javadocFiles')]
    
    print "Generating Javadoc in " + outdir + "..."
    subprocess.call(javadoc_cmd, env=os.environ, stdout=sout, stderr=serr)
    print "Done! You can open " + os.path.join(outdir, 'overview-tree.html') + " in your browser."
    # Remove configuration file
    os.remove(os.path.join(outdir , 'javadocOptions'))
    os.remove(os.path.join(outdir , 'javadocFiles'))
    
    
    
def defineJavadocOptions(app, outdir, args):
    f = open(os.path.join(outdir , 'javadocOptions'), 'w')
    f.write(' '.join(['-classpath', app.cp_args(), '-d', outdir, '-encoding', 'UTF-8', '-charset', 'UTF-8']))
   
    # Add some default options
    if args.count('-doctitle') == 0:
        f.write(' -doctitle "' + app.readConf('application.name')  + '"' )
        
    if args.count('-header') == 0:    
        f.write(' -header "<b>' +  app.readConf('application.name') + '</b>"')
        
    if args.count('-footer') == 0:    
        f.write(' -footer "<b>' +  app.readConf('application.name') + '</b>"')
  
    if args.count('--links'):
        print "~ Build project Javadoc with links to :"
        args.remove('--links')
        # Add link to JavaDoc of JAVA
        
        javaVersion = getJavaVersion()
        print "~ using java version \"%s\"" % javaVersion
        if javaVersion.startswith("1.5"):
            print "~    Java(TM) Platform, Platform Standard Edition 5.0"        
            print "~    Java(TM) EE 5 Specification APIs"
            f.write(' -link http://docs.oracle.com/javase/1.5.0/docs/api/')
            f.write(' -link http://docs.oracle.com/javaee/5/api/')   
        else:
            urlVersion = javaVersion[2:3]
            print "~    Java(TM) Platform, Standard Edition " + urlVersion + " API Specification"        
            print "~    Java(TM) EE " + urlVersion + " Specification APIs"
            f.write(' -link http://docs.oracle.com/javase/' + urlVersion + '/docs/api/')
            f.write(' -link http://docs.oracle.com/javaee/' + urlVersion + '/api/')         
     
       
        # Add link to JavaDoc of Play Framework
        playVersion = app.play_env['version']
        if "localbuild" in playVersion:
            print "~    API documentation to Play! Framework V" + playVersion + " doesn't exist => link to V" + DEFAULT_API_VERSION
            playVersion = DEFAULT_API_VERSION

        print "~    Play Framework V" + playVersion + " API documentation"     
        f.write(' -link https://www.playframework.com/documentation/' + playVersion + '/api/')

   
    argsCmd = ' '.join(args)
    if (argsCmd != ''):
        f.write(' ' + ' '.join(args))        
    f.close()
    
def defineJavadocFiles(app, outdir):
    fileList = []
    def add_java_files(path):
        for root, subFolders, files in os.walk(path):	
            for file in files:
                 if file.endswith(".java"):
   	                fileList.append(os.path.join(root, file))
                    
    add_java_files(os.path.join(app.path, "app"))
    add_java_files(os.path.join(app.path, "src"))
    
    # Javadoc of modules
    modules = app.modules()  
    for module in modules:
	    add_java_files(os.path.normpath(os.path.join(module, "app")))
	    add_java_files(os.path.normpath(os.path.join(module, "src")))
        
    #Write files list in files    
    f = open(os.path.join(outdir, 'javadocFiles'), 'w')
    f.write(' '.join(fileList)) 
    f.close()
  
