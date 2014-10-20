package org.sel.UnifiedBTS.Bugzilla.Model;

/**
 * 관계에 대핸 속성
 * @author Zeck
 *
 */
public class Relationship {
	/**
	 * @uml.property  name="srcID"
	 */
	public int SrcID;
	/**
	 * @uml.property  name="destID"
	 */
	public int DestID;
	/**
	 * @uml.property  name="type"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	public RelationshipType Type;
	
	
	public Relationship()
	{
		SrcID = -1;
		DestID = -1;
		Type = RelationshipType.None;		
	}
	
	public Relationship(int _srcID, int _destID, RelationshipType _type)
	{
		SrcID = _srcID;
		DestID = _destID;
		Type = _type;		
		
	}
}
