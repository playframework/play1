# Here you can create play commands that are specific to the module

# Example below:
# ~~~~
if play_command == 'hello':
	try:
		print "~ Hello"
		sys.exit(0)
				
	except getopt.GetoptError, err:
		print "~ %s" % str(err)
		print "~ "
		sys.exit(-1)
		
	sys.exit(0)