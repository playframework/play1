%{
    if(_delete == 'all') {
        play.test.Fixtures.deleteAll()
    } else if(_delete) {
        play.test.Fixtures.delete(_delete)
    }
}%

%{
    if(_load) {
        play.test.Fixtures.load(_load)
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