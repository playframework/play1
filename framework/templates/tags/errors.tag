*{ 
	_arg:	(optional) fieldname to filter errors, if not specified all 
errors are returned 
	_field:	(optional) fieldname to filter errors, if not specified all 
errors are returned 
}* 

%{ 
        _field = _arg ?: _field 
        if (! _field) { 
                validations = play.data.validation.Validation.errors() 
        } else { 
                validations = play.data.validation.Validation.errors(_field) 
        } 
        size = validations.size()
        validations.eachWithIndex() { item, i -> 
                attrs = [:] 
                attrs.put('error', item) 
                attrs.put('error_index', i+1) 
                attrs.put('error_isLast', (i+1) == size) 
                attrs.put('error_isFirst', i==0) 
                attrs.put('error_parity', (i+1)%2==0?'even':'odd') 
}% 
        #{doBody vars:attrs /} 
%{ 
        } 
}% 