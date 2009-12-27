
if play_command == ('grizzly:run'):
	check_application()
	load_modules()
	do_classpath()
	do_java('play.modules.grizzly.Server')
	print "~ Ctrl+C to stop"
	print "~ "
	if application_mode == 'dev':
		check_jpda()
		java_cmd.insert(2, '-Xdebug')
		java_cmd.insert(2, '-Xrunjdwp:transport=dt_socket,address=%s,server=y,suspend=n' % jpda_port)
		java_cmd.insert(2, '-Dplay.debug=yes')
	subprocess.call(java_cmd, env=os.environ)
	print
	sys.exit(0)