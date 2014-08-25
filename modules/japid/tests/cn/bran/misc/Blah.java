package cn.bran.misc;

public class Blah implements MyInterface{
	int c = 0;
	
	@Override
	public void doSomething(int i) {
		c += i;
	}

}
