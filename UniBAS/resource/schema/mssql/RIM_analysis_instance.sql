--------------------------------------------------------------
-- 분석 스키마 정보저장.
--------------------------------------------------------------
CREATE PROCEDURE saveAnalysisInfo
(
	 @TITLE			AS NVARCHAR(256)	
	,@DESCRIPTION	AS NVARCHAR(256)	
	,@SCHEMA_NAME	AS NVARCHAR(256)	
	,@IS_UNIFORMLY	AS BIT				= 1
	,@START_DATE	AS DATETIME			= null
	,@END_DATE		AS DATETIME			= null
	,@CONDITION		AS NVARCHAR(MAX)	= null
)
AS
BEGIN
	DECLARE @ANALYSIS_ID	AS integer

	IF not exists (SELECT * FROM analysis WHERE title = @TITLE OR [schema_name] = @SCHEMA_NAME)
	BEGIN
		INSERT INTO analysis VALUES	(@TITLE, @DESCRIPTION, @SCHEMA_NAME, @IS_UNIFORMLY, @START_DATE, @END_DATE, @CONDITION)
		SET @ANALYSIS_ID = @@IDENTITY

		INSERT INTO analysis_history VALUES (@ANALYSIS_ID, 1, SYSDATETIME(), 'analysis', 'id', null,'NEW')
	END

	ELSE
	BEGIN
		RETURN -1
		--UPDATE A SET 
		--	 [DESCRIPTION]	=@DESCRIPTION
		--	,[IS_UNIFORMLY]	=@IS_UNIFORMLY
		--	,[START_DATE]	=@START_DATE
		--	,[END_DATE]		=@END_DATE
		--	,[CONDITION]	=@CONDITION
		--FROM analysis A
		--WHERE [TITLE] = @TITLE
		--PRINT N'WARNNING::중복된 정보입니다.'
	END	

	RETURN @ANALYSIS_ID
END;;




--------------------------------------------------------------
-- 전체 데이터 이동.
--------------------------------------------------------------
CREATE PROCEDURE moveAnalysis
(
	 @SCHEMA_NAME	as NVARCHAR(512)
	,@SITE_ID		as integer
	,@PROJECT_ID	as NVARCHAR(Max)	= null
	,@start_date	as date				= null
	,@end_date		as date				= null
	,@condition		as NVARCHAR(Max)	= null
)
AS
BEGIN
	DECLARE @cond as nvarchar(1024)	--보조 조건 변수	

	SET @cond = N'id in (' + CAST(@SITE_ID AS NVARCHAR) + N')'

	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'site'					, @condition = @cond
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'analysis_query'
	
	--프로젝트를 먼저 옮김.
	SET @cond = N'id in (' + @PROJECT_ID + N')'
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'project'	, @site_id=@SITE_ID, @condition = @cond


	--버그 테이블 : 필터를 정확하게 거쳐서 옮겨야함.  (인자로 받은 condition은 버그에만 해당함)
	DECLARE @condString  as NVARCHAR(max) 
	EXEC getConditionString @SITE_ID = 6, @Condition = @condition, @retStr=@condString OUTPUT
	EXEC moveAnalysis_Element @SCHEMA_NAME, N'bug', @SITE_ID, @PROJECT_ID, @start_date, @end_date, @condString


	--이동된 프로젝트에 대한 컴포넌트 이동.
	SET @cond = N'project_id in (SELECT id FROM ' + @SCHEMA_NAME +N'.dbo.project WHERE site_id = ' + CAST(@SITE_ID as nvarchar(32)) + N')'
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'component'		, @site_id=@SITE_ID, @condition = @cond
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'version'			, @site_id=@SITE_ID, @condition = @cond
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'milestone'		, @site_id=@SITE_ID, @condition = @cond

	
	--이동된 버그 아이디에 대해서 이동.(이미 다 옮겨졌으니까 프로젝트 아이디는 추가로 필요없음)
	SET @cond = N'bug_id in (SELECT id FROM ' + @SCHEMA_NAME +N'.dbo.bug WHERE site_id = ' + CAST(@SITE_ID as nvarchar(32)) + N')'

	
	--선택된 버그 아이디에 따라서 이동.
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'comment'			, @site_id=@SITE_ID, @condition = @cond
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'attachment'		, @site_id=@SITE_ID, @condition = @cond
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'history'			, @site_id=@SITE_ID, @condition = @cond
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'reference'		, @site_id=@SITE_ID, @condition = @cond
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'monitor'			, @site_id=@SITE_ID, @condition = @cond
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'bug_keyword'		, @site_id=@SITE_ID, @condition = @cond
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'additional_info'	, @site_id=@SITE_ID, @condition = @cond

	--선택된 버그 아이디에 따라서 이동하지만 명칭이 다름
	SET @cond = N'src_bug_id in (SELECT id FROM ' + @SCHEMA_NAME +N'.dbo.bug WHERE site_id = ' + CAST(@SITE_ID as nvarchar(32)) + N')'
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'relationship'		, @site_id=@SITE_ID, @condition = @cond	

	
	--선택된 첨부파일에 대해서 이동
	SET @cond = N'attach_id in (select id from ' + @SCHEMA_NAME +N'.dbo.attachment where site_id = '+ CAST(@SITE_ID as nvarchar(32)) + N')'
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'attach_data'		, @site_id=@SITE_ID, @condition=@cond

	--유저도 일부만 옮길 수도 있으나 복잡하므로 그냥 다 옮김.
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'user'				, @site_id=@SITE_ID


	--전체 내용을 옮기는 테이블들.
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'keyword'			, @site_id=@SITE_ID
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'status'			, @site_id=@SITE_ID
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'resolution'		, @site_id=@SITE_ID
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'severity'		, @site_id=@SITE_ID
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'priority'		, @site_id=@SITE_ID
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'platform'		, @site_id=@SITE_ID
	EXEC moveAnalysis_Element @schema=@SCHEMA_NAME, @table_name=N'os'				, @site_id=@SITE_ID

	RETURN 1
