package cn.bran.play.exceptions;

import cn.bran.japid.exceptions.JapidRuntimeException;

public class ReverseRouteException extends JapidRuntimeException{
	String action;

	public ReverseRouteException(String action) {
		super("Reverse Route Exception", "action not found: " + action);
		this.action = action;
	}
	
}
