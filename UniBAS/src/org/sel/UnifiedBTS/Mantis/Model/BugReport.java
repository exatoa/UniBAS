package org.sel.UnifiedBTS.Mantis.Model;

import java.util.ArrayList;
import java.util.List;

public class BugReport {
	
	public int ID;			
	public Project Project;
	public Category Category;
	
	public User Reporter;
	public User Handler;
	public int DuplicateID; 
	
	public String Priority;		
	public String Severity; 		
	public String Status;	
	public String Resolution;
	public String Reproducibility;

	public String OS; 				
	public String OS_Build;		
	public String Platform; 		
	public String Version;
	public String Build;
	public String TargetVersion;
	public String FixedInVersion;

	public String Summary;
	public String DateSubmitted; 	
	public String LastUpdated; 	
	public String DueDate;	
	//public int BugTextID;

	public int ViewState;
	public int Projection;
	public int ProfileID; 		
	public int ETA;
	public int SponsorshipTotal;
	public int Sticky;			
	public String Tags;
	public String Description;
	public String Steps;
	public String AdditionalInfo;
	

	/**
	 * @uml.property  name="attachments"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="org.sel.UnifiedBTS.DataType.Attachment"
	 */
	public List<Attachment> Attachments;
	/**
	 * @uml.property  name="cCList"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	public List<String> CCList;
	/**
	 * @uml.property  name="bugnotes"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="org.sel.UnifiedBTS.DataType.Bugnote"
	 */
	public List<Bugnote> Bugnotes;
	/**
	 * @uml.property  name="relationships"
	 */
	public List<Relationship> Relationships;
	
	public List<BugHistory> Histories;
	
	public List<BugChangeset> Changesets;
	
	
	
	public BugReport()
	{
		ID = -1;
		Project = null;
		Category = null;
		Build = null;
		
		Reporter = null;
		Handler = null;
		DuplicateID = 0;
		
		Priority = null;
		Severity = null;
		Status = null;
		Resolution = null;
		Reproducibility = null;
		
		OS = null;
		OS_Build = null;
		Platform = null;
		Version = null;		
		FixedInVersion = null;
		
		Tags = null;
		Summary = null;
		DateSubmitted = null;		//DateTime
		LastUpdated = null;			//DateTime
		ProfileID = -1;
		ViewState = -1;
		DueDate = null;				//DateTime

		ETA = 10;	
		SponsorshipTotal = 0;
		Sticky = 0;
		Projection = -1;


		Attachments = null;
		Bugnotes = null;
		Relationships = null;
		CCList =  null;
		Histories = null;
		Changesets = null;
	}



	public BugReport(int _id) {
		ID = _id;
		Project = null;
		Category = null;
		Build = null;
		
		Reporter = null;
		Handler = null;
		DuplicateID = 0;
		
		Priority = null;
		Severity = null;
		Status = null;
		Resolution = null;
		Reproducibility = null;
		
		OS = null;
		OS_Build = null;
		Platform = null;
		Version = null;		
		FixedInVersion = null;
		
		Tags = null;
		Summary = null;
		DateSubmitted = null;		//DateTime
		LastUpdated = null;			//DateTime
		ProfileID = -1;
		ViewState = -1;
		DueDate = null;				//DateTime

		ETA = 10;	
		SponsorshipTotal = 0;
		Sticky = 0;
		Projection = -1;


		Attachments = null;
		Bugnotes = null;
		Relationships = null;
		CCList =  null;
		Histories = null;
		Changesets = null;
	}
}


