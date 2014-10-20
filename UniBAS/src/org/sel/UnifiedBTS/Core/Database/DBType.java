package org.sel.UnifiedBTS.Core.Database;

//public static enum DBType {MYSQL, MSSQL};
public enum DBType {
	MYSQL(1),
	MSSQL(2);
   
	/**
	 * @uml.property  name="value"
	 */
	private int value;
	private DBType(int value) {
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