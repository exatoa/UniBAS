package org.sel.UnifiedBTS.Util;

public enum LogType {
	Message(1), 	//일반메세지, 화면에만 출력
	Normal(2), 		//일반메세지, 화면.로그에 출력
	Warning(3),		//경고메세지, 화면.로그에 출력
	Error(4),		//에러메세지, 화면.로그에 출력
	Notice(5),		//일반메세지, 로그에도 남김.
	Logging(6);		//일반메세지, 로그에도 남김.

	/**
	 * @uml.property  name="value"
	 */
	private int value;
	private LogType(int value) {
		this.value = value;
	}
	/**
	 * @return
	 * @uml.property  name="value"
	 */
	public int getValue() {
		return value;
	}
}