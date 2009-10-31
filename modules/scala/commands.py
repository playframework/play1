# Scala

# ~~~~~~~~~~~~~~~~~~~~~~ New
if play_command == 'new':
	os.remove(os.path.join(application_path, 'app/controllers/Application.java'))
	shutil.copyfile(os.path.join(play_base, 'modules/scala/resources/Application.scala'), os.path.join(application_path, 'app/controllers/Application.scala'))