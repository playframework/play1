package cn.bran.play;

import java.io.Serializable;

public interface RunAction extends Serializable{
	void runPlayAction() throws cn.bran.play.JapidResult;
}
