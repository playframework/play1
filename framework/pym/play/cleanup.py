import os, os.path, Properties

from xml.dom.minidom import parseString

def doCleanUp(dotSettings,app,args,play_env):
	uiPrefs = os.path.join(dotSettings, 'org.eclipse.jdt.ui.prefs')

	if app.readConf('code.cleanup'):
		configuredCleanUp = app.readConf('code.cleanup')

		try:
			config = Properties.Properties()
			config.load(open(uiPrefs,'r'))
		except IOError:
			pass

		fileXML = open(configuredCleanUp,'r')
		xmlContent = fileXML.read()
		fileXML.close()
		dom = parseString(xmlContent)
		for node in dom.getElementsByTagName("setting"):
			attribute = node.getAttribute("id")
			value = node.getAttribute("value")
			config.setProperty(str(attribute), str(value))
		node = dom.getElementsByTagName("profile")
		if (node.length > 0):
			config.setProperty('cleanup_profile', '_' + str(node[0].getAttribute("name")))
			config.setProperty('cleanup_settings_version', str(node[0].getAttribute("version")))
		config.store(open(uiPrefs,'w'))
				