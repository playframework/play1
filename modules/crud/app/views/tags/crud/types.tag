%{
        models = [];
	for(controllerClass in play.Play.classloader.getAssignableClasses(controllers.CRUD.class)) {
		resourceModel = controllers.CRUD.ObjectType.get(controllerClass);
		if(resourceModel != null) {
			models.add(resourceModel);
		}
	}
}%

%{ models.eachWithIndex() { item, i -> }%
	%{
		attrs = [:]
		attrs.put('type', item)
	}%
    #{doBody vars:attrs /}
%{ } }%