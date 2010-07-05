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

    app.check()
    modules = app.modules()
    if not os.environ.has_key('JAVA_HOME'):
        javadoc_path = "javadoc"
    else:
        javadoc_path = os.path.normpath("%s/bin/javadoc" % os.environ['JAVA_HOME'])

    fileList = []
    def add_java_files(app_path):
        for root, subFolders, files in os.walk(os.path.join(app_path, 'app')):
            for file in files:
                if file.endswith(".java"):
                    fileList.append(os.path.join(root, file))
    add_java_files(app.path)
    for module in modules:
        add_java_files(os.path.normpath(module))
    outdir = os.path.join(app.path, 'javadoc')
    sout = open(os.path.join(app.log_path(), 'javadoc.log'), 'w')
    serr = open(os.path.join(app.log_path(), 'javadoc.err'), 'w')
    if (os.path.isdir(outdir)):
        shutil.rmtree(outdir)
    javadoc_cmd = [javadoc_path, '-classpath', app.cp_args(), '-d', outdir] + fileList
    print "Generating Javadoc in " + outdir + "..."
    subprocess.call(javadoc_cmd, env=os.environ, stdout=sout, stderr=serr)
    print "Done! You can open " + os.path.join(outdir, 'overview-tree.html') + " in your browser."
