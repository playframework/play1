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