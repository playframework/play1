%{
	play.templates.Template.layout.set(
		play.templates.TemplateLoader.load(play.Play.getFile('app/views/'+_arg.toString()))
	);
}%