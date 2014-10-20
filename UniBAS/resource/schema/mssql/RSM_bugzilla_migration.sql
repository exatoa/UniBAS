/**
	입력하는 데이터베이스 명을 변수화 해서 설정한 데이터베이스에 맞도록 해야햠.
**/

------------------------------------------------------
-- MIGRATION BUG
------------------------------------------------------
CREATE PROCEDURE Migration_bug
(
	@site_id	integer
)
AS
BEGIN
	
	insert into <{0}>.dbo.bug 
	select
		 @site_id					as site_id
		,id							as ID
		,reporter					as reporter_id
		,assigned_to				as assignee_id
		,qa_contact					as qa_id
		,product_id					as project_id
		,component_id				as component_id
		,[bug_status]				as [status]
		,resolution					as resolution
		,everconfirmed				as everconfirmed
		,[priority]					as [priority]
		,bug_severity				as [severity]
		,rep_platform				as [platform]
		,op_sys						as [os]
		,[version]					as [version]
		,null						as fixed_in_version
		,target_milestone			as milestone
		,short_desc					as summary
		,(select top 1 thetext from longdescs where bug_id = b.id order by comment_id)	as [description]
		,creation_ts
		,delta_ts
	from bugs b
		
	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''bugs'' migrated to ''bug''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;

	
------------------------------------------------------
-- MIGRATION COMMENTS
------------------------------------------------------
--Comment Type은 모두가 0번.   Mantis가 Type이 있음.
CREATE PROCEDURE Migration_comment
(
	@site_id	as	integer
)
as
BEGIN
	--ID선택.
	DECLARE @bug_id as integer
	DECLARE @rowCnt as Integer = 0
	
	DECLARE CUR_IDS CURSOR FOR
	select id from bugs

	OPEN CUR_IDS
	FETCH NEXT FROM CUR_IDS INTO @bug_id

	WHILE (@@FETCH_STATUS = 0)
	BEGIN
		insert into <{0}>.dbo.comment
		select 
			 @site_id
			,comment_id
			,bug_id
			,null			as parent_comment_id
			,who
			,[type]
			,bug_when
			,bug_when
			,thetext
		from longdescs
		where bug_id = @bug_id
		and comment_id <> (select top 1 comment_id from longdescs where bug_id=@bug_id)

		SET @rowCnt += @@RowCount
		PRINT N'worked id = ' + cast(@bug_id as NVARCHAR)

		FETCH NEXT FROM CUR_IDS INTO @bug_id
	END

	CLOSE CUR_IDS
	DEALLOCATE CUR_IDS


	PRINT N'Table ''longdescs'' migrated to ''comment''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;
	

