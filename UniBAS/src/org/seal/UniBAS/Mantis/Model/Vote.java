package org.seal.UniBAS.Mantis.Model;


public class Vote {
	/**
	 * @uml.property  name="voter"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	public User Voter;
	/**
	 * @uml.property  name="count"
	 */
	public int Count;
	
	public Vote(String _name, int _count)
	{
		Voter = new User(-1, _name);
		Count = _count;
	}
	public Vote(int _id, String _name, int _count)
	{
		Voter = new User(_id, _name);
		Count = _count;
	}
}
