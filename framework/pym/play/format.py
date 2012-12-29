import os, os.path, Properties

from xml.dom.minidom import parseString

def doFormatting(dotSettings,app,args,play_env):
	corePrefs = os.path.join(dotSettings, 'org.eclipse.jdt.core.prefs')
	uiPrefs = os.path.join(dotSettings, 'org.eclipse.jdt.ui.prefs')

	if app.readConf('code.style'):
		eclipseFormatter = app.readConf('code.style')
		
		fileXML = open(eclipseFormatter,'r')
		xmlContent = fileXML.read()
		fileXML.close()
		dom = parseString(xmlContent)
				
		config = Properties.Properties()
		try:
			config.load(open(corePrefs,'r'))
		except IOError:
			pass

		for node in dom.getElementsByTagName("setting"):
			attribute = node.getAttribute("id")
			value = node.getAttribute("value")
			config.setProperty(str(attribute), str(value))
		config.store(open(corePrefs, 'w'))

		node = dom.getElementsByTagName("profile")
		if (node.length > 0):
			config = Properties.Properties()
			try:
				config.load(open(uiPrefs,'r'))
			except IOError:
				pass
	
			config.setProperty('eclipse.preferences.version', '1')
			config.setProperty('formatter_profile', '_' + str(node[0].getAttribute("name")))
			config.setProperty('formatter_settings_version', str(node[0].getAttribute("version")))
			config.store(open(uiPrefs,'w'))
