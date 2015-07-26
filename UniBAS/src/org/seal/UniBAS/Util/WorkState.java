package org.seal.UniBAS.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class WorkState implements java.io.Serializable {
	/**
	 * serialVersionUID 자동할당됨.
	 */
	private static final long serialVersionUID = 7861753297375010136L;

	
	private int CurrentBugID = 0;
	private int LastBugID = 0;	

	private File file;


	public boolean setCurrentBugID(int _value) {
		
		CurrentBugID = _value;
		
		return writeObject();
	}
	
	public int getCurrentBugID() {
		return CurrentBugID;
	}
	
	public boolean setLastBugID(int _value) {
		
		LastBugID = _value;
		
		return writeObject();
	}
	
	public int getLastBugID() {
		return LastBugID;
	}
	
	//##########################################################################
	// Behaviors
	//##########################################################################
	private static WorkState Instance=null;
	public static WorkState getInstance(String _path)
	{
		if (Instance==null)
			Instance = new WorkState(_path);
		return Instance;
	}
	
	public static WorkState getInstance()
	{
		return Instance;		
	}

	/**
	 * Constructs an empty list with the specified initial capacity.
	 * @param initialCapacity : the initial capacity of the list
	 * @Throws java.lang.IllegalArgumentException if the specified initial capacity is negative
	 */
	private WorkState(String _path) {
		
		if (_path ==null)
			throw new IllegalArgumentException("Illegal _path");
		
		//변수 값 초기화.
		CurrentBugID = 0;
		LastBugID = 0;
		
		OriginalSize = -1;
		LastIndex = -1;
		WorkingDate = "";
					
		try {
			file = new File(_path);
			File parent = file.getParentFile();

			if(!parent.exists() && !parent.mkdirs()){
			    throw new IllegalStateException("Couldn't create dir: " + parent);
			}
			
			if(file.exists()==false)
			{
				writeObject();
			}			
			readObject();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Save the state of the ArrayList instance to a stream (that is, serialize it).
	 * SerialData:
	 * The length of the array backing the ArrayList instance is emitted (int),
	 * followed by all of its elements (each an Object) in the proper order.
 	 */
	public boolean writeObject()
	{
		try{
			ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(file));
	
			// Write out values.
			s.writeObject(CurrentBugID);
			s.writeObject(LastBugID);	
			
			s.writeObject(OriginalSize);	
			s.writeObject(LastIndex);	
			s.writeObject(WorkingDate);	
			
	
			s.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Reconstitute the ArrayList instance from a stream (that is, deserialize it).
 	 */
	public boolean readObject() throws java.io.IOException, ClassNotFoundException {
		// Read in size, and any hidden stuff
		//s.defaultReadObject();
		try{
			ObjectInputStream s = new ObjectInputStream(new FileInputStream(file));

			CurrentBugID = (Integer)s.readObject();
			LastBugID = (Integer)s.readObject();
			
			OriginalSize = (Integer)s.readObject();
			LastIndex = (Integer)s.readObject();
			WorkingDate = (String)s.readObject();
			
			
			s.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	private int OriginalSize = -1;
	private int LastIndex = -1;
	private String WorkingDate = "";
	
	
	public boolean setWorkingDate(String dateString) {
		WorkingDate = dateString;
		return writeObject();
		
	}
	
	public boolean setOriginalSize(int size) {
		OriginalSize = size;
		return writeObject();	
		
	}
	public boolean setLastIndex(int lastIdx) {
		LastIndex = lastIdx;
		return writeObject();		
	}
	public int getLastIndex() {
		return LastIndex;
	}
	public int getOriginalSize() {
		return OriginalSize;
	}
	public String getWorkingDate() {
		return WorkingDate;
	}






}