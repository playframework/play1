%{
    if(_delete == 'all') {
        play.test.Fixtures.deleteAll()
    } else if(_delete) {
        play.test.Fixtures.delete(_delete)
    }
}%

%{
    if(_load && _loadAsTemplate != null) {
        play.test.Fixtures.loadModels(_loadAsTemplate, _load)
    }else if(_load){
        play.test.Fixtures.loadModels(_load)
    }
}%

%{
    if(_arg && _arg instanceof String) {
        try {
            play.Play.classloader.loadClass(_arg).newInstance()
        } catch(Exception e) {
            throw new play.exceptions.TagInternalException('Cannot apply ' + _arg + ' fixture because of ' + e.getClass().getName() + ', ' + e.getMessage())
        }
    }
%}
