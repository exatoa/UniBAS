package org.seal.UniBAS.Bugzilla.Model;

public enum RelationshipType {
	None(0),
	DuplicateOf(1), 	// dupe - dupe_of
	HasDuplicate(2), 	// dupe_of - dupe
	ChildOf(3),			// blocked - dependson		
	ParentOf(4),		// dependson - blocked
	RelatedTo(5);		// src - dest
	

	/**
	 * @uml.property  name="value"
	 */
	private int value;
	private RelationshipType(int value) {
		this.value = value;
	}
	/**
	 * @return
	 * @uml.property  name="value"
	 */
	public int getValue() {
		return value;
	}
	
	/**
	 * 각 타입의 문자열 값을 반환.
	 * @return
	 */
	public String getString() {
		switch(this.value)
		{
		case 0 : return "None";
		case 1 : return "Duplicate Of";
		case 2 : return "Has Duplicate";
		case 3 : return "Child Of";
		case 4 : return "Parent Of";
		case 5 : return "Related To";
		}
		return "";
	}
}
