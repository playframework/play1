# Scala
import sys
if play_command == 'scala:console':
	check_application()
  	load_modules()
       	do_classpath()
	#add precompiled classes to classpath
	cp_args += ":"+os.path.normpath(os.path.join(application_path,'tmp/classes'))
     	do_java()
	# replace last element with the console app
	java_cmd[len(java_cmd)-1]="play.console.Console"
	java_cmd.insert(2, '-Xmx256M -Xms32M')
  	subprocess.call(java_cmd, env=os.environ)
     	print
	sys.exit(0)
# ~~~~~~~~~~~~~~~~~~~~~~ New
if play_command == 'new':
	os.remove(os.path.join(application_path, 'app/controllers/Application.java'))
	shutil.copyfile(os.path.join(play_base, 'modules/scala/resources/Application.scala'), os.path.join(application_path, 'app/controllers/Application.scala'))
