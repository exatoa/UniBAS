package org.sel.UnifiedBTS.Bugzilla.Model;

public class PropertyItem {
	/**
	 * @uml.property  name="iD"
	 */
	public int ID;
	/**
	 * @uml.property  name="name"
	 */
	public String Name;
	/**
	 * @uml.property  name="desc"
	 */
	public String Desc;
	
	
	public PropertyItem(int _id, String _name, String _desc)
	{
		ID = _id;
		Name = _name;
		Desc = _desc;
	}
	
	public PropertyItem(int _id, String _name)
	{
		ID = _id;
		Name = _name;
		Desc = null;
	}
	
	public PropertyItem(String _name)
	{
		ID = -1;
		Name = _name;
		Desc = null;
	}
	
	public PropertyItem()
	{
		ID = -1;
		Name = null;
		Desc = null;
	}
}
