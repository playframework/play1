%{
        oldOut = _body.getProperty('out')
        sw = new StringWriter()
        _body.setProperty('out', new PrintWriter(sw))
        _body.call()
	play.templates.Template.layoutData.get().put(_arg, sw.toString())
        _body.setProperty('out', oldOut)
}%

