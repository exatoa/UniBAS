--use master
--create database bugzilla_Test
--use bugzilla_landfill

--===================================================================================
--    작업 보조 함수.
--===================================================================================
CREATE FUNCTION getIntArray
(
	 @strItems		as NVARCHAR(MAX)
	,@delimiter		as NVARCHAR(2) = null
)
RETURNS @Items TABLE 
(
    Item	integer primary key NOT NULL
)
AS
BEGIN
	DECLARE @sp_len		as INTEGER
	DECLARE @len		as INTEGER
	DECLARE @s			as INTEGER
	DECLARE @e			as INTEGER
	DECLARE @str		as NVARCHAR(100)
	DECLARE @item		as INTEGER

	if @strItems IS NULL
		RETURN
	--작업을 위한 초기설정
	IF @delimiter is null					--기본 DELIMITER 지정
		SET @delimiter = ','
	
	SET @strItems = LTRIM(RTRIM(@strItems))

	--마지막에 delimiter가 없으면 추가.
	IF CHARINDEX (@delimiter, @strItems, LEN(@strItems)) <=0
		SET @strItems = @strItems + @delimiter		--마지막에 DELIMITER 추가

	SELECT @sp_len = LEN(@delimiter)		--DELIMITER 길이
	SET @s = 1								--처음 시작위치지정

	WHILE (1=1)
	BEGIN
		--delimiter 검색
		SELECT @e = CHARINDEX(@delimiter, @strItems, @s)
		IF @e <= 0 BREAK

		--@s, @e사이의 문자열 잘라냄.
		SET @str = LTRIM(RTRIM(SUBSTRING(@strItems,@s,@e-@s)))
		IF len(@str)>0
		BEGIN
			SET @item = CONVERT(INTEGER, @str)
		
			IF NOT EXISTS (SELECT Item FROM @Items Where Item = @item)
				INSERT INTO @Items VALUES (@item)
		END

		SET @s = @e+@sp_len
	END

	RETURN 
END;;


