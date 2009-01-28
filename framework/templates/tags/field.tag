%{
	def field = [:]
	field['name'] = _arg
        field['id'] = _arg.replace('.', '_')
	field['flash'] = play.mvc.Scope.Flash.current().get(_arg)
        field['flashArray'] = field['flash'] ? field['flash'].split(',') : []
	field['error'] = play.data.validation.Validation.error(_arg)
	field['errorClass'] = field['error'] ? 'hasError' : ''
}%
#{doBody vars:[field: field] /}