package org.sel.UnifiedBTS.Mantis.Model;

public class Category extends PropertyItem {
	/**
	 * @uml.property  name="parentID"
	 */
	public int ParentID;
	
	public Category(){
		super();
		ParentID = -1;
	}
	
	public Category(String _name, int _parentID){
		super(_name);
		ParentID = _parentID;
	}


	public Category(int _id, String _name, int _parentID){
		super(_id, _name);
		ParentID = _parentID;
	}
	
	public Category(int _id, String _name, String _desc, int _parentID){
		super(_id, _name, _desc);
		ParentID = _parentID;
	}
	

}