------------------------------------------------------
-- MIGRATION ATTACHMENT
------------------------------------------------------
CREATE PROCEDURE Migration_attachment
(
	@site_id as INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.attachment
	select 
			 @site_id
			,attach_id
			,bug_id
			,submitter_id
			,ispatch
			,[filename]
			,mimetype
			,filesize
			,creation_ts
			,modification_time
	from attachments

	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''attachments'' migrated to ''attachment''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;




------------------------------------------------------
-- MIGRATION ATTACH_DATA
------------------------------------------------------

CREATE PROCEDURE Migration_attach_data
(
	@site_id	AS INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.attach_data
	select
		 @site_id
		,id
		,thedata
	from attach_data

	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''attach_data'' migrated to ''attach_data''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;




------------------------------------------------------
-- MIGRATION HISTORY
------------------------------------------------------

CREATE PROCEDURE Migration_history
(
	@site_id	AS INTEGER
)
as
BEGIN
	
	--Query문 생성
	insert into <{0}>.dbo.history
	select
		 @site_id
		,bug_id
		,who
		,bug_when
		,fieldid
		,removed
		,added
	from bugs_activity
	 
	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''bugs_activity'' migrated to ''history''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;




------------------------------------------------------
-- MIGRATION Relationship
--	None(0)
--	DuplicateOf(1), 	// dupe - dupe_of
--	HasDuplicate(2), 	// dupe_of - dupe
--	ChildOf(3),			// blocked - dependson		
--	ParentOf(4),		// dependson - blocked
--	RelatedTo(5);		// src - dest
------------------------------------------------------
CREATE PROCEDURE Migration_relationship
(
	@site_id	AS INTEGER
)
as
BEGIN
	Declare @totalCnt as integer = 0
	Declare @rowCnt as integer = 0

	--종속성을 가지는 버그들에 대한 정보 이동
	insert into <{0}>.dbo.relationship
	select 
		 @site_id
		,blocked
		,dependson
		,3					--2번 은 child of type.
	from dependencies

	SET @rowCnt = @@RowCount
	PRINT N'Table ''dependencies'' migrated to ''relationship''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	SET @totalCnt += @rowCnt
	
	--중복 버그들 체크.
	insert into <{0}>.dbo.relationship
	select
		 @site_id
		,dupe
		,dupe_of
		,1
	from duplicates
	
	SET @rowCnt = @@RowCount
	PRINT N'Table ''dependencies'' migrated to ''relationship''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	SET @totalCnt += @rowCnt


	--See Also의 값들도 버그리포트를 참조하는 경우 관계로 변환
	Declare @url as nvarchar(512)
	SELECT @url = base_url FROM <{0}>.dbo.[site] WHERE id = @site_id
	SET @url += N'show_bug.cgi?id='
	
	INSERT INTO <{0}>.dbo.relationship
	SELECT site_id, src_bug_id, dest_bug_id, relationship_type FROM
	(
		SELECT 
		 @site_id								as site_id
		,bug_id									as src_bug_id
		,substring(value, len(@url)+1, 20)		as dest_bug_id
		,5										as relationship_type
		FROM bug_see_also where value like @url+'%'
	)T
	where T.dest_bug_id not like '%[a-z]%'
	
	SET @rowCnt = @@RowCount
	PRINT N'Table ''bug_see_also'' migrated to ''relationship''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	SET @totalCnt += @rowCnt

	RETURN @totalCnt
END;;




------------------------------------------------------
-- MIGRATION Reference
------------------------------------------------------
CREATE PROCEDURE Migration_reference
(
	@site_id	AS INTEGER
)
as
BEGIN
	
	insert into <{0}>.dbo.reference
	select
		 @site_id
		,bug_id
		,value
	from bug_see_also

	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''bug_see_also'' migrated to ''reference''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;


------------------------------------------------------
-- MIGRATION Monitor
------------------------------------------------------
CREATE PROCEDURE Migration_monitor
(
	@site_id	AS INTEGER
)
as
BEGIN
	
	insert into <{0}>.dbo.monitor
	select
		 @site_id
		,bug_id
		,who
	from cc

	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''cc'' migrated to ''monitor''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;




------------------------------------------------------
-- MIGRATION USER
------------------------------------------------------
CREATE PROCEDURE Migration_user
(
	@site_id	AS INTEGER
)
as
BEGIN
	
	insert into <{0}>.dbo.[user]
	select 
		 @site_id
		,id
		,login_name
		,realname
		,null				as [role]
		,null				as creation_ts
		,null				as lastchange_ts
	from profiles

	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''profiles'' migrated to ''user''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;


------------------------------------------------------
-- MIGRATION PROJECT
------------------------------------------------------
CREATE PROCEDURE Migration_project
(
	@site_id	AS INTEGER
)
as
BEGIN
	
	insert into <{0}>.dbo.project
	select 
		 @site_id
		,id
		,null
		,name
		,[description]
	from products


	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''products'' migrated to ''product''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;





------------------------------------------------------
-- MIGRATION COMPONENT
------------------------------------------------------
CREATE PROCEDURE Migration_component
(
	@site_id	AS INTEGER
)
as
BEGIN
	
	insert into <{0}>.dbo.component
	select 
		 @site_id
		,id
		,product_id
		,name
		,description
	from components


	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''components'' migrated to ''component''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;




------------------------------------------------------
-- MIGRATION keyword
------------------------------------------------------
CREATE PROCEDURE Migration_keyword
(
	@site_id	AS INTEGER
)
as
BEGIN

	INSERT INTO <{0}>.dbo.keyword
	SELECT 
		 @site_id
		,id
		,name
		,[description]
	FROM keyworddefs

	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''keyworddefs'' migrated to ''keyword''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;



------------------------------------------------------
-- MIGRATION BUG_keyword
------------------------------------------------------
CREATE PROCEDURE Migration_bug_keyword
(
	@site_id	AS INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.bug_keyword
	select 
		 @site_id
		,bug_id
		,keywordid
	from keywords
	
	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''keywords'' migrated to ''bug_tag''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;

------------------------------------------------------
-- MIGRATION additional_info
------------------------------------------------------
CREATE PROCEDURE Migration_additional_info
(
	@site_id	AS INTEGER
)
as
BEGIN
	PRINT 'None'	
	RETURN 1
END;;



------------------------------------------------------
-- MIGRATION STATUS
------------------------------------------------------
CREATE PROCEDURE Migration_status
(
	@site_id	AS INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.[status]
	select 
		 @site_id
		,id
		,sortkey
		,value
	from bug_status

	
	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''bug_status'' migrated to ''status''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;


------------------------------------------------------
-- MIGRATION RESOLUTION
------------------------------------------------------
CREATE PROCEDURE Migration_resolution
(
	@site_id	AS INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.resolution
	select 
		 @site_id
		,id
		,sortkey
		,value
	from resolution

	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''resolution'' migrated to ''resolution''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;


------------------------------------------------------
-- MIGRATION SEVERITY
------------------------------------------------------
CREATE PROCEDURE Migration_severity
(
	@site_id	AS INTEGER
)
as
BEGIN

	INSERT INTO <{0}>.dbo.severity
	SELECT 
		 @site_id
		,id
		,sortkey
		,value
	FROM bug_severity


	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''bug_severity'' migrated to ''severity''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;




------------------------------------------------------
-- MIGRATION PRIORITY
------------------------------------------------------
CREATE PROCEDURE Migration_priority
(
	@site_id	AS INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.[priority]
	select 
		 @site_id
		,id
		,sortkey
		,value
	from [priority]


	
	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''priority'' migrated to ''priority''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;





------------------------------------------------------
-- MIGRATION PLATFORM
------------------------------------------------------
CREATE PROCEDURE Migration_platform
(
	@site_id	AS INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.[platform]
	select 
		 @site_id
		,id
		,sortkey
		,value
	from rep_platform
	

	
	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''rep_platform'' migrated to ''platform''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;



------------------------------------------------------
-- MIGRATION OS
------------------------------------------------------
CREATE PROCEDURE Migration_os
(
	@site_id	AS INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.os
	select 
		 @site_id
		,id
		,sortkey
		,value
	from op_sys


	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''op_sys'' migrated to ''os''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;



------------------------------------------------------
-- MIGRATION VERSION
------------------------------------------------------
CREATE PROCEDURE Migration_version
(
	@site_id	AS INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.[version]
	select 
		 @site_id
		,product_id
		,id
		,value
	from versions

	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''versions'' migrated to ''version''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;




------------------------------------------------------
-- MIGRATION MILESTONE
------------------------------------------------------
CREATE PROCEDURE Migration_milestone
(
	@site_id	AS INTEGER
)
as
BEGIN

	insert into <{0}>.dbo.milestone
	select 
		 @site_id
		,product_id
		,id
		,sortkey
		,value
	from milestones

	Declare @rowCnt as integer = @@RowCount
	PRINT N'Table ''milestones'' migrated to ''milestone''! Inserted ' + cast(@rowCnt as NVARCHAR) + N' rows'
	RETURN @rowCnt
END;;

