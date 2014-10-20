----------------------------------------
-- Target Info 테이블 생성
----------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'site')
BEGIN
	CREATE TABLE [site]
	(
		 id					int				IDENTITY(1,1)				-- key, autoincrement
		,name				NVARCHAR(64)								-- 사이트에 대한 명칭
		,[description]		NVARCHAR(256)	DEFAULT('')					-- Memo
		,[site_type]		NVARCHAR(64)	DEFAULT('')		NOT NULL	-- BTS 시스템의 종류(Bugzilla, Mantis, Trac 중 1)
		,[schema_name]		NVARCHAR(64)								-- RSM Name (It's Database Name)   -- There is no constraint but, It has to unique
		,base_url			NVARCHAR(256)					NOT NULL	-- 타겟의 기본 url
		,log_path			NVARCHAR(256)					NOT NULL	-- 분석에 사용된 로그파일 위치
		,cache_path			NVARCHAR(256)					NOT NULL	-- 캐쉬를 저장하고 있는 위치
		,create_ts			datetime									-- 데이터베이스 생성시간.
		,delta_ts			datetime									-- 마지막으로 변경한 시간
		,[status]			NVARCHAR(64)								-- 수집 상태 표시. 수집작업 완료일자 (완료가 안된경우 null)
		,PRIMARY KEY (id)
	);
	CREATE UNIQUE NONCLUSTERED INDEX	site_name_idx	ON [site](name);
END;;


----------------------------------------
-- BUG Tracking System 정보 등록
----------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'site_type')
BEGIN
	CREATE TABLE site_type
	(
		 id						SMALLINT		IDENTITY(1,1)				--a unique ID.
		,sortkey				SMALLINT		DEFAULT (0)     NOT NULL	--A number used to determine the order in which values are shown.
		,value					NVARCHAR(64)    DEFAULT ('')    NOT NULL	--A possible value of the field
		,PRIMARY KEY (id)
	);
	CREATE NONCLUSTERED INDEX			site_type_sortkey_idx				ON site_type(sortkey, value);
	CREATE UNIQUE NONCLUSTERED INDEX	site_type_value_idx					ON site_type(value);
	
	INSERT INTO site_type VALUES(1,N'bugzilla');
	INSERT INTO site_type VALUES(2,N'mantis');
	INSERT INTO site_type VALUES(3,N'trac');
END;;


----------------------------------------
-- 통계용 테이블 (site)
----------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'site_summary')
BEGIN
	CREATE TABLE site_summary
	(
		 site_id		int				NOT NULL
		,[status]		NVARCHAR(64)
		,ProejctCnt		int
		,BugCnt			int
		,UserCnt		int
		,[start_date]	datetime		-- 데이터 시작시간
		,[end_date]		datetime		-- 데이터 종료시간
		,PRIMARY KEY(site_id)
	);
END;;

----------------------------------------
-- 사이트에 속한 프로젝트의 요약정보들
----------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'project_summary')
BEGIN
	CREATE TABLE project_summary
	(
		 site_id			int				NOT NULL		-- 사이트 아이디 아이디
		,project_id			int				NOT NULL		-- 저장소 종속모델의 project_id 참고
		,[start_date]		datetime						-- 데이터의 시작 시간
		,[end_date]			datetime						-- 데이터의 종료 시간
		,BugCnt				int				
		,UserCnt			int
		,PRIMARY KEY(site_id, project_id)
	)
END;;



----------------------------------------
-- 분석용 테이블
----------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'analysis')
BEGIN
	CREATE TABLE analysis
	(
		 id					int				identity(1,1)
		,title				NVARCHAR(256)					-- 분석인스턴스 제목
		,[description]		NVARCHAR(max)					-- 분석 인스턴스 설명
		,[schema_name]		NVARCHAR(256)					-- 분석인스턴스 데이터베이스
		,is_uniformly		BIT								-- 조건을 통합적으로 사용하는지 선택 (1)이면 아래 필드들 사용
		,[start_date]		datetime						-- 데이터 시작시간
		,[end_date]			datetime						-- 데이터 종료시간		
		,condition			NVARCHAR(max)					-- 데이터 필터 STR
		,PRIMARY KEY(id)
	);
END;;

