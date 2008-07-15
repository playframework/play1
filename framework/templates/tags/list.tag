%{
    if(!_as) {
        throw new play.exceptions.TagInternalException("as attribute cannot be empty");
    }
    if(_items == null) _items = []
    _start = _start ? _start : 0
    _end = _end ? _end : _items.size()
}%
%{ _items.eachWithIndex() { item, i -> }%
	%{
		attrs = [:]
		attrs.put(_as, item)
		attrs.put(_as+'_index', i+1)
		attrs.put(_as+'_isLast', (i+1)==size)
		attrs.put(_as+'_isFirst', i==0)
		attrs.put(_as+'_parity', (i+1)%2==0?'even':'odd')
	}%
        %{ if(i >= _start && i <= _end) { }%
            #{doBody vars:attrs /}
        %{ } }%
%{ } }%