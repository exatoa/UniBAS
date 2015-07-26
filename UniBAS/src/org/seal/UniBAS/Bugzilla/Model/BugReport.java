package org.seal.UniBAS.Bugzilla.Model;

import java.util.ArrayList;
import java.util.List;

public class BugReport {
	public int BugID;
	public String Alias;
	public String Status;
	public String Resolution;
	public PropertyItem Classification;
	public Product Product;
	public PropertyItem Component;
	public String Version;				
	public String Milestone;
	public String FixedInVersion;
	public String Priority;
	public String Severity;
	public String Reproducibility;
	public String Platform;
	public String OS;
	public String OSBuild;
	public User Reporter;
	public User Assignee;
	public User QA;
	public String BugFileLoc;
	public String Tags;
	public String Summary;
	public String StatusWhiteboard;
	public String CreationTime;			//DateTime
	public String UpdatedTime;			//DateTime
	public int Votes;
	public int ReporterAccessible;
	public int CClistAccessible;
	public int Everconfirmed;
	public String Lastdiffed;			//DateTime
	public String Deadline;				//DateTime
	public double RemainingTime;	
	public double EstimatedTime;
	public double ActualTime;

	public List<String> SeeAlsos;
	public List<Attachment> Attachments;
	public List<User> CCList;
	public List<Bugnote> Bugnotes;
	public List<Relationship> Relationships;
	
	
	public BugReport()
	{
		BugID = -1;
		Alias = null;
		Status = null;
		Resolution = null;
		Version = null;
		Milestone = null;
		FixedInVersion = null;
		Priority = null;
		Severity = null;
		Reproducibility = null;
		Platform = null;
		OS = null;
		OSBuild = null;
		BugFileLoc = null;
		Tags = null;
		Summary = null;
		StatusWhiteboard = null;
		CreationTime = null;			//DateTime
		UpdatedTime = null;			//DateTime
		Votes = -1;
		ReporterAccessible = -1;
		CClistAccessible = -1;
		Everconfirmed = -1;
		RemainingTime = 0.0;
		EstimatedTime = 0.0;
		ActualTime = 0.0;
		Lastdiffed = null;				//DateTime
		Deadline = null;				//DateTime


		Reporter = null;
		Assignee = null;
		QA = null;
		Classification = null;
		Product = null;
		Component = null;
		
		Attachments = new ArrayList<Attachment>();
		Bugnotes = new ArrayList<Bugnote>();
		Relationships = new ArrayList<Relationship>();
		SeeAlsos = new ArrayList<String>();
		CCList =  new ArrayList<User>();
	}
	

	public void PrintInfo()
	{
		System.out.println("["+BugID+"] "+CreationTime);
	}

}