----------------------------------------
-- 분석인스턴스에 포함된 프로젝트 정보
----------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'analysis_project')
BEGIN
	CREATE TABLE analysis_project
	(
		 analysis_id		int				NOT NULL		-- 분석테이블 아이디
		,site_id			int				NOT NULL		-- site_id
		,project_id			int				NOT NULL		-- 저장소 종속모델의 project_id 참고
		,[satart_date]		datetime						-- 순서
		,[end_date]			datetime						-- 순서
		,condition			NVARCHAR(max)					-- 데이터 필터 STR
		,bugs				int				
		,users				int
		,PRIMARY KEY(analysis_id, site_id, project_id)
	)
END;;


----------------------------------------
-- 분석용 쿼리 저장소
----------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'analysis_query')
BEGIN
	CREATE TABLE analysis_query
	(
		 id					int				NOT NULL		-- 식별자
		,[type]				NVARCHAR(32)	NOT NULL		-- 쿼리 종류(SYSTEM PROCDDURE, SYSREM FUNCTION, USER PROCEDURE, USER FUNCTION, QUERY)
		,title				NVARCHAR(256)	NOT NULL		-- 쿼리 제목
		,[description]		NVARCHAR(max)					-- 쿼리 설명
		,[name]				NVARCHAR(256)					-- 시스템에 등록된 PROCEDURE OR FUNCTION의 명칭
		,[query]			NVARCHAR(MAX)	NOT NULL		-- 쿼리 내용
		,creation_ts		datetime						-- 데이터 시작시간
		,modification_ts	datetime						-- 데이터 종료시간
		,PRIMARY KEY(id)
	)
END;;


----------------------------------------
-- 분석인스턴스 히스토리
----------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'analysis_history')
BEGIN
	CREATE TABLE analysis_history
	(
		 id					int				identity(1,1)	-- 식별자
		,analysis_id		int				NOT NULL		-- 분석테이블 아이디
		,seq				int				NOT NULL		-- 순서
		,[when]				datetime						-- 변경 시간
		,[area]				NVARCHAR(256)					-- 변경 영역 (테이블)
		,[what]				NVARCHAR(256)					-- 변경 위치 (필드)
		,[before]			NVARCHAR(MAX)					-- 이전 내용
		,[after]			NVARCHAR(MAX)					-- 현재 내용
		,PRIMARY KEY(id)
	)
END;;



----------------------------------------------------------------------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------------------------------------------------------------------


----------------------------------------
-- 사이트 추가 프로시저
----------------------------------------
CREATE PROCEDURE saveSiteInfo
(
	 @name			AS NVARCHAR(64)		-- 사이트명
	,@description	AS NVARCHAR(256)	-- 사이트에 대한 설명
	,@site_type		AS NVARCHAR(64)		-- 사이트의 BTS 종류
	,@sname			AS NVARCHAR(64)		-- 스키마 명
	,@base_url		AS NVARCHAR(256)	-- 기본 URL
	,@log_path		AS NVARCHAR(256)	-- 로그 패스
	,@cache_path	AS NVARCHAR(256)	-- 캐쉬 패스
)
AS
BEGIN

	IF NOT EXISTS ( SELECT id FROM site_type WHERE value=LTRIM(RTRIM((@site_type))))
		RETURN -1

	--키 이름 정제
	SET @name = LTRIM(RTRIM((@name)))
	
	
	--기존에 존재하는 데이터인지 검증 (존재하면 정보업데이트 및 '재작업'으로 상태변경)
	DECLARE @id as integer
	SELECT @id = id FROM [site] WHERE name=@name
	IF @id is not null
	BEGIN
		UPDATE s 
		SET
			 [schema_name] = @sname
			,[description] = @description
			,log_path = @log_path
			,cache_path = @cache_path
			,delta_ts = SYSDATETIME()
			,[status] = N'Reworking'
		FROM [site] s
		WHERE id = @id

		RETURN @id	
	END

	INSERT INTO [site] VALUES(@name, @description, @site_type, @sname, @base_url, @log_path, @cache_path, SYSDATETIME(), SYSDATETIME(), N'Working')
	SET @id = @@IDENTITY --IDENT_CURRENT('target_info')
	RETURN @id
END;;


----------------------------------------
-- 사이트정보 업데이트 프로시저
----------------------------------------
CREATE PROCEDURE updateSiteInfo
(
	 @id			AS integer		= null
	,@name			AS NVARCHAR(64)
	,@status		AS NVARCHAR(64)
)
AS
BEGIN
	IF (@id is null)
	BEGIN
		SELECT @id = id FROM [site] WHERE name=@name
		IF (@id is null)	RETURN -1
	END
	ELSE
	BEGIN
		IF NOT EXISTS (SELECT id  FROM [site] WHERE id=@id)	RETURN -1
	END

	UPDATE i 
	SET	[status] = @status 
		,delta_ts = SYSDATETIME()
	from [site] i WHERE id=@id

	RETURN 1
