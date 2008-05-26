%{ 
	if(_arg) { 
	args = [:]
	args['kiki'] = '%%%%%%%KIKI'
	play.Logger.info('> %s', args)
}%
	#{doBody vars:args /}
%{ 
	} 
}%