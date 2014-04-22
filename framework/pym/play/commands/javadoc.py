import os, os.path
import shutil
import subprocess

from play.utils import *

COMMANDS = ['javadoc', 'jd']

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
    sout = open(os.path.join(app.log_path(), 'javadoc.log'), 'w')
    serr = open(os.path.join(app.log_path(), 'javadoc.err'), 'w')
    if (os.path.isdir(outdir)):
        shutil.rmtree(outdir)
        
    defineJavadocOptions(app, outdir, args)
    defineJavadocFiles(app)
    javadoc_cmd = [javadoc_path, '@javadocOptions', '@javadocFiles']
    
    print "Generating Javadoc in " + outdir + "..."
    subprocess.call(javadoc_cmd, env=os.environ, stdout=sout, stderr=serr)
    print "Done! You can open " + os.path.join(outdir, 'overview-tree.html') + " in your browser."
    
    
def defineJavadocOptions(app, outdir, args):
    f = open('javadocOptions', 'w')
    f.write(' '.join(['-classpath', app.cp_args(), '-d', outdir, '-encoding', 'UTF-8', '-charset', 'UTF-8']))
    
    argsCmd = ' '.join(args)
    if (argsCmd != ''):
        f.write(' ' + ' '.join(args))        
    f.close()
    
def defineJavadocFiles(app):
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
    f = open('javadocFiles', 'w')
    f.write(' '.join(fileList)) 
    f.close()
     