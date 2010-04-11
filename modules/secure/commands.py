# Secure

# ~~~~~~~~~~~~~~~~~~~~~~ [secure:ov] Override a view
if play_command == 'secure:ov' or play_command == 'secure:override':
	try:
		optlist, args = getopt.getopt(remaining_args, '', ['css', 'login', 'layout'])
		for o, a in optlist:				
			if o == '--css':
				override('public/stylesheets/secure.css', 'public/stylesheets/secure.css')
				print "~ "
				sys.exit(0)
			if o == '--login':
				override('app/views/Secure/login.html', 'app/views/Secure/login.html')
				print "~ "
				sys.exit(0)
			if o == '--layout':
				override('app/views/Secure/layout.html', 'app/views/Secure/layout.html')
				print "~ "
				sys.exit(0)
				
	except getopt.GetoptError, err:
		print "~ %s" % str(err)
		print "~ "
		sys.exit(-1)

if play_command.startswith('secure:'):
	print "~ Use: --css to override the Secure css" 
	print "~      --login to override the login page" 
	print "~      --layout to override the login layout page" 
	print "~ "
	
	sys.exit(0)