END;;



----------------------------------------
-- 사이트정보 업데이트 프로시저 for Migration
----------------------------------------
CREATE PROCEDURE updateSiteInfoMig
(
	@site_id as integer
)
AS
BEGIN
	UPDATE s SET [status] = 'COMPLETED' FROM [SITE] s WHERE id = @site_id
	UPDATE s SET [status] = 'COMPLETED' FROM [site_summary] s WHERE site_id = @site_id
	RETURN 1
END;;


----------------------------------------
-- 사이트아이디 얻기
----------------------------------------
CREATE PROCEDURE getSiteID
(
	@schema as NVARCHAR(256)
)
AS
BEGIN
	DECLARE @site_id as integer

	SELECT @site_id = id from [site] where [schema_name] = @schema

	PRINT @site_id

	RETURN @site_id
END;;




------------------------------------------------------------------------------
-- 사이트의 통계 정보 생성  (지정된 사이트에 대해서 수행)
------------------------------------------------------------------------------
CREATE PROCEDURE makeSummary
(
	@site_id as integer
)
AS
BEGIN

	IF @site_id is null RETURN -1

	BEGIN TRANSACTION
	INSERT INTO site_summary
	SELECT 
		 S.id
		,S.[status]
		,(SELECT count(id) as ProjectCNT from project where site_id = S.id)	as ProjectCNT
		,(SELECT count(id) as BugCNT from bug where site_id = S.id)			as BugCNT
		,(SELECT count(id) as UserCNT from [user] where site_id = S.id)		as UserCNT
		,(SELECT min(creation_ts) from [bug] where site_id = S.id)			as [Startdate]
		,(SELECT max(creation_ts) from [bug] where site_id = S.id)			as [Enddate]
	FROM [site] S
	WHERE S.id = @site_id


	--프로젝트 요약 정보 삽입
	INSERT INTO project_summary
	SELECT 
		 site_id
		,id 
		,(SELECT min(creation_ts) FROM bug where site_id = p.site_id and project_id = p.id) as [start_date]
		,(SELECT max(creation_ts) FROM bug where site_id = p.site_id and project_id = p.id) as end_date
		,(SELECT count(id) FROM bug where site_id = p.site_id and project_id = p.id) as BugCNT
		,(SELECT count([uid])	AS users 
			FROM (	SELECT DISTINCT reporter_id	AS [uid]	FROM [bug]		WHERE site_id = p.site_id and project_id = p.id UNION 
					SELECT DISTINCT assignee_id				FROM [bug]		WHERE site_id = p.site_id and project_id = p.id UNION 
					SELECT DISTINCT qa_id					FROM [bug]		WHERE site_id = p.site_id and project_id = p.id UNION
					SELECT DISTINCT submitter_id			FROM comment	WHERE site_id = p.site_id and bug_id in (SELECT id FROM bug WHERE site_id = p.site_id and project_id = p.id) UNION
					SELECT DISTINCT submitter_id			FROM attachment WHERE site_id = p.site_id and bug_id in (SELECT id FROM bug WHERE site_id = p.site_id and project_id = p.id) UNION
					SELECT DISTINCT who_id					FROM history	WHERE site_id = p.site_id and bug_id in (SELECT id FROM bug WHERE site_id = p.site_id and project_id = p.id) UNION
					SELECT DISTINCT [user_id]				FROM monitor	WHERE site_id = p.site_id and bug_id in (SELECT id FROM bug WHERE site_id = p.site_id and project_id = p.id) 
				) A	--각 테이블별 프로젝트 내에서 관련된  User를 추출.
		 ) UserCNT
	from project p
	WHERE site_id = @site_id

	COMMIT
	RETURN 1
END;;