END;;




--------------------------------------------------------------
-- 한 테이블에 대한 이동함수
--------------------------------------------------------------
CREATE PROCEDURE moveAnalysis_Element
(
	 @schema		as NVARCHAR(512)
	,@table_name	as NVARCHAR(512)
	,@site_id		as integer			= null
	,@project_id	as NVARCHAR(max)	= null
	,@start_date	as date				= null
	,@end_date		as date				= null
	,@condition		as NVARCHAR(MAX)	= null
)
AS 
BEGIN 
	--입력 인자 검사.
	IF @schema is NULL		RETURN -1
	IF @table_name is NULL	RETURN -1
	IF @site_id is NULL		RETURN -1
	IF @start_date is not null and @end_date is not null and @start_date>@end_date	RETURN -1

	--계산을 위한 날짜 변경
	IF @end_date is not null	SET @end_date = dateadd(day,1,@end_date)

	--비정상적인 컨디션 처리
	IF @condition is not null and @condition = ''	SET @condition = null


	-- 데이터베이스 있는지 검사.
	IF NOT EXISTS (SELECT name from sys.databases where name = @schema)
	BEGIN
		PRINT 'DOESN''T EXISTS SCHEMA NAME' + @SCHEMA
		RETURN -1
	END
	
	-- 테이블 있는지 검사--------------------------------------------------------------------------------------
	DECLARE @sql	as NVARCHAR(MAX)
	DECLARE @where	as NVARCHAR(MAX)	= N''
	DECLARE @params as nvarchar(256)	= N'@name as NVARCHAR(128) OUTPUT'	--OUTPUT에 주의
	DECLARE @ret	as integer
	DECLARE @name	as NVARCHAR(64)

	SET @sql = 'SELECT @name = name from '+@schema+'.sys.tables where name = '''+@table_name+''''
	EXEC @ret = sp_executesql @sql, @params, @name OUTPUT
	IF (@name is null)
	BEGIN
		PRINT 'DOESN''T EXISTS Table NAME' + @table_name
		RETURN -1
	END

	--sql 생성.-----------------------------------------------------------------------------------------------------------------------------------------
	SET @sql =			N'INSERT INTO '+ @schema+ N'.dbo.['+@table_name+'] '
	SET @sql = @sql +	N' SELECT * FROM ['+@table_name+'] '
	IF @site_id is not null SET @where = ' and site_id = '+ CAST(@site_id AS NVARCHAR(32))
	IF @project_id is not null
		SET @where = @where + N' and project_id in ('+ @project_id + N') '
	
	
	--날짜에 대한 쿼리 생성.
	IF(@start_date is not null or @end_date is not null) 
	BEGIN
		IF(@start_date is not null and @end_date is not null)	SET @where = @where + N' and  creation_ts >= ''' + CAST(@start_date AS NVARCHAR)	+ N''' and creation_ts < '''+CAST(@end_date AS NVARCHAR) +N''''
		IF(@start_date is not null and @end_date is null)		SET @where = @where + N' and  creation_ts >= ''' + CAST(@start_date AS NVARCHAR)	+ N''''
		IF(@start_date is null and @end_date is not null)		SET @where = @where + N' and  creation_ts <  ''' + CAST(@end_date AS NVARCHAR)		+ N''''
	END

	--CONDITION 에 대한 쿼리 생성.
	IF(@condition is not null) 
		SET @where = @where + N' and '+@condition

	IF len(@where) > 0
	BEGIN
		IF substring(@WHERE, 1, 4) = N' and'
			SET @where = N'WHERE '+ substring(@where, 5, len(@where))
	END 
	SET @sql = @sql + @where

	--sql 실행
	DECLARE @resCnt as integer
	SET @params = N'@resCnt as int OUTPUT'	--OUTPUT에 주의
	PRINT @sql
	EXEC @ret = sp_executesql @sql, @params, @resCnt OUTPUT
	RETURN @ret
END;;


---------------------------------------------------------
-- condition들을 조건문으로 변경
---------------------------------------------------------
CREATE PROCEDURE getConditionString
(
	 @SITE_ID	AS INTEGER
	,@condition AS NVARCHAR(MAX)
	,@retStr    as Nvarchar(MAX) OUTPUT
)
AS
BEGIN

	Declare COND_CURSOR CURSOR FOR 
	SELECT Item FROM getStringArray(';',@condition)
	
	--기본값 설정
	DECLARE @cond  as NVARCHAR(256) = N''
	DECLARE @conStr  as NVARCHAR(max) = N''
	SET @retStr = N''
		
	--커서 시작
	OPEN COND_CURSOR
	FETCH NEXT FROM COND_CURSOR	INTO @cond
	WHILE @@FETCH_STATUS = 0
	BEGIN
		EXEC getOneCondition @SITE_ID, @cond, @conStr OUTPUT
		SET @retStr = @retStr + @conStr
		FETCH NEXT FROM COND_CURSOR	INTO @cond
		IF @@FETCH_STATUS=0
			SET @retStr = @retStr + N' and '
	END
	
	CLOSE COND_CURSOR
	DEALLOCATE COND_CURSOR

	RETURN
END;;

--------------------------------------------------------------
-- 한 조건에 대한 쿼리 반환.
--------------------------------------------------------------
CREATE PROCEDURE getOneCondition
(
	 @SITE_ID	AS INTEGER
	,@condition AS NVARCHAR(MAX)
	,@retStr    as Nvarchar(MAX) OUTPUT
)
AS
BEGIN
	--변수선언
	DECLARE @table_name as nvarchar(128)
	DECLARE @field_name as nvarchar(128)
	DECLARE @function   as nvarchar(128)
	DECLARE @value		as nvarchar(128)
	DECLARE @oper		as nvarchar(128)
	
	--초기값 설정
	SET @retStr = ''

	--조건 분리
	--DECLARE @CONDITION		AS NVARCHAR(MAX)	= N'comment.count(*) > 10'
	exec splitCondition  @condition, @table_name OUTPUT,@field_name OUTPUT,@function OUTPUT,@oper OUTPUT,@value OUTPUT
	IF (@table_name='' OR @oper = '')
	BEGIN
		PRINT 'Invalid Condition : syntax error'
		RETURN
	END






	--테이블 검사
	IF NOT EXISTS (select name from sys.tables where name = @table_name)
	BEGIN
		PRINT 'Invalid Condition : unknown table'
		RETURN 
	END

	--필드명이 있으면 필드명 검사, 없으면 BUG_ID 필드명 할당
	IF(@field_name <> '')
	BEGIN
		--필드명 검사
		IF NOT EXISTS (select name from sys.all_columns where  [object_id] = (select [object_id] from sys.tables where name = @table_name) and name = @field_name)
		BEGIN
			PRINT 'Invalid Condition : unknown field'
			RETURN 
		END	
	END

	--필드명 설정.
	DECLARE @field_bug as nvarchar(128)
	IF @table_name = N'relationship'	SET @field_bug = N'src_bug_id'
	ELSE								SET @field_bug = N'bug_id'



	--쿼리생성.
	DECLARE @sql as nvarchar(max)
	
	IF (@table_name = 'bug')
	BEGIN
		SET @sql = N' [' + @field_name + N'] ' + @oper + N' ' + @value
	END
	ELSE IF (@FUNCTION = '')
	BEGIN
		SET @sql = N'SELECT ['+@field_bug + N'] from ['+@table_name + N'] c '
		SET @sql = @sql + N'WHERE site_id = '+CAST(@SITE_ID AS NVARCHAR(32)) + N' and ['+@field_name + N'] '+@oper+N' '+@value
		SET @sql = N' id in (' + @sql + N')'
	END
	ELSE
	BEGIN
		SET @sql = N'SELECT ['+@field_bug + N'] from ['+@table_name + N'] c '
		SET @sql = @sql + N'WHERE site_id = '+CAST(@SITE_ID AS NVARCHAR(32)) + N' GROUP BY ['+@field_bug + N'] HAVING '+ @function + N' '+@oper+N' '+@value
		SET @sql = N' id in (' + @sql + N')'
	END

	--결과반환
	--PRINT @sql
	SET @retStr = @sql

	RETURN
END;;





---------------------------------------------------------
-- 컨디션을 분리하여 항목으로 나눔
---------------------------------------------------------
CREATE PROCEDURE splitCondition
(
	 @condition		AS NVARCHAR(MAX)
	,@table_name	AS NVARCHAR(128) OUTPUT
	,@field_name	AS NVARCHAR(128) OUTPUT
	,@function		AS NVARCHAR(128) OUTPUT
	,@oper			AS NVARCHAR(128) OUTPUT
	,@value			AS NVARCHAR(128) OUTPUT
)
AS
BEGIN
	--기본값 설정
	SET @table_name	= N''
	SET @field_name	= N''
	SET @function	= N''
	SET @oper		= N''
	SET @value		= N''
			
	--조건 분리
	BEGIN TRY
		DECLARE @idx as integer
		SET @idx = CHARINDEX('.',@condition,1)
		SET @table_name = substring(@condition, 1,@idx-1)
		SET @field_name = substring(@condition, @idx+1,len(@condition))


					  SET @idx = charindex(N' ',@field_name)
		IF (@idx = 0) SET @idx = charindex(N'<',@field_name) 
		IF (@idx = 0) SET @idx = charindex(N'>',@field_name) 
		IF (@idx = 0) SET @idx = charindex(N'=',@field_name) 
	
		SET @value = ltrim(rtrim(substring(@field_name, @idx,len(@field_name))))
		SET @field_name = ltrim(rtrim(substring(@field_name, 1,@idx-1)))
	
					  SET @idx = charindex(N' ',@value)
		IF (@idx = 0) SET @idx = charindex(N'=',@value)
		IF (@idx = 0) SET @idx = charindex(N'>',@value)
		IF (@idx = 0) SET @idx = charindex(N'<',@value)
	
		SET @oper = ltrim(rtrim(substring(@value, 1,@idx)))
		SET @value = ltrim(rtrim(substring(@value, @idx+1,len(@value))))
	

		DECLARE @idx2 as integer
		SET @idx = charindex(N'(',@field_name)
		SET @idx2 = charindex(N')',@field_name)
		IF( @idx >0 and @idx2 > 0 and @idx2> @idx)
		BEGIN
			SET @function = ltrim(rtrim(substring(@field_name, 1,@idx)))
			SET @function = @function + N'*'+ ltrim(rtrim(substring(@field_name, @idx2,10)))
			SET @field_name = ltrim(rtrim(substring(@field_name, @idx+1, @idx2-@idx-1)))

			IF @field_name='*' SET @field_name = ''
		END
	
	END TRY
	BEGIN CATCH
		PRINT N'ERRPR : '+ @condition
		SET @table_name	= N''
		SET @field_name	= N''
		SET @function	= N''
		SET @oper		= N''
		SET @value		= N''
	END CATCH

	RETURN
END;;


------------------------------------------------------------------------------
-- 사이트의 통계 정보 생성  (지정된 analysis, 사이트에 대해서 수행)
------------------------------------------------------------------------------
--CREATE PROCEDURE makeAnalysisSummary
--(
--	 @analysis_id	as integer
--	,@site_id		as integer
--	,@project_id	as NVARCHAR(max)
--)
--AS
--BEGIN
--	IF @analysis_id is null RETURN -1
--	IF @site_id is null		RETURN -1
--	IF @analysis_id is null RETURN -1

--	BEGIN TRANSACTION

--	insert into  analysis_project
--	SELECT 
--		 @analysis_id	as analysis_id
--		,@site_id		as site_id
--		,p.Item			as project_id
--		,(SELECT min(creation_ts) FROM Analysis_Bug1..bug where site_id = @site_id and project_id = p.Item) as [start_date]
--		,(SELECT max(creation_ts) FROM Analysis_Bug1..bug where site_id = @site_id and project_id = p.Item) as end_date
--		,(SELECT condition FROM analysis) as condition
--		,(SELECT count(id) FROM Analysis_Bug1..bug where site_id = @site_id and project_id = p.Item) as BugCNT
--		,(SELECT count([uid])	AS users 
--			FROM (	SELECT DISTINCT reporter_id	AS [uid]	FROM Analysis_Bug1..[bug]		WHERE site_id = @site_id and project_id = p.Item UNION 
--					SELECT DISTINCT assignee_id				FROM Analysis_Bug1..[bug]		WHERE site_id = @site_id and project_id = p.Item UNION 
--					SELECT DISTINCT qa_id					FROM Analysis_Bug1..[bug]		WHERE site_id = @site_id and project_id = p.Item UNION
--					SELECT DISTINCT submitter_id			FROM Analysis_Bug1..comment		WHERE site_id = @site_id and bug_id in (SELECT id FROM Analysis_Bug1..bug WHERE site_id = @site_id and project_id = p.Item) UNION
--					SELECT DISTINCT submitter_id			FROM Analysis_Bug1..attachment	WHERE site_id = @site_id and bug_id in (SELECT id FROM Analysis_Bug1..bug WHERE site_id = @site_id and project_id = p.Item) UNION
--					SELECT DISTINCT who_id					FROM Analysis_Bug1..history		WHERE site_id = @site_id and bug_id in (SELECT id FROM Analysis_Bug1..bug WHERE site_id = @site_id and project_id = p.Item) UNION
--					SELECT DISTINCT [user_id]				FROM Analysis_Bug1..monitor		WHERE site_id = @site_id and bug_id in (SELECT id FROM Analysis_Bug1..bug WHERE site_id = @site_id and project_id = p.Item) 
--				) A	--각 테이블별 프로젝트 내에서 관련된  User를 추출.
--		 ) UserCNT
--	FROM getIntArray(',', @PROJECT_ID) p		--project_id list
	
--	COMMIT
--	RETURN 1
--END;;

------------------------------------------------------------------------------
-- 사이트의 통계 정보 생성  (지정된 analysis, 사이트에 대해서 수행)
------------------------------------------------------------------------------
CREATE PROCEDURE makeAnalysisSummary
(
	 @analysis_id	as integer
	,@Schema_Name	as NVARCHAR(max)
	,@site_id		as integer
	,@project_id	as NVARCHAR(max) = null
)
AS
BEGIN
	IF @analysis_id is null RETURN -1
	IF @site_id is null		RETURN -1
	
	
	DECLARE @sql       as NVARCHAR(MAX )
    DECLARE @ret       as integer       

	SET @sql =		  N' INSERT INTO  analysis_project '
	SET @sql = @sql + N' SELECT '
	SET @sql = @sql + N' 	'+CAST(@analysis_id AS nvarchar(16)) + N'	as analysis_id'
	SET @sql = @sql + N' 	,'+cast(@site_id AS nvarchar(16)) + N'		as site_id'
	SET @sql = @sql + N' 	,p.id			as project_id'
	SET @sql = @sql + N' 	,(SELECT min(creation_ts) FROM '+@Schema_Name + N'..bug where site_id = ' +cast(@site_id AS nvarchar(16))+ N' and project_id = p.id) as [start_date]'
	SET @sql = @sql + N' 	,(SELECT max(creation_ts) FROM '+@Schema_Name + N'..bug where site_id = ' +cast(@site_id AS nvarchar(16))+ N' and project_id = p.id) as end_date'
	SET @sql = @sql + N' 	,(SELECT condition FROM analysis WHERE ID = ' + CAST(@analysis_id AS nvarchar(16)) +N') as condition'
	SET @sql = @sql + N' 	,(SELECT count(id) FROM '+@Schema_Name + N'..bug where site_id = '+cast(@site_id AS nvarchar(16))+N' and project_id = p.id) as BugCNT'
	SET @sql = @sql + N' 	,(SELECT count([uid])	AS users '
	SET @sql = @sql + N' 		FROM (	SELECT DISTINCT reporter_id	AS [uid]	FROM '+@Schema_Name + N'..[bug]			WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and project_id = p.id UNION'
	SET @sql = @sql + N' 				SELECT DISTINCT assignee_id				FROM '+@Schema_Name + N'..[bug]			WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and project_id = p.id UNION'
	SET @sql = @sql + N' 				SELECT DISTINCT qa_id					FROM '+@Schema_Name + N'..[bug]			WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and project_id = p.id UNION'
	SET @sql = @sql + N' 				SELECT DISTINCT submitter_id			FROM '+@Schema_Name + N'..comment		WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and bug_id in (SELECT id FROM '+@Schema_Name + N'..bug WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and project_id = p.id) UNION'
	SET @sql = @sql + N' 				SELECT DISTINCT submitter_id			FROM '+@Schema_Name + N'..attachment	WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and bug_id in (SELECT id FROM '+@Schema_Name + N'..bug WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and project_id = p.id) UNION'
	SET @sql = @sql + N' 				SELECT DISTINCT who_id					FROM '+@Schema_Name + N'..history		WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and bug_id in (SELECT id FROM '+@Schema_Name + N'..bug WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and project_id = p.id) UNION'
	SET @sql = @sql + N' 				SELECT DISTINCT [user_id]				FROM '+@Schema_Name + N'..monitor		WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and bug_id in (SELECT id FROM '+@Schema_Name + N'..bug WHERE site_id = ' +cast(@site_id AS nvarchar(16))+ N' and project_id = p.id) '
	SET @sql = @sql + N' 			) A	'--각 테이블별 프로젝트 내에서 관련된  User를 추출.
	SET @sql = @sql + N' 	 ) UserCNT'
	SET @sql = @sql + N' FROM project p'
	SET @sql = @sql + N' Where site_id = '+cast(@site_id AS nvarchar(16))

	IF @project_id is NOT null
		SET @sql = @sql + N' and id in ('+@project_id+N')' 

	EXEC @ret = sp_executesql @sql , N''

	IF (@ret <>0)
	BEGIN
		PRINT 'ERROR'
		RETURN -1
	END

	RETURN 1
END;;


---------------------------------------------------------
-- 문자열을 분리해주는 함수
---------------------------------------------------------
CREATE FUNCTION getStringArray
(
	 @delimiter		as NVARCHAR(2) = null
	,@strItems		as NVARCHAR(MAX)
)
RETURNS @Items TABLE 
(
    Item	NVARCHAR(256)
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
		SET @delimiter = N';'
	
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
			IF NOT EXISTS (SELECT Item FROM @Items Where Item = @str)
				INSERT INTO @Items VALUES (@str)
		END

		SET @s = @e+@sp_len
	END

	RETURN 
END;;



------------------------------------------------------------------------------
-- 분석 데이터베이스 삭제
------------------------------------------------------------------------------
CREATE PROCEDURE deleteAnalysis
(
	 @analysis_id	as integer
)
AS
BEGIN
	IF @analysis_id is null RETURN -1

	IF not exists (select * from [analysis] where id = @analysis_id)
	BEGIN
		PRINT 'No Analysis ID'
		RETURN -1
	END
	
	--analysis 데이터베이스 삭제--------------------------------------------------------
	DECLARE @resCnt			AS INTEGER
	DECLARE @sql			AS NVARCHAR(MAX)
	DECLARE @params			AS NVARCHAR(256) = '@resCnt as int OUTPUT'	--OUTPUT에 주의
	DECLARE @schema_name	AS NVARCHAR(128)
	DECLARE @ret			AS INTEGER
	SELECT @schema_name = [schema_name]  from [analysis] where id = @analysis_id


	------   단일사용자 모드로 변경 후 ---------------	
	SET @sql = N'ALTER DATABASE '+ @schema_name +' SET  SINGLE_USER WITH ROLLBACK IMMEDIATE'
	EXEC @ret = sp_executesql @sql, @params, @resCnt OUTPUT
	IF @ret <> 0	--sp_executesql은 리턴이 0이면 정상실행 아니면 에러.
	BEGIN
		PRINT 'ERROR_CODE = ' + cast(@ret as nvarchar(128))
		RETURN -1
	END

	------  DB 삭제 ---------------
	SET @sql = N'drop database  ' + @schema_name 
	EXEC @ret = sp_executesql @sql, @params, @resCnt OUTPUT

	IF @ret <> 0	--sp_executesql은 리턴이 0이면 정상실행 아니면 에러.
	BEGIN
		PRINT 'ERROR_CODE = ' + cast(@ret as nvarchar(128))
		RETURN -1
	END

	-- 부가 정보 삭제---------------------------------------------------------	
	delete from [analysis_history] where analysis_id = @analysis_id	
	delete from [analysis_project] where analysis_id = @analysis_id
	delete from [analysis] where id = @analysis_id
END;;


-----------------------------------------------
---- 프로젝트의 아이디 목록 구하기
-----------------------------------------------
--CREATE PROCEDURE getProjectString
--(
--	  @SCHEMA_NAME	as NVARCHAR(512)
--	  ,@site_id     as integer
--)
--AS
--BEGIN
--	-- 테이블 있는지 검사--------------------------------------------------------------------------------------
--	DECLARE @sql	as NVARCHAR(MAX)
--	DECLARE @params as nvarchar(256)	= N'@resCnt as INTEGER OUTPUT'	--OUTPUT에 주의
--	DECLARE @ret	as integer
--	DECLARE @resCnt	as NVARCHAR(64)

--	SET @sql = N'Declare ID_CURSOR CURSOR FOR SELECT id FROM '+@SCHEMA_NAME+N'.dbo.project WHERE site_id = '+@site_id
--	EXEC @ret = sp_executesql @sql, @params, @resCnt OUTPUT
--	IF (@ret <> 0)
--		RETURN ''
	


--	--기본값 설정
--	DECLARE @str  as NVARCHAR(MAX) = ''
--	Declare @id as integer
		
--	--커서 시작
--	OPEN ID_CURSOR
--	FETCH NEXT FROM ID_CURSOR	INTO @id

--	WHILE @@FETCH_STATUS = 0
--	BEGIN
		
--		SET @str = @str + convert(NVARCHAR(64),@id)

--		FETCH NEXT FROM ID_CURSOR	INTO @id
		
--		IF (@@FETCH_STATUS = 0) 
--			SET @str = @str + N','
--	END
	
--	CLOSE ID_CURSOR
--	DEALLOCATE ID_CURSOR

--	RETURN @str
--END;;

