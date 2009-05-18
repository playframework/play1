# CRUD

# ~~~~~~~~~~~~~~~~~~~~~~ [crud:ov] Override a view
if play_command == 'crud:ov' or play_command == 'crud:override':
	try:
		optlist, args = getopt.getopt(remaining_args, 't:', ['css','layout','template='])
		for o, a in optlist:
			if o in ('-t', '--template'):
				c = a.split('/')[0]
				t = a.split('/')[1]
				override('app/views/CRUD/%s.html' % t, 'app/views/%s/%s.html' % (c, t))
				print "~ "
				sys.exit(0)
			
			if o == '--layout':
				override('app/views/CRUD/layout.html', 'app/views/CRUD/layout.html')
				print "~ "
				sys.exit(0)
				
			if o == '--css':
				override('public/stylesheets/crud.css', 'public/stylesheets/crud.css')
				print "~ "
				sys.exit(0)
				
	except getopt.GetoptError, err:
		print "~ %s" % str(err)
		print "~ "
		sys.exit(-1)
	
	print "~ Specify the template to override, ex : -t Users/list" 
	print "~ "
	print "~ Use --css to override the CRUD css" 
	print "~ Use --layout to override the CRUD layout" 
	print "~ "
		
	sys.exit(0)