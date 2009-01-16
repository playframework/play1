%{
	def field = [:]
	field['name'] = _arg
        field['id'] = _arg.replace('.', '_')
	field['flash'] = flash[_arg]
	field['error'] = play.data.validation.Validation.error(_arg)?.message()
	field['errorClass'] = field['error'] ? 'hasError' : ''
}%
#{doBody vars:[field: field] /}