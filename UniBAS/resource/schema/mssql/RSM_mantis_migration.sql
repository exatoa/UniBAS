
------------------------------------------------------
-- MIGRATION BUG
------------------------------------------------------
CREATE procedure Migration_bugs
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + N'.dbo.bug '
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N'	id'
	SET @sql = @sql +	N'	,alias'
	SET @sql = @sql +	N'	,[bug_status]'
	SET @sql = @sql +	N'	,resolution'
	SET @sql = @sql +	N'	,product_id'
	SET @sql = @sql +	N'	,component_id'
	SET @sql = @sql +	N'	,[version]'
	SET @sql = @sql +	N'	,target_milestone'
	SET @sql = @sql +	N'	,target_milestone'			--fixed_in_version
	SET @sql = @sql +	N'	,[priority]'
	SET @sql = @sql +	N'	,bug_severity'
	SET @sql = @sql +	N'	,'''''						--reproducibility
	SET @sql = @sql +	N'	,rep_platform'
	SET @sql = @sql +	N'	,op_sys'
	SET @sql = @sql +	N'	,reporter'
	SET @sql = @sql +	N'	,assigned_to'
	SET @sql = @sql +	N'	,qa_contact'
	SET @sql = @sql +	N'	,short_desc'
	SET @sql = @sql +	N'	,status_whiteboard'
	SET @sql = @sql +	N'	,creation_ts'
	SET @sql = @sql +	N'	,delta_ts'
	SET @sql = @sql +	N'	,0'							--profile_id(Mantis에 만 있는것) 빈걸로 함
	SET @sql = @sql +	N'	,1'
	SET @sql = @sql +	N'	,keywords'
	SET @sql = @sql +	N'	,bug_file_loc'
	SET @sql = @sql +	N'	,deadline'
	SET @sql = @sql +	N'	,votes'
	SET @sql = @sql +	N'	,reporter_accessible'
	SET @sql = @sql +	N'	,cclist_accessible'
	SET @sql = @sql +	N'	,everconfirmed'
	SET @sql = @sql +	N'	,lastdiffed'
	SET @sql = @sql +	N'	,remaining_time'
	SET @sql = @sql +	N'	,estimated_time'
	SET @sql = @sql +	N'	,'''''					-- build
	SET @sql = @sql +	N'	,'''''					-- eta
	SET @sql = @sql +	N'	,0'						-- sponsorship_total
	SET @sql = @sql +	N'	,0'						-- sticky
	SET @sql = @sql +	N'	,0'						-- projection
	SET @sql = @sql +	N' from bugs'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''bugs'' migrated to ''bug''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;


	
------------------------------------------------------
-- MIGRATION COMMENTS
------------------------------------------------------
--Comment Type은 모두가 0번.   Mantis가 Type이 있음.
CREATE procedure Migration_longdescs
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.bugnote'
	SET @sql =	@sql +	N' select '
	SET @sql =	@sql +	N'	 comment_id'
	SET @sql =	@sql +	N'	,bug_id'
	SET @sql =	@sql +	N'	,[type]'
	SET @sql =	@sql +	N'	,who'
	SET @sql =	@sql +	N'	,bug_when'
	SET @sql =	@sql +	N'	,bug_when'
	SET @sql =	@sql +	N'	,isprivate'
	SET @sql =	@sql +	N'	,thetext'
	SET @sql =	@sql +	N' from longdescs'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''longdescs'' migrated to ''bugnote''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;
	

	
------------------------------------------------------
-- MIGRATION BUGNOTE_TYPE
------------------------------------------------------
CREATE procedure Migration_bugnote_type
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.bugnote_type'
	SET @sql =	@sql +	N' values	 (0, 0,''Comment'',1)'
	SET @sql =	@sql +	N'			,(1, 1,''Description'',1)'
	SET @sql =	@sql +	N'			,(2, 2,''Reproduce'',1)'
	SET @sql =	@sql +	N'			,(3, 3,''Summary'',1)'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Inserted ''bugnote_type'' values! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;
	


		
------------------------------------------------------
-- MIGRATION ATTACHMENT
------------------------------------------------------

CREATE procedure Migration_attachments
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.attachment'
	SET @sql =	@sql +	N' select '
	SET @sql =	@sql +	N' 		attach_id'
	SET @sql =	@sql +	N' 		,bug_id	'
	SET @sql =	@sql +	N' 		,creation_ts'
	SET @sql =	@sql +	N' 		,modification_time'
	SET @sql =	@sql +	N' 		,[description]'
	SET @sql =	@sql +	N' 		,[filename]'
	SET @sql =	@sql +	N' 		,mimetype'
	SET @sql =	@sql +	N' 		,submitter_id'
	SET @sql =	@sql +	N' 		,filesize'
	SET @sql =	@sql +	N' 		,isobsolete'
	SET @sql =	@sql +	N' 		,ispatch'
	SET @sql =	@sql +	N' 		,isprivate'
	SET @sql =	@sql +	N' 		,isurl'
	SET @sql =	@sql +	N' 		,null'
	SET @sql =	@sql +	N' 		,null'
	SET @sql =	@sql +	N' 		,null'
	SET @sql =	@sql +	N' from attachments'



	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''attachments'' migrated to ''attachment''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;
	




------------------------------------------------------
-- MIGRATION ATTACH_DATA
------------------------------------------------------

CREATE procedure Migration_attach_data
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.attach_data'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N'	 id'
	SET @sql = @sql +	N'	,thedata'
	SET @sql = @sql +	N' from attach_data'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''attach_data'' migrated to ''attach_data''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;



------------------------------------------------------
-- MIGRATION DEPENDENCY
------------------------------------------------------

CREATE procedure Migration_dependencies
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.bug_relationship'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 blocked'
	SET @sql = @sql +	N' 	,dependson'
	SET @sql = @sql +	N' 	,2'					--2번 은 child of type.
	SET @sql = @sql +	N' from dependencies'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''dependencies'' migrated to ''bug_relationship''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION DUPLICATION
------------------------------------------------------
CREATE procedure Migration_duplicates
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.bug_relationship'
	SET @sql = @sql +	N' select'
	SET @sql = @sql +	N' 	 dupe'  
	SET @sql = @sql +	N' 	,dupe_of'
	SET @sql = @sql +	N' 	,0'
	SET @sql = @sql +	N' from duplicates'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''duplicates'' migrated to ''bug_relationship''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION RELATIONSHIP
------------------------------------------------------
CREATE procedure Migration_relationships
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.bug_relationship'
	SET @sql = @sql +	N' select'
	SET @sql = @sql +	N' 	 src_id'  
	SET @sql = @sql +	N' 	,dest_id'
	SET @sql = @sql +	N' 	,4'
	SET @sql = @sql +	N' from relationships'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''relationships'' migrated to ''bug_relationship''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;



------------------------------------------------------
-- MIGRATION CC LIST
------------------------------------------------------
CREATE procedure Migration_cc
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.cc'
	SET @sql = @sql +	N' select'
	SET @sql = @sql +	N' 	 bug_id'
	SET @sql = @sql +	N' 	,who'
	SET @sql = @sql +	N' from cc'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''cc'' migrated to ''cc''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION SEE ALSO
------------------------------------------------------
CREATE procedure Migration_see_also
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.see_also'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 bug_id'
	SET @sql = @sql +	N' 	,value'
	SET @sql = @sql +	N' from bug_see_also'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''bug_see_also'' migrated to ''see_also''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION VOTE
------------------------------------------------------
CREATE procedure Migration_vote
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.vote'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 bug_id'
	SET @sql = @sql +	N' 	,who'
	SET @sql = @sql +	N' 	,vote_count'
	SET @sql = @sql +	N' from votes'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	----PRINT N'Table ''votes'' migrated to ''vote''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION USER
------------------------------------------------------
CREATE procedure Migration_profiles
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.[user]'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 id'
	SET @sql = @sql +	N' 	,login_name'
	SET @sql = @sql +	N' 	,realname'
	SET @sql = @sql +	N' 	,timezone'
	SET @sql = @sql +	N' 	,create_ts'
	SET @sql = @sql +	N' 	,delta_ts'
	SET @sql = @sql +	N' from profiles'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''profiles'' migrated to ''user''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;


------------------------------------------------------
-- MIGRATION CLASSFICATION
------------------------------------------------------
CREATE procedure Migration_classifications
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.classification'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 id'
	SET @sql = @sql +	N' 	,sortkey'
	SET @sql = @sql +	N' 	,name'
	SET @sql = @sql +	N' 	,[description]'
	SET @sql = @sql +	N' from classifications'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''classifications'' migrated to ''classification''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;



------------------------------------------------------
-- MIGRATION PRODUCT
------------------------------------------------------
CREATE procedure Migration_products
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.product'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	id'
	SET @sql = @sql +	N' 	,name'
	SET @sql = @sql +	N' 	,classification_id'
	SET @sql = @sql +	N' 	,[description]'
	SET @sql = @sql +	N' 	,CASE disallownew'
	SET @sql = @sql +	N'          WHEN 0 THEN 1'
	SET @sql = @sql +	N'          WHEN 1 THEN 0'
	SET @sql = @sql +	N'      END	as [enabled]'
	SET @sql = @sql +	N' 	,0	as initialcomponent'
	SET @sql = @sql +	N' from products'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''products'' migrated to ''product''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;





------------------------------------------------------
-- MIGRATION COMPONENT
------------------------------------------------------
CREATE procedure Migration_components
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.component'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	id'
	SET @sql = @sql +	N' 	,product_id'
	SET @sql = @sql +	N' 	,name'
	SET @sql = @sql +	N' 	,description'
	SET @sql = @sql +	N' 	,initialowner'
	SET @sql = @sql +	N' 	,initialqacontact'
	SET @sql = @sql +	N' 	,null				as status'
	SET @sql = @sql +	N' from components'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''components'' migrated to ''component''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION TAGS
------------------------------------------------------
CREATE procedure Migration_keyworddefs
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.tag'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 id'
	SET @sql = @sql +	N' 	,name'
	SET @sql = @sql +	N' 	,[description]'
	SET @sql = @sql +	N' from keyworddefs'




	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''keyworddefs'' migrated to ''tag''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;



------------------------------------------------------
-- MIGRATION BUG_TAGS
------------------------------------------------------
CREATE procedure Migration_keywords
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.bug_tag'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 bug_id'
	SET @sql = @sql +	N' 	,keywordid'
	SET @sql = @sql +	N' from keywords'

	
	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''keywords'' migrated to ''bug_tag''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION VERSION
------------------------------------------------------
CREATE procedure Migration_versions
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.[version]'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 id				as sortkey'
	SET @sql = @sql +	N' 	,product_id'
	SET @sql = @sql +	N' 	,value'
	SET @sql = @sql +	N' from versions'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''versions'' migrated to ''version''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION SEVERITY
------------------------------------------------------
CREATE procedure Migration_bug_severity
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.severity'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 id'
	SET @sql = @sql +	N' 	,sortkey'
	SET @sql = @sql +	N' 	,value'
	SET @sql = @sql +	N' 	,isactive'
	SET @sql = @sql +	N' 	,visibility_value_id'
	SET @sql = @sql +	N' from bug_severity'




	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''bug_severity'' migrated to ''severity''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;



------------------------------------------------------
-- MIGRATION STATUS
------------------------------------------------------
CREATE procedure Migration_bug_status
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.[status]'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 id'
	SET @sql = @sql +	N' 	,sortkey'
	SET @sql = @sql +	N' 	,value'
	SET @sql = @sql +	N' 	,is_open'
	SET @sql = @sql +	N' 	,isactive'
	SET @sql = @sql +	N' 	,visibility_value_id'
	SET @sql = @sql +	N' from bug_status'

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''bug_status'' migrated to ''status''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;



------------------------------------------------------
-- MIGRATION OS
------------------------------------------------------
CREATE procedure Migration_op_sys
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.os'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 sortkey'
	SET @sql = @sql +	N' 	,value'
	SET @sql = @sql +	N' 	,isactive'
	SET @sql = @sql +	N' 	,visibility_value_id'
	SET @sql = @sql +	N' from op_sys'




	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''op_sys'' migrated to ''os''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION PRIORITY
------------------------------------------------------
CREATE procedure Migration_priority
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.[priority]'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 id'
	SET @sql = @sql +	N' 	,sortkey'
	SET @sql = @sql +	N' 	,value'
	SET @sql = @sql +	N' 	,isactive'
	SET @sql = @sql +	N' 	,visibility_value_id'
	SET @sql = @sql +	N' from [op_sys]'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''priority'' migrated to ''priority''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;





------------------------------------------------------
-- MIGRATION PLATFORM
------------------------------------------------------
CREATE procedure Migration_rep_platform
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.[platform]'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 sortkey'
	SET @sql = @sql +	N' 	,value'
	SET @sql = @sql +	N' 	,isactive'
	SET @sql = @sql +	N' 	,visibility_value_id'
	SET @sql = @sql +	N' from rep_platform'
	

	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''rep_platform'' migrated to ''platform''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;





------------------------------------------------------
-- MIGRATION RESOLUTION
------------------------------------------------------
CREATE procedure Migration_resolution
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.resolution'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 id'
	SET @sql = @sql +	N' 	,sortkey'
	SET @sql = @sql +	N' 	,value'
	SET @sql = @sql +	N' 	,isactive'
	SET @sql = @sql +	N' 	,visibility_value_id'
	SET @sql = @sql +	N' from resolution'

	
	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''resolution'' migrated to ''resolution''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




------------------------------------------------------
-- MIGRATION MILESTONE
------------------------------------------------------
CREATE procedure Migration_milestones
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.milestone'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 sortkey'
	SET @sql = @sql +	N' 	,product_id'
	SET @sql = @sql +	N' 	,value'
	SET @sql = @sql +	N' from milestones'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''milestones'' migrated to ''milestone''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;






------------------------------------------------------
-- MIGRATION BUG_FULLTEXT
------------------------------------------------------
CREATE procedure Migration_bugs_fulltext
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.bug_fulltext'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 bug_id'
	SET @sql = @sql +	N' 	,short_desc'
	SET @sql = @sql +	N' 	,comments'
	SET @sql = @sql +	N' 	,comments_noprivate'
	SET @sql = @sql +	N' from bugs_fulltext'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''bugs_fulltext'' migrated to ''bug_fulltext''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;





------------------------------------------------------
-- MIGRATION STATUS_WORKFLOW
------------------------------------------------------
CREATE procedure Migration_status_workflow
(
	@dbname as NVARCHAR(64)
)
as
BEGIN
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) 
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--Query문 생성
	SET @sql =			N' insert into ' + @dbname + '.dbo.status_workflow'
	SET @sql = @sql +	N' select '
	SET @sql = @sql +	N' 	 new_status'
	SET @sql = @sql +	N' 	,old_status'
	SET @sql = @sql +	N' 	,require_comment'
	SET @sql = @sql +	N' from status_workflow'


	EXEC sp_executesql @sql, @params, @resCnt OUTPUT

	--PRINT N'Table ''status_workflow'' migrated to ''status_workflow''! Inserted ' + cast(@@RowCount as varchar) + ' rows'
	RETURN @@RowCount
END;;




--EXEC  Migration_bugs 			@dbname=N'Bug_Mozilla2'
--EXEC  Migration_longdescs	 	@dbname=N'Bug_Mozilla2'
--EXEC  Migration_bugnote_type 	@dbname=N'Bug_Mozilla2'
--EXEC  Migration_duplicates 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_cc 				@dbname=N'Bug_Mozilla2'
--EXEC  Migration_see_also 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_vote 			@dbname=N'Bug_Mozilla2'
--EXEC  Migration_profiles 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_classifications @dbname=N'Bug_Mozilla2'
--EXEC  Migration_products 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_components 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_keyworddefs 	@dbname=N'Bug_Mozilla2'
--EXEC  Migration_keywords 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_versions 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_bug_severity 	@dbname=N'Bug_Mozilla2'
--EXEC  Migration_bug_status 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_op_sys 			@dbname=N'Bug_Mozilla2'
--EXEC  Migration_priority		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_rep_platform 	@dbname=N'Bug_Mozilla2'
--EXEC  Migration_resolution 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_milestones 		@dbname=N'Bug_Mozilla2'
--EXEC  Migration_bugs_fulltext 	@dbname=N'Bug_Mozilla2'
--EXEC  Migration_status_workflow @dbname=N'Bug_Mozilla2'