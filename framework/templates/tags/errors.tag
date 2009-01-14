%{ play.data.validation.Validation.errors().eachWithIndex() { item, i -> }%
	%{
		attrs = [:]
		attrs.put('error', item)
		attrs.put('error_index', i+1)
		attrs.put('error_isLast', (i+1) == size)
		attrs.put('error_isFirst', i==0)
		attrs.put('error_parity', (i+1)%2==0?'even':'odd')
	}%
        #{doBody vars:attrs /}
%{ } }%