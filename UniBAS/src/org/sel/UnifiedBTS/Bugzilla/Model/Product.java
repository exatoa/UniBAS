package org.sel.UnifiedBTS.Bugzilla.Model;

public class Product extends PropertyItem {
	/**
	 * @uml.property  name="parentID"
	 */
	public int ParentID;
	
	public Product(){
		super();
		ParentID = -1;
	}
	
	public Product(String _name, int _parentID){
		super(_name);
		ParentID = _parentID;
	}


	public Product(int _id, String _name, int _parentID){
		super(_id, _name);
		ParentID = _parentID;
	}
	
	public Product(int _id, String _name, String _desc, int _parentID){
		super(_id, _name, _desc);
		ParentID = _parentID;
	}
	

}
