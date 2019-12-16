%{
    models = [];
    for(controllerClass in play.Play.classloader.getAssignableClasses(__loadClass('controllers.CRUD'))) {
        resourceModel = __loadClass('controllers.CRUD$ObjectType').get(controllerClass)
        if(resourceModel != null) {
            models.add(resourceModel)
        }
    }
    for(controllerClass in play.Play.classloader.getAssignableClasses(__loadClass('play.scalasupport.crud.CRUDWrapper'))) {
        resourceModel = __loadClass('controllers.CRUD$ObjectType').get(controllerClass)
        if(resourceModel != null) {
            models.add(resourceModel)
        }
    }
    java.util.Collections.sort(models)
}%

%{ models.eachWithIndex() { item, i -> }%
	%{
		attrs = [:]
		attrs.put('type', item)
	}%
    #{doBody vars:attrs /}
%{ } }%