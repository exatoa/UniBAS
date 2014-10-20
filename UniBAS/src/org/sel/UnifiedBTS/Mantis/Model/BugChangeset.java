package org.sel.UnifiedBTS.Mantis.Model;

public class BugChangeset {
	/**
	 * @uml.property  name="who"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	public User Who;
	/**
	 * @uml.property  name="when"
	 * @uml.associationEnd  qualifier="h:org.sel.UnifiedBTS.DataType.BugHistory java.lang.String"
	 */
	public String When;
	/**
	 * @uml.property  name="fieldID"
	 */
	public String FieldID;
	/**
	 * @uml.property  name="removed"
	 */
	public String Removed;
	/**
	 * @uml.property  name="added"
	 */
	public String Added;
	/**
	 * @uml.property  name="attachID"
	 */
	public int AttachID;
	
	public BugChangeset()
	{
		Who = new User();
		When = null;
		FieldID = null;
		Removed = null;
		Added = null;
		AttachID = -1;
	}
	
	public BugChangeset(String _who, String _when, String _fieldID, String _removed, String _added, int _attachID)
	{
		Who = new User(-1, _who);
		When = _when;
		FieldID = _fieldID;
		Removed = _removed;
		Added = _added;
		AttachID = _attachID;
	}
	public BugChangeset(int _whoID, String _who,  String _when, String _fieldID, String _removed, String _added, int _attachID)
	{
		Who = new User(_whoID, _who);
		When = _when;
		FieldID = _fieldID;
		Removed = _removed;
		Added = _added;
		AttachID = _attachID;
	}
}