--###################################################################################################
--# Bug Manager 테이블
--###################################################################################################
CREATE TABLE list_manager
(
	 id				int				identity(1,1)	NOT NULL
	,[type]			int				NOT NULL		--0 : 날짜로 수집된 리스트.
	,[date]			datetime		NOT NULL		--type==0이면 수집을 한 날짜, type==1 :: 연관된 날짜
	,bug_id			int								--관련 버그 아이디
	,progress		NVARCHAR(20)		NOT NULL		--상태유형 : REQUESTED, DOWNLOADED, SAVED, DONE (?)
	,[path]			NVARCHAR(512)		
	,url			NVARCHAR(400)	
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX list_manager_url_idx		ON list_manager(url);;



CREATE TABLE bugs_manager
(
	 bug_id			int				NOT NULL
	,list_id		int				NOT NULL
	,progress		NVARCHAR(20)		NOT NULL		--상태유형 : PENDING
													--			 BUG_REQUEST, BUG_DOWNLOADED, BUG_PARSED, BUG_SAVED
													--			 HIST_REQUEST, HIST_DOWNLOADED, HIST_PARSED, HIST_SAVED
													--			 VOTE_REQUEST, VOTE_DOWNLOADED, VOTE_PARSED, VOTE_SAVED
													--			 DONE
	,bug_url		NVARCHAR(512)		
	,bug_path		NVARCHAR(512)		

	,history_url	NVARCHAR(512)
	,history_path	NVARCHAR(512)

	,vote_url		NVARCHAR(512)
	,vote_path		NVARCHAR(512)	
	,Primary KEY (bug_id)
);;
CREATE NONCLUSTERED INDEX bugs_manager_bug_id_progress_idx		ON bugs_manager(bug_id, progress);;




--###################################################################################################
--# Bug 관련 테이블들.
--###################################################################################################

CREATE TABLE bugs
(
	 id						int									NOT NULL		-- 본 모델에는 identity(1,1)이지만, 서버에서 ID값을 확인할수 있으므로 제외. a unique ID.
	,alias					NVARCHAR(20)											--An alias for the bug which can be used instead of the bug number.
	,bug_status				NVARCHAR(64)		DEFAULT ('')	NOT NULL		--The workflow status of the bug. foreign key bug_status.value.
	,resolution				NVARCHAR(64)		DEFAULT ('')	NOT NULL		--The bug's resolution foreign key resolution.value.
	,product_id				smallint			DEFAULT (0)     NOT NULL		--The product (foreign key products.id)
	,component_id			smallint			DEFAULT (0)     NOT NULL		--The product component (foreign key components.id)
	,[version]				NVARCHAR(64)		DEFAULT ('')	NOT NULL		--The product version (foreign key versions.value)
	,target_milestone		NVARCHAR(20)							NOT NULL		--The milestone by which this bug should be resolved. (foreign key milestones.value)
	,[priority]				NVARCHAR(64)		DEFAULT ('')	NOT NULL		--The priority of the bug. foreign key priority.value.
	,bug_severity			NVARCHAR(64)		DEFAULT ('')	NOT NULL		--See the notes. foreign key bug_severity.value.
	,votes					int					DEFAULT (0)     NOT NULL		--The number of votes.
	,rep_platform			NVARCHAR(64)		DEFAULT ('')	NOT NULL		--The platform on which the bug was reported. foreign key rep_platform.value.
	,op_sys					NVARCHAR(64)		DEFAULT ('')	NOT NULL		--The operating system on which the bug was observed. foreign key op_sys.value.
	,reporter				int					DEFAULT (0)     NOT NULL		--The user who reported this (foreign key profiles.userid)
	,assigned_to			int					DEFAULT (0)     NOT NULL		--The current owner of the bug (foreign key profiles.userid).
	,qa_contact				int													--The QA contact (foreign key profiles.userid)
	,bug_file_loc			text												--A URL which points to more information about the bug.
	,keywords				text				DEFAULT ('')	NOT NULL		--A set of keywords. Note that this duplicates the information in the keywords table. (foreign key keyworddefs.name)
	,short_desc				NVARCHAR(255)		DEFAULT ('')	NOT NULL		--A short description of the bug.
	,status_whiteboard		text				DEFAULT ('')	NOT NULL		--This seems to be just a small whiteboard field.
	,creation_ts			datetime							NOT NULL		--The times of the bug's creation.
	,delta_ts				datetime			DEFAULT ('0000-00-00 00:00:00')     NOT NULL     --The timestamp of the last update. This includes updates to some related tables (e.g. the longdescs table).
	,reporter_accessible	tinyint				DEFAULT (1)     NOT NULL		--1 if the reporter can see this bug (even if in the wrong group), 0 otherwise.
	,cclist_accessible		tinyint				DEFAULT (1)     NOT NULL		--1 if people on the CC list can see this bug (even if in the wrong group), 0 otherwise.
	,everconfirmed			tinyint				DEFAULT (0)     NOT NULL		--1 if this bug has ever been confirmed. This is used for validation of some sort.
	,lastdiffed				datetime											--The time at which information about this bug changing was last emailed to the cc list.		
	,deadline				datetime											--The deadline for this bug (a date).
	,remaining_time			decimal(10,2)		DEFAULT (0)     NOT NULL		--The current estimate of the remaining effort required to fix this bug (in hours).
	,estimated_time			decimal(10,2)		DEFAULT (0)     NOT NULL		--The original estimate of the total effort required to fix this bug (in hours).
	,actual_time			decimal(10,2)		DEFAULT (0)		NOT NULL
	
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX bugs_assigned_to_idx		ON bugs(assigned_to);;
CREATE NONCLUSTERED INDEX bugs_bug_severity_idx		ON bugs(bug_severity);;
CREATE NONCLUSTERED INDEX bugs_bug_status_idx		ON bugs(bug_status);;
CREATE NONCLUSTERED INDEX bugs_component_id_idx		ON bugs(component_id);;
CREATE NONCLUSTERED INDEX bugs_creation_ts_idx		ON bugs(creation_ts);;
CREATE NONCLUSTERED INDEX bugs_delta_ts_idx			ON bugs(delta_ts);;
CREATE NONCLUSTERED INDEX bugs_op_sys_idx			ON bugs(op_sys);;
CREATE NONCLUSTERED INDEX bugs_priority_idx			ON bugs([priority]);;
CREATE NONCLUSTERED INDEX bugs_product_id_idx		ON bugs(product_id);;
CREATE NONCLUSTERED INDEX bugs_qa_contact_idx		ON bugs(qa_contact);;
CREATE NONCLUSTERED INDEX bugs_reporter_idx			ON bugs(reporter);;
CREATE NONCLUSTERED INDEX bugs_resolution_idx		ON bugs(resolution);;
CREATE NONCLUSTERED INDEX bugs_target_milestone_idx ON bugs(target_milestone);;
CREATE NONCLUSTERED INDEX bugs_version_idx			ON bugs([version]);;
CREATE NONCLUSTERED INDEX bugs_votes_idx			ON bugs(votes);;
CREATE NONCLUSTERED INDEX bugs_alias_idx			ON bugs(alias);;


CREATE TABLE bugs_activity
(
	 bug_id			int			DEFAULT (0)     NOT NULL		--Which bug (foreign key bugs.bug_id)
	,who			int			DEFAULT (0)     NOT NULL		--Which user (foreign key profiles.userid)
	,bug_when		datetime	DEFAULT ('0000-00-00 00:00:00')     NOT NULL     --When was the change made?
	,fieldid		NVARCHAR(64) DEFAULT ('')     NOT NULL		--What was the fieldid? (foreign key fielddefs.id)
	,removed		text										--The old value of this field, or values which have been removed for multi-value fields such as bugs.keywords, the cc table, and the dependencies table
	,added			text										--The new value of this field, or values which have been added for multi-value fields such as bugs.keywords, the cc table, and the dependencies table
	,attach_id		int											--If the change was to an attachment, the ID of the attachment (foreign key attachments.attach_id)
);;
CREATE NONCLUSTERED INDEX bugs_activity_bug_id_idx		ON bugs_activity(bug_id);;
CREATE NONCLUSTERED INDEX bugs_activity_bug_when_idx	ON bugs_activity(bug_when);;
CREATE NONCLUSTERED INDEX bugs_activity_who_idx			ON bugs_activity(who);;
CREATE NONCLUSTERED INDEX bugs_activity_fieldid_idx		ON bugs_activity(fieldid);;
CREATE NONCLUSTERED INDEX bugs_activity_id_when_field_idx	ON bugs_activity(bug_id, bug_when, fieldid);;


--###################################################################################################
--# Bug 관련 테이블들  (부가정보테이블)
--###################################################################################################

CREATE TABLE attachments
(
	 attach_id			int				NOT NULL							-- 본 모델에는 identity(1,1)이지만, 서버에서 ID값을 확인할수 있으므로 제외. a unique ID.
	,bug_id				int				DEFAULT (0)			NOT NULL		--the bug to which this is attached (foreign key bugs.bug_id)
	,creation_ts		datetime		DEFAULT ('0000-00-00 00:00:00')     NOT NULL     --the creation time.
	,[description]		text			DEFAULT ('')		NOT NULL		--a description of the attachment.
	,[filename]			NVARCHAR(100)	DEFAULT ('')		NOT NULL		--the filename of the attachment.
	,isobsolete			tinyint			DEFAULT (0)			NOT NULL		--Non-zero if this attachment is marked as obsolete.
	,ispatch			tinyint												--non-zero if this attachment is a patch file.
	,isprivate			tinyint			DEFAULT (0)			NOT NULL		--Non-zero if this attachment is "private", i.e. only visible to members of the "insider" group.
	,isurl				tinyint			DEFAULT (0)			NOT NULL		--Non-zero if this attachment is actually a URL.
	,filesize			integer								NOT NULL
	,mimetype			NVARCHAR(100)	DEFAULT ('')		NOT NULL		--the MIME type of the attachment.
	,modification_time	datetime		DEFAULT ('0000-00-00 00:00:00')     NOT NULL     --the modification time of the attachment.
	,submitter_id		int				DEFAULT (0)			NOT NULL		--the userid of the attachment (foreign key profiles.userid)
	,PRIMARY KEY (attach_id)
);;
CREATE NONCLUSTERED INDEX attachments_modification_time_idx ON attachments(modification_time);;
CREATE NONCLUSTERED INDEX attachments_submitter_id_idx ON attachments(submitter_id, bug_id);;
CREATE NONCLUSTERED INDEX attachments_bug_id_idx ON attachments(bug_id);;
CREATE NONCLUSTERED INDEX attachments_creation_ts_idx ON attachments(creation_ts);;

CREATE TABLE attach_data
(
	id			int				DEFAULT (0)     NOT NULL     --The attachment id (foreign key attachments.attach_id).
	,thedata	VARbinary(max)					NOT NULL     --the content of the attachment.
	,PRIMARY KEY (id)
);;



---------------------------------------------
-- 첨부파일 관련 flags
---------------------------------------------
CREATE TABLE flags
(
	 id					int              --A unique ID.
	,attach_id			int               --The attachment, or NULL if this flag is not on an attachment. (foreign key attachments.attach_id)
	,bug_id				int     DEFAULT (0)     NOT NULL     --The bug. (foreign key bugs.bug_id)
	,[type_id]			smallint     DEFAULT (0)     NOT NULL     --The flag type. (foreign key flagtypes.id)
	,requestee_id		int               --The ID of the user to whom this request flag is addressed, or NULL for non-requestee flags (foreign key profiles.userid)
	,setter				varchar(128)               --The ID of the user who created, or most recently modified, this flag (foreign key profiles.userid)
	,[status]			char(1)     DEFAULT ('')     NOT NULL     --'+' (granted), '-' (denied), or '?' (requested).
	,creation_date		datetime     DEFAULT ('0000-00-00 00:00:00')     NOT NULL     --The date the flag was created.
	,modification_date	datetime               --The date the flag was most recently modified or created.
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX flags_bug_id_idx ON flags(bug_id, attach_id);;
CREATE NONCLUSTERED INDEX flags_type_id_idx ON flags([type_id]);;
CREATE NONCLUSTERED INDEX flags_requestee_id_idx ON flags(requestee_id);;
CREATE NONCLUSTERED INDEX flags_setter_id_idx ON flags(setter);;

---------------------------------------------
--  flags 종류에 관한 정의
---------------------------------------------
CREATE TABLE flagtypes
(
	 id     smallint							    --The flag type ID
	,name     varchar(50)     DEFAULT ('')     NOT NULL     --The short flag name
	,sortkey     smallint     DEFAULT (0)     NOT NULL     --An integer used for sorting flags for display.
	,[description]     text     DEFAULT ('')     NOT NULL     --The description of the flag
	,target_type     char(1)     DEFAULT ('b')     NOT NULL     --'a' for attachment flags, 'b' for bug flags
	
	--,cc_list     varchar(200)               --A string containing email addresses to which notification of requests for this flag should be sent. This is filtered using the groups system before messages are actually sent, so that users not entitled to see a bug don't receive notifications concerning it.
	--,grant_group_id     int               --Group membership required to grant this flag. (foreign key groups.id)
	--,is_active     tinyint     DEFAULT (1)     NOT NULL     --1 if the flag appears in the UI and can be set; 0 otherwise.
	--,is_multiplicable     tinyint     DEFAULT (0)     NOT NULL     --1 if multiple instances of this flag may be set on the same item; 0 otherwise.
	--,is_requestable     tinyint     DEFAULT (0)     NOT NULL     --1 if the flag may be requested; 0 otherwise.
	--,is_requesteeble     tinyint     DEFAULT (0)     NOT NULL     --1 if a request for this flag may be aimed at a particular user; 0 otherwise.
	--,request_group_id     int               --Group membership required to request this flag. (foreign key groups.id)
	,PRIMARY KEY (id)
);;


CREATE TABLE bug_see_also
(
	 bug_id     int				DEFAULT (0)     NOT NULL      --The bug id, (foreign key bugs.bug_id)
	,value		NVARCHAR(255)    DEFAULT ('')     NOT NULL     --The URL of a related bug in another Bugzilla.
	
);;
CREATE UNIQUE NONCLUSTERED INDEX bug_see_also_bug_id_idx ON bug_see_also(bug_id, value);;

CREATE TABLE cc
(
	bug_id     int     DEFAULT (0)     NOT NULL     --The bug (foreign key bugs.bug_id)
	,who     int     DEFAULT (0)     NOT NULL		--The user (foreign key profiles.userid)
);;
CREATE UNIQUE NONCLUSTERED INDEX cc_bug_id_idx ON cc(bug_id, who);;
CREATE NONCLUSTERED INDEX cc_who_idx ON cc(who);;


CREATE TABLE votes
(
	 bug_id			int			DEFAULT (0)     NOT NULL     --The bug (foreign key bugs.bug_id)
	,who			int			DEFAULT (0)     NOT NULL     --The user (foreign key profiles.userid)
	,vote_count     smallint	DEFAULT (0)     NOT NULL     --How many votes.
);;
CREATE NONCLUSTERED INDEX votes_bug_id_idx ON votes(bug_id);;
CREATE NONCLUSTERED INDEX votes_who_idx ON votes(who);;

CREATE TABLE keywords
(
	 bug_id		int			DEFAULT (0)     NOT NULL     --The bug (foreign key bugs.bug_id)
	,keywordid	smallint	DEFAULT (0)     NOT NULL     --The keyword ID (foreign key keyworddefs.id)
);;
CREATE UNIQUE NONCLUSTERED INDEX keywords_bug_id_idx ON keywords(bug_id, keywordid);;
CREATE NONCLUSTERED INDEX keywords_keywordid_idx ON keywords(keywordid);;

CREATE TABLE keyworddefs
(
	
	 id				smallint			identity(1,1)	--A unique number identifying this keyword.
	,name			NVARCHAR(64)		DEFAULT ('')	NOT NULL		--The keyword itself.
	,description	text											--The meaning of the keyword.
	,PRIMARY KEY (id)
);;
CREATE UNIQUE NONCLUSTERED INDEX keyworddefs_name_idx ON keyworddefs(name);;



create TABLE longdescs
(
	 comment_id		int							NOT NULL					--A unique ID for this comment.
	,[type]			smallint	DEFAULT (0)     NOT NULL		--The type of a comment, used to identify and localize the text of comments which are automatically added by Bugzilla. 0 for a normal comment. 1 for a comment marking this bug as a duplicate of another. 2 for a comment marking another bug as a duplicate of this. 3 for a comment recording a transition to NEW by voting. 4 for a comment recording that this bug has been moved.
	,bug_id			int			DEFAULT (0)     NOT NULL		--the bug (foreign key bugs.bug_id)
	,who			int			DEFAULT (0)     NOT NULL		--the user who added this text (foreign key profiles.userid)
	,bug_when		datetime	DEFAULT ('0000-00-00 00:00:00')     NOT NULL     --when the text was added
	,thetext		text		DEFAULT ('')     NOT NULL		--the text itself.
	,extra_data		NVARCHAR(255)								--Used in conjunction with longdescs.type to provide the variable data in localized text of an automatic comment. For instance, a duplicate bug number.
	,isprivate		tinyint     DEFAULT (0)     NOT NULL		--Non-zero if this comment is "private", i.e. only visible to members of the "insider" group.
	--,work_time     decimal(5,2)     DEFAULT (0)     NOT NULL  --Number of hours worked on this bug (for time tracking purposes).
	--already_wrapped     tinyint     DEFAULT (0)     NOT NULL  --Non-zero if this comment is word-wrapped in the database (and so should not be wrapped for display).
	,PRIMARY KEY (comment_id)
);;
CREATE NONCLUSTERED INDEX longdescs_bug_id_idx		ON longdescs(bug_id);;
CREATE NONCLUSTERED INDEX longdescs_bug_when_idx	ON longdescs(bug_when);;
CREATE NONCLUSTERED INDEX longdescs_who_idx			ON longdescs(who, bug_id);;



--###################################################################################################
--# Bug 관련 테이블들 (관계테이블)
--###################################################################################################


CREATE TABLE status_workflow
(
	 new_status			smallint		DEFAULT (0)     NOT NULL    --The new bug status (foreign key bug_status.id)
	,old_status			smallint									--The old bug status, None for bug creation (foreign key bug_status.id)
	,require_comment	tinyint			DEFAULT (0)     NOT NULL    --1 if this transition requires a comment, 0 otherwise.
);;
CREATE UNIQUE NONCLUSTERED INDEX status_workflow_idx ON status_workflow(old_status, new_status);;



CREATE TABLE dependencies
(
	 blocked		int     DEFAULT (0)     NOT NULL     --Which bug is blocked (foreign key bugs.bug_id)
	,dependson		int     DEFAULT (0)     NOT NULL     --Which bug does it depend on (foreign key bugs.bug_id)
	
);;
CREATE UNIQUE NONCLUSTERED INDEX dependencies_blocked_dependson_idx		ON dependencies(blocked, dependson);;
CREATE NONCLUSTERED INDEX dependencies_blocked_idx		ON dependencies(blocked);;
CREATE NONCLUSTERED INDEX dependencies_dependson_idx	ON dependencies(dependson);;

CREATE TABLE duplicates
(
	 dupe		 int     DEFAULT (0)     NOT NULL     --The duplicate bug (foreign key bugs.bug_id)
	,dupe_of     int     DEFAULT (0)     NOT NULL     --The bug which is duplicated (foreign key bugs.bug_id)
	,PRIMARY KEY (dupe)
);;

CREATE TABLE relationships
(
	 src_id		 int     DEFAULT (0)     NOT NULL     --The bug report (foreign key bugs.bug_id)
	,dest_id     int     DEFAULT (0)     NOT NULL     --The bug which is related to (foreign key bugs.bug_id)
);;
CREATE UNIQUE NONCLUSTERED INDEX relationships_src_dest_idx		ON relationships(src_id, dest_id);;
CREATE NONCLUSTERED INDEX relationships_src_idx		ON relationships(src_id);;
CREATE NONCLUSTERED INDEX relationships_dest_idx	ON relationships(dest_id);;


--###################################################################################################
--# Bug 관련 테이블들 (상태정의)
--###################################################################################################

CREATE TABLE bug_severity
(
	 id						smallint		identity(1,1)					--a unique ID.
	,sortkey				smallint		DEFAULT (0)     NOT NULL		--A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT ('')    NOT NULL		--A possible value of the field
	,isactive				tinyint			DEFAULT (1)     NOT NULL		--1 if this value is available in the user interface, 0 otherwise
	,visibility_value_id	smallint										--If set, this value is only available if the chooser field (identified by fielddefs.value_field_id) has the value with this ID. Foreign key <field>.id, for example products.id or cf_<field>.id.
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX bug_severity_sortkey_idx ON bug_severity(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX bug_severity_value_idx ON bug_severity(value);;
CREATE NONCLUSTERED INDEX bug_severity_visibility_value_id_idx ON bug_severity(visibility_value_id);;


CREATE TABLE bug_status
(
	id						smallint		identity(1,1)     --a unique ID.
	,sortkey				smallint		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT ('')     NOT NULL     --A possible value of the field
	,is_open				tinyint			DEFAULT (1)     NOT NULL     --1 if the status is "Open", 0 if it is "Closed".
	,isactive				tinyint			DEFAULT (1)     NOT NULL     --1 if this value is available in the user interface, 0 otherwise
	,visibility_value_id	smallint               --If set, this value is only available if the chooser field (identified by fielddefs.value_field_id) has the value with this ID. Foreign key <field>.id, for example products.id or cf_<field>.id.
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX			bug_status_sortkey_idx				ON bug_status(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX	bug_status_value_idx				ON bug_status(value);;
CREATE NONCLUSTERED INDEX			bug_status_visibility_value_id_idx	ON bug_status(visibility_value_id);;

CREATE TABLE op_sys
(
	id						smallint		identity(1,1)     --a unique ID.
	,sortkey				smallint		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT ('')     NOT NULL     --A possible value of the field
	,isactive				tinyint			DEFAULT (1)     NOT NULL     --1 if this value is available in the user interface, 0 otherwise
	,visibility_value_id	smallint               --If set, this value is only available if the chooser field (identified by fielddefs.value_field_id) has the value with this ID. Foreign key <field>.id, for example products.id or cf_<field>.id.
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX				op_sys_sortkey_idx				ON op_sys(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX		op_sys_value_idx				ON op_sys(value);;
CREATE NONCLUSTERED INDEX				op_sys_visibility_value_id_idx ON op_sys(visibility_value_id);;

CREATE TABLE [priority]
(
	id						smallint		identity(1,1)     --a unique ID.
	,sortkey				smallint		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT ('')     NOT NULL     --A possible value of the field
	,isactive				tinyint			DEFAULT (1)     NOT NULL     --1 if this value is available in the user interface, 0 otherwise
	,visibility_value_id	smallint               --If set, this value is only available if the chooser field (identified by fielddefs.value_field_id) has the value with this ID. Foreign key <field>.id, for example products.id or cf_<field>.id.
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX priority_sortkey_idx ON [priority](sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX priority_value_idx ON [priority](value);;
CREATE NONCLUSTERED INDEX priority_visibility_value_id_idx ON [priority](visibility_value_id);;

CREATE TABLE rep_platform
(
	id						smallint		identity(1,1)     --a unique ID.
	,sortkey				smallint		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT ('')     NOT NULL     --A possible value of the field
	,isactive				tinyint			DEFAULT (1)     NOT NULL     --1 if this value is available in the user interface, 0 otherwise
	,visibility_value_id	smallint               --If set, this value is only available if the chooser field (identified by fielddefs.value_field_id) has the value with this ID. Foreign key <field>.id, for example products.id or cf_<field>.id.
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX rep_platform_sortkey_idx ON rep_platform(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX rep_platform_value_idx ON rep_platform(value);;
CREATE NONCLUSTERED INDEX rep_platform_visibility_value_id_idx ON rep_platform(visibility_value_id);;


CREATE TABLE resolution
(
	id						smallint		identity(1,1)     --a unique ID.
	,sortkey				smallint		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT ('')     NOT NULL     --A possible value of the field
	,isactive				tinyint			DEFAULT (1)     NOT NULL     --1 if this value is available in the user interface, 0 otherwise
	,visibility_value_id	smallint               --If set, this value is only available if the chooser field (identified by fielddefs.value_field_id) has the value with this ID. Foreign key <field>.id, for example products.id or cf_<field>.id.
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX resolution_sortkey_idx ON resolution(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX resolution_value_idx ON resolution(value);;
CREATE NONCLUSTERED INDEX resolution_visibility_value_id_idx ON resolution(visibility_value_id);;







--###################################################################################################
--# Product 관련 테이블들.
--###################################################################################################
CREATE TABLE classifications
(
	 id				smallint		NOT NULL					--The classification id.
	,sortkey		smallint		DEFAULT (0)     NOT NULL		--A number used to determine the order in which classifications are shown.
	,name			NVARCHAR(64)     DEFAULT ('')     NOT NULL		--The classification name.
	,[description]	text											--A description of the classification
	,PRIMARY KEY (id)
);;
CREATE UNIQUE NONCLUSTERED INDEX classifications_name_idx ON classifications(name);;
INSERT INTO classifications VALUES (1, 1, 'Unclassified', 'Basic class');;


CREATE TABLE products
(
	 id						smallint					identity(1,1)   --The product ID.
	,name					NVARCHAR(64)	DEFAULT ('')    NOT NULL		--The product name.
	,classification_id		smallint    DEFAULT (1)     NOT NULL		--The classification ID (foreign key classifications.id).
	,defaultmilestone		NVARCHAR(20)					NOT NULL		--The default milestone for a new bug (foreign key milestones.value)
	,[description]			text										--The description of the product
	,disallownew			tinyint     DEFAULT (0)     NOT NULL		--New bugs can only be created for this product if this is 0.	
	,maxvotesperbug			smallint    DEFAULT (10000)	NOT NULL		--Maximum number of votes which a bug may have.
	,milestoneurl			text		DEFAULT ('')    NOT NULL		--The URL of a document describing the product milestones.
	,votesperuser			smallint    DEFAULT (0)     NOT NULL		--Total votes which a single user has for bugs of this product.
	,votestoconfirm			smallint    DEFAULT (0)     NOT NULL		--How many votes are required for this bug to become NEW.
	,PRIMARY KEY (id)
);;
CREATE UNIQUE NONCLUSTERED INDEX products_name_idx ON products(name);;

CREATE TABLE components
(
	 id					smallint					identity(1,1)   --The component id.
	,product_id			smallint    DEFAULT (0)     NOT NULL		--The product (foreign key products.id)
	,name				NVARCHAR(64)	DEFAULT ('')    NOT NULL		--The component id.
	,description		text		DEFAULT ('')    NOT NULL		--A description of the component.
	,initialowner		int			DEFAULT (0)     NOT NULL		--The default initial owner of bugs in this component. On component creation, this is set to the user who creates the component. foreign key profiles.userid.
	,initialqacontact	int											--The initial "qa_contact" field for bugs of this component. Note that the use of the qa_contact field is optional, parameterized by Param("useqacontact"). foreign key profiles.userid.
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX			components_name_idx			ON components(name);;
CREATE UNIQUE NONCLUSTERED INDEX	components_product_id_idx	ON components(product_id, name);;



CREATE TABLE milestones
(
	 id				int				identity(1,1)				--A unique numeric ID
	,sortkey		smallint		DEFAULT (0)     NOT NULL    --A number used for sorting milestones for a given product.
	,product_id     smallint		DEFAULT (0)     NOT NULL    --The product (foreign key products.id)	
	,value			NVARCHAR(20)		DEFAULT ('')     NOT NULL   --The name of the milestone (e.g. "3.1 RTM", "0.1.37", "tweakfor BigCustomer", etc).
	,PRIMARY KEY (id)
);;
CREATE UNIQUE NONCLUSTERED INDEX milestones_product_id_idx ON milestones(product_id, value);;

CREATE TABLE versions
(
	 id				int							identity(1,1)   --A unique numeric ID
	,product_id     smallint     DEFAULT (0)    NOT NULL		--The product (foreign key products.id)
	,value			NVARCHAR(64)  DEFAULT ('')   NOT NULL		--The name of the version
	,PRIMARY KEY (id)
);;
CREATE UNIQUE NONCLUSTERED INDEX versions_product_id_idx ON versions(product_id, value);;






--###################################################################################################
--# User 관련 테이블들.
--###################################################################################################


CREATE TABLE profiles
(
	 id				int								identity(1,1)  --A unique identifier for the user. Used in other tables to identify this user.
	,login_name     NVARCHAR(255)     DEFAULT (N'')     NOT NULL     --The user's email address. Used when logging in or providing mailto: links.
	,realname		NVARCHAR(255)     DEFAULT (N'')     NOT NULL     --The user's real name.
	,timezone		NVARCHAR(10)		DEFAULT(N'UTC')
	,create_ts		datetime
	,delta_ts		datetime
	,PRIMARY KEY (id)
);;
CREATE UNIQUE NONCLUSTERED INDEX profiles_login_name_idx ON profiles(login_name);;

INSERT INTO profiles	VALUES (N'Administrator', N'Administrator',N'UTC',null,null);;


--###################################################################################################
--# 기타
--###################################################################################################

CREATE TABLE component_cc
(
	component_id     smallint     DEFAULT (0)     NOT NULL     --The component id (foreign key components.id).
	,[user_id]     int     DEFAULT (0)     NOT NULL     --The user id (foreign key profiles.userid).
	
);;
CREATE UNIQUE NONCLUSTERED INDEX component_cc_user_id_idx ON component_cc(component_id, [user_id]);;



CREATE TABLE bugs_fulltext
(
	 bug_id					int				DEFAULT (0)     NOT NULL		--Which bug (foreign key bugs.bug_id)
	,comments				text											--The bug's comments, concatenated (longdescs.thetext)
	,comments_noprivate     text											--Those comments visible to non-members of the "insider" group (i.e. with longdescs.isprivate zero).
	,short_desc				NVARCHAR(255)     DEFAULT ('')   NOT NULL		--The bug's short description (bugs.short_desc)
	,PRIMARY KEY (bug_id)
);;
CREATE NONCLUSTERED INDEX bugs_fulltext_short_desc_idx ON bugs_fulltext(short_desc);;



--######################################################################################
--######################################################################################
-- Procedure 정의
--######################################################################################
--######################################################################################

----------------------------------------------------------------
-- BugID 존재하는지 확인
----------------------------------------------------------------
CREATE PROCEDURE checkStateBugID
(	 
	 @id		as integer			= null
)
as
BEGIN
	--기존에 존재하는 버그리포트 아이디인지 확인.
	IF EXISTS (SELECT * FROM bugs WHERE id = @id)
		RETURN 1
	ELSE RETURN -1
END;;



----------------------------------------------------------------
-- 버그아이디 리스에서 존재하는 마지막 버그위치 찾기
----------------------------------------------------------------
CREATE PROCEDURE checkLastBugID
(	 
	  @strID		as NVARCHAR(max)
	 ,@delimiter	as NVARCHAR(4) = null
)
as
BEGIN

	DECLARE @s INTEGER
	DECLARE @e INTEGER
	DECLARE @sp_len INTEGER
	DECLARE @str NVARCHAR(64)
	DECLARE @bug_id INTEGER
	DECLARE @last_id INTEGER

		
	--작업을 위한 초기설정
	IF @delimiter is null					--DELIMITER 문자 지정
		SET @delimiter = N','					
	SET @strID = @strID + @delimiter		--마지막에 DELIMITER 추가
	SELECT @sp_len = LEN(@delimiter)		--DELIMITER 길이
	SET @s = 1								--처음 시작위치지정

	WHILE (1=1)
	BEGIN
		--delimiter 검색
		SELECT @e = CHARINDEX (@delimiter, @strID, @s)
		IF @e <= 0 BREAK

		--@s, @e사이의 문자열 잘라냄.
		SET @str = LTRIM(RTRIM(SUBSTRING(@strID,@s,@e-@s)))
		SET @bug_id = CONVERT(INTEGER, @str)


		IF NOT EXISTS (SELECT id FROM bugs WHERE id = @bug_id)
			 BREAK

		SET @last_id = @bug_id

		SET @s = @e+@sp_len
	END

	RETURN @last_id
END;;


----------------------------------------------------------------
-- 사용자 정보 저장
----------------------------------------------------------------
CREATE PROCEDURE saveUser
(	 
	 @id		as integer			= null
	,@name		as NVARCHAR(128)
	,@realname	as NVARCHAR(128)		= N''
	,@timezone	as NVARCHAR(16)		= N'UTC'
)
as
BEGIN
	--기존에 존재하는 회원인지 확인 //존재하면 해당 ID반환.
	IF(@id is null)
		SELECT @id = id FROM profiles where login_name=@name

	IF (@id is null )
	BEGIN
		--등록
		INSERT INTO profiles VALUES (@name, @realname, @timezone, NULL, NULL)
		SET @id = @@IDENTITY
	END
	ELSE
	BEGIN
		IF (@realname <> N'') 
			UPDATE profiles SET realname = @realname	FROM profiles WHERE	login_name = @name

		IF (@timezone <> 'UTC') 
			UPDATE profiles SET timezone = @timezone	FROM profiles WHERE	login_name = @name
	END

	--등록결과 반환
	return @id
END;;


-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveClassification
(
	 @id		as integer		=null
	,@name    as NVARCHAR(64)
	,@desc    as NVARCHAR(64)	= N''
)
AS
BEGIN
	IF (@id is not null)
	BEGIN
		UPDATE c SET name = @name, [description] = @desc  FROM classifications c WHERE id = @id
		RETURN @id
	END

	SELECT @id = id FROM classifications WHERE name = @name

	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 from classifications
		INSERT INTO classifications VALUES(@id, @id, @name, @desc)
	END
	ELSE 
	BEGIN
		IF (@desc <> N'')
		BEGIN
			UPDATE c SET [description] = @desc  FROM classifications c WHERE id = @id
		END
	END

	--등록결과 반환
	return @id
END;;

	

-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveProduct
(
	 @name		as NVARCHAR(64)
	,@class_id  as integer			= 1
	,@desc		as NVARCHAR(1024)	= N''
)
as
BEGIN
	IF @class_id is null	RETURN -1
	IF @name is null		RETURN -1


	DECLARE @id  as integer
	SELECT @id=id FROM products WHERE name=@name

	IF @id is null
	BEGIN
		INSERT INTO products VALUES(@name, @class_id, N'', @desc, 0,10000,N'',0,0)
		SET @id = @@IDENTITY
	END
	ELSE 
	BEGIN
		IF (@class_id <> 1)
		BEGIN
			UPDATE p SET classification_id = @class_id  FROM products p WHERE id = @id
		END

		IF (@desc <> '')
		BEGIN
			UPDATE p SET [description] = @desc  FROM products p WHERE id = @id
		END
	END

	RETURN @id
END;;

-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveComponent
(
	 
	 @name			as NVARCHAR(64)
	,@product_id	as integer
	,@desc			as NVARCHAR(1024)	= N''
)
AS
BEGIN
	IF @product_id is null	RETURN -1
	IF @name is null		RETURN -1
	

	DECLARE @id  as integer
	SELECT @id=id FROM components WHERE name=@name and product_id = @product_id

	IF( @id is null)
	BEGIN
		INSERT INTO components VALUES(@product_id, @name, @desc,0, null)
		SET @id = @@IDENTITY
		PRINT @id
	END
	ELSE 
	BEGIN
		IF (@desc <> N'')
		BEGIN
			UPDATE c SET [description] = @desc  FROM components c WHERE id = @id
		END
	END

	RETURN @id

END;;


-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveKeyword
(
	 @name as NVARCHAR(64)
	,@desc as NVARCHAR(max)	=	N''
)
AS
BEGIN
	IF @name is null	RETURN -1

	DECLARE @id  as integer
	SELECT @id=id FROM keyworddefs WHERE name=@name

	IF( @id is null)
	BEGIN
		INSERT INTO keyworddefs VALUES(@name, @desc)
		SET @id = @@IDENTITY
	END
	ELSE 
	BEGIN
		IF (@desc <> N'')
		BEGIN
			UPDATE k SET [description] = @desc  FROM keyworddefs k WHERE id = @id
		END
	END

	RETURN @id
END;;

----------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------
--조금...불안정하네.
CREATE PROCEDURE saveActivity
(
	 @bug_id as integer
	,@who as integer
	,@when as NVARCHAR(64)
	,@what as NVARCHAR(64)
	,@removed as text			=N''
	,@added as text				=N''
	,@attach_id as integer		= null
)
as
BEGIN
	IF (@attach_id = -1) SET @attach_id = null

	IF @who is null		RETURN -1
	IF @bug_id is null	RETURN -1
	IF @when is null	RETURN -1
	IF @what is null	RETURN -1

	IF NOT EXISTS (SELECT bug_id FROM bugs_activity where bug_id = @bug_id and bug_when = CONVERT(datetime, @when, 120) and fieldid = @what)
	BEGIN
		BEGIN TRY
			INSERT INTO bugs_activity VALUES(@bug_id, @who, CONVERT(datetime, @when, 120), @what, @removed, @added, @attach_id)
			RETURN 1
		END TRY
		BEGIN CATCH
			IF (ERROR_NUMBER() = 2601)	RETURN 0 
			ELSE RETURN -1
		END CATCH

	END

	--상태 업데이트
	UPDATE B 
	SET	 progress = 'HIST_SAVED'
	FROM bugs_manager B
	where bug_id = @bug_id
	RETURN 0
END;;


-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveVote
(
	 @bug_id as integer
	,@user_id as integer
	,@count as integer
)
AS
BEGIN
	IF @user_id is null RETURN -1
	IF @bug_id is null 	RETURN -1
	IF @count is null 	RETURN -1


	BEGIN TRY
		INSERT INTO votes VALUES(@bug_id, @user_id, @count)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH

	return 1
END;;







-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveCC
(
	 @bug_id as integer
	,@user_id as integer
)
AS
BEGIN
	IF @user_id is null RETURN -1
	IF @user_id is null RETURN -1
	
	BEGIN TRY
		INSERT INTO cc VALUES(@bug_id, @user_id)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH
	
	return 1
END;;


-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveSeeAlso
(
	 @bug_id as integer
	,@value as NVARCHAR(255)
)
AS
BEGIN	
	IF @bug_id is null	RETURN -1
	IF @value is null	RETURN -1

	BEGIN TRY
		INSERT INTO bug_see_also VALUES (@bug_id, @value)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH

	RETURN 1
END;;



-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveDependency
(
	 @bug_id as integer
	,@dependson as integer
)
AS
BEGIN
	IF @bug_id is null RETURN -1
	IF @dependson is null RETURN -1

	BEGIN TRY
		INSERT INTO dependencies VALUES (@bug_id, @dependson)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH


	return 1
END;;


-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveDuplication
(
	 @bug_id as integer
	,@dup_id as integer
)
AS
BEGIN	

	IF @bug_id is null RETURN -1
	IF @dup_id is null RETURN -1

	BEGIN TRY
		INSERT INTO duplicates VALUES (@bug_id, @dup_id)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH

	RETURN 1
END;;



-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveRelationship
(
	 @src_id as integer
	,@dest_id as integer
)
AS
BEGIN	

	IF @src_id is null RETURN -1
	IF @dest_id is null RETURN -1

	BEGIN TRY
		INSERT INTO relationships VALUES (@src_id, @dest_id)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH

	RETURN 1
END;;


-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveComment
(
	 @comment_id	int
	,@type			smallint
	,@bug_id		int
	,@who			integer
	,@bug_when		NVARCHAR(64)
	,@thetext		text			=N''
	,@isprivate		tinyint
)
AS
BEGIN
	DECLARE @id as integer

	SELECT @id = comment_id FROM longdescs where comment_id = @comment_id

	IF (@id is null)
	BEGIN 
		BEGIN TRY
			INSERT INTO longdescs VALUES (@comment_id
										,@type		
										,@bug_id	
										,@who
										,convert(datetime,@bug_when	,120)
										,@thetext	
										,N''
										,@isprivate)
		END TRY
		BEGIN CATCH
			IF (ERROR_NUMBER() = 2601)	RETURN 0 
			ELSE RETURN -1
		END CATCH

		RETURN 1
	END

	RETURN 0
END;;


-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveAttachment
(
	  @attach_id		int				
	, @bug_id			int				
	, @creation_ts		NVARCHAR(100)
	, @updated_ts		NVARCHAR(100)	
	, @description		text			
	, @filename			NVARCHAR(100)	
	, @filesize			integer
	, @mimetype			NVARCHAR(100)	
	, @isobsolete		tinyint			
	, @ispatch			tinyint			
	, @isprivate		tinyint			
	, @isurl			tinyint			
	, @user_id			integer
	, @thedata			NVARCHAR(max)
)
AS
BEGIN


	BEGIN TRY
		INSERT INTO attachments VALUES (  @attach_id		
										, @bug_id			
										, convert(datetime,@creation_ts, 120)
										, @description
										, @filename	
										, @isobsolete
										, @ispatch
										, @isprivate
										, @isurl
										, @filesize
										, @mimetype
										, convert(datetime,@updated_ts,120)
										, @user_id)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH
			
	BEGIN TRY						
		INSERT INTO attach_data VALUES (@attach_id, convert(varbinary(max),@thedata))
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH

	RETURN 1
END;;

-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------

CREATE PROCEDURE saveOS
(
	@value as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1

	DECLARE @id			AS INTEGER
	DECLARE @sortkey	AS INTEGER
	
	SELECT @id = id FROM op_sys WHERE value = @value
	
	IF (@id is null)
	BEGIN
		SELECT @sortkey = count(*) from op_sys
		INSERT INTO op_sys VALUES(@sortkey, @value,1,0)
		SET @id = @@IDENTITY
	END

	RETURN @id
END;;

-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveSeverity
(
	@value as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1

	DECLARE @id			AS INTEGER
	DECLARE @sortkey	AS INTEGER
	
	SELECT @id = id FROM bug_severity WHERE value = @value
	
	IF (@id is null)
	BEGIN
		SELECT @sortkey = count(*) from bug_severity
		INSERT INTO bug_severity VALUES(@sortkey, @value,1,0)
		SET @id = @@IDENTITY
	END

	RETURN @id
END;;


-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveStatus
(
	@value as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1

	DECLARE @id			AS INTEGER
	DECLARE @sortkey	AS INTEGER
	
	SELECT @id = id FROM bug_status WHERE value = @value
	
	IF (@id is null)
	BEGIN
		SELECT @sortkey = count(*) from bug_status
		INSERT INTO bug_status VALUES(@sortkey, @value,1,1,0)
		SET @id = @@IDENTITY
	END

	RETURN @id
END;;


-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE savePriority
(
	@value as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1

	DECLARE @id			AS INTEGER
	DECLARE @sortkey	AS INTEGER
	
	SELECT @id = id FROM [priority] WHERE value = @value
	
	IF (@id is null)
	BEGIN
		SELECT @sortkey = count(*) from [priority]
		INSERT INTO [priority] VALUES(@sortkey, @value,1,0)
		SET @id = @@IDENTITY
	END

	RETURN @id
END;;


-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE savePlatform
(
	@value as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1

	DECLARE @id			AS INTEGER
	DECLARE @sortkey	AS INTEGER
	
	SELECT @id = id FROM rep_platform WHERE value = @value
	
	IF (@id is null)
	BEGIN
		SELECT @sortkey = count(*) from rep_platform
		INSERT INTO rep_platform VALUES(@sortkey, @value,1,0)
		SET @id = @@IDENTITY
	END

	RETURN @id
END;;

-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveResolution
(
	@value as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1

	DECLARE @id			AS INTEGER
	DECLARE @sortkey	AS INTEGER
	
	SELECT @id = id FROM resolution WHERE value = @value
	
	IF (@id is null)
	BEGIN
		SELECT @sortkey = count(*) from resolution
		INSERT INTO resolution VALUES(@sortkey, @value,1,0)
		SET @id = @@IDENTITY
	END

	RETURN @id
END;;

-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveMilestone
(
	 @product_id	as integer
	,@value			as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)			RETURN -1
	IF (@product_id is null)	RETURN -1

	DECLARE @id			AS INTEGER
	DECLARE @sortkey	AS INTEGER
	
	SELECT @id = id FROM milestones WHERE value = @value and product_id = @product_id
	
	
	IF (@id is null)
	BEGIN
		SELECT @sortkey = count(*) from milestones
		INSERT INTO milestones VALUES(@sortkey, @product_id, @value)
		SET @id = @@IDENTITY
	END

	RETURN @id
END;;

-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveVersion
(
	@product_id		as integer
	,@value			as NVARCHAR(64)
)
AS
BEGIN

	IF (@value is null)			RETURN -1
	IF (@product_id is null)	RETURN -1

	DECLARE @id			AS INTEGER
	
	SELECT @id = id FROM versions WHERE value = @value and product_id = @product_id
	
	IF (@id is null)
	BEGIN
		INSERT INTO versions VALUES(@product_id, @value)
		SET @id = @@IDENTITY
	END

	RETURN @id
END;;

	
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE checkDuplicateReport
(
	 @bug_id		as integer
)
AS
BEGIN
	IF (@bug_id is null)			RETURN -1


	DECLARE @id	AS INTEGER
	
	SELECT @id = id FROM bugs WHERE id = @bug_id
	
	IF (@id is null)
	BEGIN
		RETURN 0
	END

	RETURN @id
END;;
	


--------------------------------------------
--------------------------------------------
--------------------------------------------
CREATE PROCEDURE saveKeywordMap
(
	 @bug_id as integer
	,@keywords as NVARCHAR(max)	--원래는 text타입이지만 변경. (text는 len함수를 사용못함)
)
AS
BEGIN
	DECLARE @s integer
	DECLARE @e integer
	DECLARE @sp_len integer
	DECLARE @delimiter NVARCHAR(4)
	DECLARE @str NVARCHAR(64)
	DECLARE @key_id integer
		
	SET @delimiter = N','
	SET @keywords = @keywords + @delimiter
	SELECT @sp_len = LEN(@delimiter)
	SET @s = 1

	WHILE (1=1)
	BEGIN
		SELECT @e = CHARINDEX (@delimiter, @keywords,@s)
		IF @e <= 0 BREAK

		SET @str = LTRIM(RTRIM(SUBSTRING(@keywords,@s,@e-@s)))
		
		SET @key_id = null

		SELECT @key_id = id from keyworddefs where name=@str
		IF @key_id is null
		BEGIN
			IF @str<>''
			BEGIN
				INSERT INTO keyworddefs VALUES (@str,N'')
				SET @key_id = @@IDENTITY
			END
		END
		
		--키 관계테이블에 삽입
		Declare @state as BIT = 0
		IF @key_id is not null
		BEGIN
			BEGIN TRY
				INSERT INTO keywords VALUES (@bug_id, @key_id)
				SET @state = 1
			END TRY
			BEGIN CATCH
				IF (ERROR_NUMBER() = 2601)
					SET @state = 1
				ELSE 
					SET @state = 0
					BREAK
			END CATCH
		END

		IF @state = 0
			RETURN -1
		
		SET @s = @e+@sp_len
	END

	RETURN 1
END;;


-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveBugReport
(
	 @bug_id				as int				
	,@alias					as NVARCHAR(20)		=N''	
	,@status				as NVARCHAR(64)		
	,@resolution			as NVARCHAR(64)		
	,@product_id			as integer
	,@component_id			as integer
	,@version				as NVARCHAR(64)		=N''
	,@target_milestone		as NVARCHAR(20)     	=N''
	,@priority				as NVARCHAR(64)		=N''
	,@severity				as NVARCHAR(64)		=N''
	,@votes					as int				
	,@platform				as NVARCHAR(64)		=N''	
	,@os					as NVARCHAR(64)		=N''

	,@reporter_id			as integer
	,@assignee_id			as integer			= null
	,@qa_contact_id			as integer			= null
	
	,@bug_file_loc			as text				= null
	,@keywords				as NVARCHAR(max)		=N''
	,@short_desc			as NVARCHAR(255)		=N''
	,@status_whiteboard		as text				=N''
	,@creation_ts			as NVARCHAR(127)
	,@delta_ts				as NVARCHAR(127)
	,@reporter_accessible	as tinyint			= 1
	,@cclist_accessible		as tinyint			= 1
	,@everconfirmed			as tinyint			= 0
	,@lastdiffed			as NVARCHAR(127)		= null
	,@remaining_time		as decimal(10,2)	= 0
	,@deadline				as NVARCHAR(127)		= null
	,@estimated_time		as decimal(10,2)	= 0
	,@actual_time			as decimal(10,2)	= 0
	--,@classification_id		as integer
	--,@classification_name	as NVARCHAR(64)
)
AS
BEGIN
	IF @reporter_id is null RETURN -1
	IF @product_id is null RETURN -1
	IF @component_id is null RETURN -1

	
	--버그리포트 등록
	BEGIN TRY
		INSERT INTO bugs VALUES(  @bug_id
								, @alias
								, @status
								, @resolution
								, @product_id
								, @component_id
								, @version
								, @target_milestone
								, @priority
								, @severity
								, @votes
								, @platform
								, @os
								, @reporter_id
								, @assignee_id
								, @qa_contact_id
								, @bug_file_loc
								, @keywords
								, @short_desc
								, @status_whiteboard
								, CONVERT(datetime, @creation_ts, 120)
								, CONVERT(datetime, @delta_ts, 120)
								, @reporter_accessible
								, @cclist_accessible
								, @everconfirmed
								, CONVERT(datetime, @lastdiffed, 120)
								, CONVERT(datetime, @deadline, 120)
								, @remaining_time								
								, @estimated_time
								, @actual_time)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)
		BEGIN
			UPDATE bugs 
			SET
				 alias               = @alias
				,bug_status          = @status
				,resolution          = @resolution
				,product_id          = @product_id
				,component_id        = @component_id
				,[version]             = @version
				,target_milestone    = @target_milestone
				,[priority]            = @priority
				,bug_severity        = @severity
				,votes               = @votes
				,rep_platform        = @platform
				,op_sys              = @os
				,reporter            = @reporter_id
				,assigned_to         = @assignee_id
				,qa_contact          = @qa_contact_id
				,bug_file_loc        = @bug_file_loc
				,keywords            = @keywords
				,short_desc          = @short_desc
				,status_whiteboard   = @status_whiteboard
				,creation_ts		 = CONVERT(datetime, @creation_ts, 120)
				,delta_ts			 = CONVERT(datetime, @delta_ts, 120)
				,reporter_accessible = @reporter_accessible
				,cclist_accessible   = @cclist_accessible
				,everconfirmed       = @everconfirmed
				,lastdiffed			 = CONVERT(datetime, @lastdiffed, 120)
				,deadline	         = CONVERT(datetime, @deadline, 120)
				,remaining_time      = @remaining_time
				,estimated_time      = @estimated_time
				,actual_time		 = @actual_time
			FROM bugs
			WHERE id = @bug_id
		END
		ELSE RETURN -1
	END CATCH

	--등록후 부가작업---------------------------------------------------------
	EXEC saveKeywordMap @bug_id, @keywords
	EXEC saveVersion @product_id, @version
	EXEC saveMilestone @product_id, @target_milestone
	EXEC saveOS @os
	EXEC saveSeverity @severity
	EXEC saveStatus @status
	EXEC savePlatform @platform
	EXEC saveResolution @resolution
	EXEC savePriority @priority

	RETURN 1
END;;





----------------------------------------------
--새로운 버그리스트를 추가한다
----------------------------------------------
CREATE PROCEDURE addList
(
	 @date			as NVARCHAR(20)
	,@type			as integer
	,@url			as NVARCHAR(512)	
	,@bug_id		as integer			= null
)
AS
BEGIN
	Declare @id as integer

	SELECT @id = id from list_manager where url = @url

	IF(@id is null)
	BEGIN
		INSERT INTO list_manager([date], [type], url, progress, bug_id) VALUES (@date, @type, @url, N'REQUESTED', @bug_id)
		SET @id = @@IDENTITY
	END

	RETURN @id
END;;


----------------------------------------------
--새로운 버그리스트를 추가한다
----------------------------------------------
CREATE PROCEDURE addListInfo
(
	 @date			as NVARCHAR(20) = null
	,@type			as integer
	,@url			as NVARCHAR(512)	
	,@path			as NVARCHAR(512)
)
AS
BEGIN
	Declare @id as integer
	SELECT @id = id from list_manager where url = @url

	IF(@id is null)
	BEGIN
		INSERT INTO list_manager([type], [date], progress, url,  [path]) VALUES (@type, @date, N'SAVED', @url, @path)
		SET @id = @@IDENTITY

		select * from list_manager
	END

	RETURN @id
END;;

--상태유형 : REQUESTED, DOWNLOADED, SAVED, DONE (?)

----------------------------------------------
-- 새로운 버그리스트에 대한 데이터를 추가한다.
-- 추후 데이터도 넣고자 하는경우 아래 DATA변수 활용.
----------------------------------------------
CREATE PROCEDURE update_pathList
(
	 @id			as integer
	,@path			as NVARCHAR(512)
	--,@data			as varbinary(max)
)
AS
BEGIN
	IF NOT EXISTS (SELECT id from list_manager where id = @id)
		RETURN -1

	UPDATE l 
	SET [path] = @path
		,progress = N'DOWNLOADED'
	FROM list_manager l
	WHERE id = @id

	
	RETURN @id
END;;


----------------------------------------------
--새로운 버그리스트의 버그 아이디들을 등록한다.
----------------------------------------------
CREATE PROCEDURE add_BugList
(
	 @id		as integer			--LIST ID
	,@delimiter	as NVARCHAR(2)
	,@strID		as NVARCHAR(MAX)
)
AS
BEGIN
	IF @strID is null RETURN -1
	IF NOT EXISTS (SELECT id from list_manager where id = @id)
		RETURN -1

	INSERT INTO bugs_manager(bug_id, list_id, progress)			--bug_manager에 삽입
	SELECT	 item												-- 삽입할 정보 : bug_id
			,@id as list_id										-- 삽입할 정보 : list_id
			,N'PENDING' as progress								-- 삽입할 정보 : 정보 수집 상태
	from getIntArray(@strID, @delimiter) i						--@strID로부터 ID배열을 얻어옴.
	left join bugs_manager b on i.Item = b.bug_id				--기존의 bugid에 존재하는지 확인.
	where b.bug_id is null										-- 존재하지 않는 bugid만 삽입.


	--처리결과 상태 반영
	UPDATE l 
	SET progress = N'SAVED'
	FROM list_manager l
	WHERE id = @id

	RETURN @id
END;;



----------------------------------------------
--버그정보를 업데이트 한다.
----------------------------------------------
CREATE PROCEDURE update_bugInfo
(
	 @bug_id			as integer
	,@progress			as NVARCHAR(30)
	,@report_url		as NVARCHAR(512)	
	,@report_path		as NVARCHAR(512)	
	,@hist_url			as NVARCHAR(512)	
	,@hist_path			as NVARCHAR(512)	
	,@vote_url			as NVARCHAR(512)	
	,@vote_path			as NVARCHAR(512)	
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @bug_id)
		RETURN -1

	UPDATE B 
	SET	 progress = @progress
		,bug_url = @report_url
		,bug_path = @report_path
		,history_url = @hist_url
		,history_path = @hist_path
		,vote_url = @vote_url
		,vote_path = @vote_path
	FROM bugs_manager B
	where bug_id = @bug_id

	RETURN @bug_id
END;;



----------------------------------------------
-- 버그리포트의 상태를 반영한다. (버그리포트 웹에 요청 완료)
----------------------------------------------
CREATE PROCEDURE check_bugDuplicate
(
	 @bug_id			as integer			--BUG ID
)
AS
BEGIN
	--같은 버그리포트가 존재하는지,
	--완료된 버그리포트가 존재하는지 검사
	IF EXISTS (SELECT bug_id FROM bugs_manager WHERE bug_id = @bug_id and progress=N'DONE')
		RETURN @bug_id
	ELSE
		RETURN -1
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다. (버그리포트 웹에 요청 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_bugURL
(
	 @id			as integer			--BUG ID
	,@url			as NVARCHAR(512)
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'BUG_REQUESTED'
		,bug_url = @url
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다. (버그리포트 다운로드 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_bugDownload
(
	 @id			as integer			--BUG ID
	,@path			as NVARCHAR(512)
	--,@data_bug		as varbinary(MAX)
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'BUG_DOWNLOADED'
		,bug_path = @path
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다. (버그리포트 다운로드 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_bugParsed
(
	 @id			as integer			--BUG ID
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'BUG_PARSED'
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다.(히스토리페이지 웹에 요청 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_histURL
(
	 @id			as integer			--BUG ID
	,@url			as NVARCHAR(512)
	--,@data_bug		as varbinary(MAX)
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'HIST_REQUESTED'
		,history_url = @url
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다. (히스토리페이지 다운로드 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_histDownload
(
	 @id			as integer			--BUG ID
	,@path			as NVARCHAR(512)
	--,@data_bug		as varbinary(MAX)
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'HIST_DOWNLOADED'
		,history_path = @path
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;


----------------------------------------------
-- 버그리포트의 상태를 반영한다. (히스토리 저장 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_bugSaved
(
	 @id			as integer			--BUG ID
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'BUG_SAVED'
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다. (히스토리 파싱 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_histParsed
(
	 @id			as integer			--BUG ID
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'HIST_PARSED'
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다. (히스토리 저장 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_histSaved
(
	 @id			as integer			--BUG ID
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'HIST_SAVED'
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;


----------------------------------------------
-- 버그리포트의 상태를 반영한다. (투표페이지 웹에 요청 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_voteURL
(
	 @id			as integer			--BUG ID
	,@url			as NVARCHAR(512)
	--,@data_bug		as varbinary(MAX)
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'VOTE_REQUESTED'
		,vote_url = @url
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다. (투표페이지 다운로드 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_voteDownload
(
	 @id			as integer			--BUG ID
	,@path			as NVARCHAR(512)
	--,@data_bug		as varbinary(MAX)
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'VOTE_DOWNLOADED'
		,vote_path = @path
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다. (투표 저장 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_voteParsed
(
	 @id			as integer			--BUG ID
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'VOTE_PARSED'
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;

----------------------------------------------
-- 버그리포트의 상태를 반영한다. (투표 저장 완료)
----------------------------------------------
CREATE PROCEDURE updateBug_voteSaved
(
	 @id			as integer			--BUG ID
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1


	Declare @url NVARCHAR(512)

	SELECT @url = vote_url from bugs_manager where bug_id = @id

	IF @url is null
		UPDATE B 
		SET	 progress = N'VOTE_SAVED', vote_url=N'', vote_path=N''
		FROM bugs_manager B
		where bug_id = @id
	ELSE
		UPDATE B 
		SET	 progress = N'VOTE_SAVED'
		FROM bugs_manager B
		where bug_id = @id

	RETURN @id
END;;



----------------------------------------------
-- 버그리포트의 상태를 반영한다. (최종완료)
----------------------------------------------
CREATE PROCEDURE updateBug_done
(
	 @id			as integer			--BUG ID
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @id)
		RETURN -1

	UPDATE B 
	SET	 progress = N'DONE'
	FROM bugs_manager B
	where bug_id = @id

	RETURN @id
END;;


---------------------------------------------
-- 재시작할 아이디 목록 구하기
---------------------------------------------
CREATE PROCEDURE getRevivalIDList
(
	@ret NVARCHAR(max) out
)
AS
BEGIN

	Declare ID_CURSOR CURSOR FOR SELECT bug_id FROM bugs_manager WHERE progress in (N'PENDING', N'FAIL')
	Declare @id as integer

	--기본값 설정
	SET @ret = ''
		
	--커서 시작
	OPEN ID_CURSOR
	FETCH NEXT FROM ID_CURSOR	INTO @id

	WHILE @@FETCH_STATUS = 0
	BEGIN
		
		SET @ret = @ret + convert(NVARCHAR(64),@id)

		FETCH NEXT FROM ID_CURSOR	INTO @id
		
		IF (@@FETCH_STATUS = 0) 
			SET @ret = @ret + N','
	END
	
	CLOSE ID_CURSOR
	DEALLOCATE ID_CURSOR
	RETURN 1
END;;