----------------------------------------
-- 모든 사이트 정보 삭제
----------------------------------------
CREATE PROCEDURE deleteSite
(
	@site_id as integer
)
AS
BEGIN
	-- 사이트 아이디가 널이면 종료
	IF @site_id is null return -1
	
	--해당 Site의 RIM정보 삭제-----------------------------------------------------
	EXEC deleteSite_RIM @site_id

	
	--RSM 데이터베이스 삭제--------------------------------------------------------
	DECLARE @resCnt			AS INTEGER
	DECLARE @sql			AS NVARCHAR(MAX)
	DECLARE @params			AS NVARCHAR(256) = '@resCnt as int OUTPUT'	--OUTPUT에 주의
	DECLARE @schema_name	AS NVARCHAR(128)
	DECLARE @ret			AS INTEGER

	SELECT @schema_name = [schema_name]  from [site] where id = @site_id
	SET @sql = N'drop database  ' + @schema_name 
	EXEC @ret = sp_executesql @sql, @params, @resCnt OUTPUT

	IF @ret <> 0	--sp_executesql은 리턴이 0이면 정상실행 아니면 에러.
	BEGIN
		PRINT 'ERROR_CODE = ' + cast(@ret as nvarchar(128))
		RETURN -1
	END

	--사이트 요약정보 삭제--------------------------------------------------------
	DELETE FROM site_summary WHERE site_id  = @site_id
	
	DELETE FROM project_summary WHERE site_id  = @site_id

	--사이트 정보 삭제------------------------------------------------------------
	DELETE FROM [site] where id = @site_id

	RETURN 1
END;;



----------------------------------------
-- 사이트 정보 삭제(RIM)
----------------------------------------
CREATE PROCEDURE deleteSite_RIM
(
	@site_id as integer
)
AS
BEGIN
	IF @site_id is null return -1
		
	delete from milestone		   where site_id = @site_id
	delete from bug				   where site_id = @site_id
	delete from comment			   where site_id = @site_id
	delete from attachment		   where site_id = @site_id
	delete from attach_data		   where site_id = @site_id
	delete from history			   where site_id = @site_id
	delete from relationship	   where site_id = @site_id
	delete from reference		   where site_id = @site_id
	delete from monitor			   where site_id = @site_id
	delete from [user]			   where site_id = @site_id
	delete from project			   where site_id = @site_id
	delete from component		   where site_id = @site_id
	delete from keyword			   where site_id = @site_id
	delete from bug_keyword		   where site_id = @site_id
	delete from additional_info	   where site_id = @site_id
	delete from [status]		   where site_id = @site_id
	delete from resolution		   where site_id = @site_id
	delete from severity		   where site_id = @site_id
	delete from [priority]		   where site_id = @site_id
	delete from [platform]		   where site_id = @site_id
	delete from os				   where site_id = @site_id
	delete from [version]		   where site_id = @site_id


	--사이트 요약정보 삭제--------------------------------------------------------
	DELETE FROM site_summary WHERE site_id  = @site_id
	DELETE FROM project_summary WHERE site_id  = @site_id

	UPDATE [site] 	SET	[status] = 'RSM_DONE'	FROM [site] where id = @site_id

	RETURN 1
END;;






----------------------------------------
-- 사이트정보들 통계 프로세저
----------------------------------------
CREATE procedure statisticBugs
AS
BEGIN
	Declare @id as integer
	Declare @name as NVARCHAR(64)
	Declare @resCnt as integer
	Declare @sql as nvarchar(256) 
	Declare @sql1 as nvarchar(256) = N'SELECT @resCnt = count(*) FROM '
	Declare @sql2 as nvarchar(256) = N'..bug'

	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의


	--기본적인데이터 삽입.
	DELETE from statistic
	INSERT INTO statistic 
	SELECT id, sname , bts_type, [status], create_ts, delta_ts, start_ts, end_ts, null,null
	FROM target_info order by id


	--커서 정의
	DECLARE site_cursor CURSOR FOR 
    SELECT id, sname 
	FROM target_info order by id


	--커서 회전
    OPEN site_cursor
	FETCH NEXT FROM site_cursor	INTO @id, @name
    WHILE @@FETCH_STATUS = 0
    BEGIN
		SET @sql = @sql1 + @name + @sql2

		EXEC sp_executesql @sql, @params, @resCnt OUTPUT
        
		UPDATE s SET ReportCNT = @resCnt FROM statistic s WHERE id = @id

        FETCH NEXT FROM site_cursor INTO @id, @name
    END

	--할당 해제
    CLOSE site_cursor
    DEALLOCATE site_cursor

	RETURN
END;;

--===================================================================================
--    문자리스트를 인트 배열로 변경
--===================================================================================
CREATE FUNCTION getIntArray
(
	 @delimiter		as NVARCHAR(2) = null
	,@strItems		as NVARCHAR(MAX)
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
