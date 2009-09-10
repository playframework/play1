# Secure

# ~~~~~~~~~~~~~~~~~~~~~~ [secure:ov] Override a view
if play_command == 'secure:ov' or play_command == 'secure:override':
	try:
		optlist, args = getopt.getopt(remaining_args, '', ['css'])
		for o, a in optlist:				
			if o == '--css':
				override('public/stylesheets/secure.css', 'public/stylesheets/secure.css')
				print "~ "
				sys.exit(0)
				
	except getopt.GetoptError, err:
		print "~ %s" % str(err)
		print "~ "
		sys.exit(-1)
	
	print "~ Use --css to override the Secure css" 
	print "~ "
		
	sys.exit(0)