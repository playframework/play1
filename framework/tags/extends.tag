%{
        if(_arg.startsWith('./')) {
            ct = play.templates.Template.currentTemplate.get().name
            ct = ct.substring(0, ct.lastIndexOf('/'))
            _arg = ct + _arg.substring(1);
        }
	play.templates.Template.layout.set(
		play.templates.TemplateLoader.load(_arg)
	);
}%