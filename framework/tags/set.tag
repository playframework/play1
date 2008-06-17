%{
        // Simple case : #{set title:'Yop' /}
        for(p in binding.getVariables().keySet()) {
            if(p.startsWith('_') && p != '_body') {
                play.templates.Template.layoutData.get().put(p.substring(1), binding.getVariables().get(p))
                return
            }
        }
        // Body case
        if(_arg && _body) {
            oldOut = _body.getProperty('out')
            sw = new StringWriter()
            _body.setProperty('out', new PrintWriter(sw))
            _body.call()
            play.templates.Template.layoutData.get().put(_arg, sw.toString())
            _body.setProperty('out', oldOut)
        }
}%

