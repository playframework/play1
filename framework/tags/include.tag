%{
	template = play.templates.TemplateLoader.load(_arg)
        args = [:]
        args.putAll(getBinding().getVariables())
        template.render(args)
}%