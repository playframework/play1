Source code
 

- https://github.com/branaway/playclipse, branched from https://github.com/erwan/playclipse

 

Features:

 

- The plugin as of now offers the same level of features that are in the original plugin for the  groovy-based templates to the Japid template engine.

  - Commands to navigate between actions and views.

  - An enhanced Play HTML editor that recognizes some Japid syntax, notably the back single quotation mark syntax - the flagship Japid syntax.  

  - Ctrl-click navigation in html views to actions, layout templates and tags. 

 

- It also integrates the Japid template transforming process to the standard Eclipse project incremental and full building processes, thus eliminates any manual process in applying Japid templates while delivering the best possible performance.   

 

- It has also fixed a few bugs coming with the original plugin and enhanced the popup menu in  the views and editors. 

 

Installation:

 

- Just put the jar file in the dropins directory of the Eclipse installation and restart the IDE (with -clean command line option if you don't see the JapidPlay menu in the IDE workbench window menu bar). 

- Starting from version 0.6.0, the Japid module (http://www.playframework.org/modules/japid) bundles a distribution of this plugin in the eclipse plugin sub-directory in the module downloads. Please use the jar file in the bundle for best version match.

 


Note:

 

- The plugin has been tested with Eclipse Helios (3.6).
- Sometimes the -clean command line option is required for the Eclipse IDE to install the plugin.  
    

Usage:

 

- Right click on your Play project and enable the Play nature to your Play/Japid application in the Eclipse, or the Japid transformation will not be integrated with the project building process, neither the popup menu will display the proper menu items. 

History:

2010/12/21:
	1. work started. 
2010/12/27:		
	1. The "New controller" command can generate Japid controllers. 
	2. the "Go to view" command can smartly go to either the Groovy based views or Japid  views, depending on the context.
2011/1/1:
	1. added a few commands to the package explorer context menu.
	2. added context menu to go to action from a view editor.Works with Play html editor and  WST html editor.
	3. added Japid template transformation to the build process to automate the template  conversion from the html files to Java code.
	4. added comand in the editor context manu to switch between japid html and japid java  code.
	5. enhanced the tag, action and layout navigation via ctrl-click in views.

2011/1/5, version 0.8.2
	1. support ctrl-click on #{invoke package.controller.action /} in views.
2011/1/10, version 0.8.2.2
	1. added support for ctrl-click on `import xxx.yyy.ZZZ in views, also import static
	2. support ctrl-click on `invoke package.controller.action()  in views.
2011/1/11, v0.8.3.3
	1. now bundled the Japid 0.6 jars.
	2. derived java files are formatted.
2011/1/25, v0.8.3.4
	1. recognized .json, .xml, .txt file extensions as templates in code generation.
	2. matched Japid 0.6.1.
	3. added route file check in incremental buiding.
2011/2/15, v0.8.6
	1. matched Japid 0.6.2.
	2. put whatever error message during the code transform in the generated Java code to get the attention. 
2011/2/22, v0.8.7
	1. matched Japid 0.7.1.
2011/9/22, v0.8.9.7
	1. matched Japid 0.8.9.7
