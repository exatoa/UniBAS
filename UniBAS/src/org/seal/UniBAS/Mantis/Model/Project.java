package org.seal.UniBAS.Mantis.Model;

public class Project extends PropertyItem {
	/**
	 * @uml.property  name="parentID"
	 */
	public int ParentID;
	
	public Project(){
		super();
		ParentID = -1;
	}
	
	public Project(String _name, int _parentID){
		super(_name);
		ParentID = _parentID;
	}


	public Project(int _id, String _name, int _parentID){
		super(_id, _name);
		ParentID = _parentID;
	}
	
	public Project(int _id, String _name, String _desc, int _parentID){
		super(_id, _name, _desc);
		ParentID = _parentID;
	}
	

}
