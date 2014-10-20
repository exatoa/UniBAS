use Bugzilla_mozilla

------------------------------------------------
-- 수집상황 확인
------------------------------------------------
SELECT * FROM getWorkStatus()

SELECT * FROM getFailedList(null) order by ID
SELECT * FROM getFailedList(1)
SELECT * FROM getFailedList(2)


------------------------------------------------
--Preprocessor  데이터 확인.
------------------------------------------------
select * from products
select * from components
select * from component_cc		--뭐지?
select * from [bug_status]
select * from resolution
select * from [priority]
select * from bug_severity
select * from op_sys
select * from rep_platform
select * from versions
select * from keyworddefs
select * from milestones
select count(*) from products
select count(*) from components
select count(*) from component_cc		--뭐지?
select count(*) from [bug_status]
select count(*) from resolution
select count(*) from [priority]
select count(*) from bug_severity
select count(*) from op_sys
select count(*) from rep_platform
select count(*) from versions
select count(*) from keyworddefs
select count(*) from milestones


------------------------------------------------
--기본 정보 확인.
------------------------------------------------
select top 100 * from bugs
select top 100 * from profiles
select top 100 * from attach_data
select top 100 * from attachments
select top 100 * from longdescs
select top 100 * from bugs_activity
select top 100 * from bug_see_also

select top 100 * from relationships
select top 100 * from dependencies
select top 100 * from duplicates
select top 100 * from cc
select top 100 * from keywords
select top 100 * from votes
select * from votes

select top 100 * from bugs_fulltext

select count(*) from profiles




------------------------------------------------
-- Function 테스트
------------------------------------------------
use Bugzilla_Test10

--주별 버그리포트 제출수 구함
DECLARE @date as date	--각주의 시작일
SET @date = '1998-04-05'
SELECT * from getCountEachWeek(@date,null) -- DATEADD(month,5,@date)
SELECT * from getCountEachWeek(null,@date)




SELECT * from getBugrateProducts('2003-06-01', '2003-06-30') order by productName
SELECT * from getBugrateInComponents((SELECT id FROM products WHERE name = N'firefox'),'2003-01-01', '2003-01-31') order by componentName


SELECT * from getBugrateWithStatusResolution(null,'2003-06-01', '2003-06-30') order by [resolution], [status]
SELECT * from getBugrateWithStatus(16,null,null)
SELECT * FROM getBugrateWithResolution(16,null,null)
SELECT * FROM getBugrateWithSeverity(16,null,null)
SELECT * from getBugrateWithPriority(16,null,null)


SELECT TOP 10 * FROM getHotissueBug(16,'2003-06-01','2003-06-30',null) order by NOTE_CNT DESC


SELECT * FROM getHotissueBug(16,'2003-06-01','2003-06-30',N'NEW') order by NOTE_CNT DESC
SELECT * FROM getHotissueBug(null,'2003-06-01','2003-06-30',N'REOPENED') order by NOTE_CNT DESC

select * from month_developer
EXEC usp_CreateUserAssignRate null, '2003-06-01', '2003-06-30'


------------------------------------------------
-- 주별 버그리포트 제출 수 
-- 시작 주를 날짜로 지정
-- 시작 주를 지정하지 않은경우 bugs 가장 빠른 날부터 시작.
------------------------------------------------
CREATE FUNCTION getCountEachWeek
(
	 @sDate as date = null
	,@eDate as date = null
)
RETURNS @Items TABLE
(
	 id integer not null identity(1,1)
	,[start] date
	,[end] date
	,CNT integer
)
AS
BEGIN
	DECLARE @start as date		-- 각주의 시작일
	DECLARE @end as datetime	-- 각주의 종료일

	--기본 데이터 값이 들어오지 않으면, 가장 빠른 날짜가 포함된 월요일을 지정.
	IF(@sDate is null)
		SELECT @sDate = MIN(creation_ts) FROM bugs

	IF(@eDate is null)
		SELECT @eDate = MAX(creation_ts) FROM bugs

	--시작하는 날의 월요일을 시작주로 결정.
	DECLARE @weeknum as integer = 0
	SET @weeknum = ((DATEPART([weekday],@sDate) + 5)%7)
	SET @start = DATEADD(day,@weeknum*-1,@sDate)


	WHILE @start<=@eDate
	BEGIN
		SET @end = DATEADD(day, 7, @start)

		INSERT INTO @Items 
		SELECT  @start, DATEADD(day,6,@start), COUNT(id) from bugs where creation_ts >= @start and creation_ts < @end

		SET @start = @end
	END 
	RETURN
END;;




------------------------------------------------
-- 현재 시스템의 작업상태를 보여줌
------------------------------------------------
CREATE Function getWorkStatus
()
RETURNS @Items TABLE
(
	 [type] varchar(64)
	,cnt	integer
)
AS
BEGIN
	INSERT INTO @Items
	SELECT [TYPE], sum(CNT) CNT
	FROM
	(
		SELECT 
			CASE WHEN progress=N'PENDING'			THEN N'FAIL'
				 WHEN progress=N'PRIVATE'			THEN N'NONE'
				 WHEN progress=N'APP_ERROR'			THEN N'NONE'
				 WHEN progress=N'DONE_WITHOUTFILE'	THEN N'DONE'
				 ELSE progress END [TYPE]
			,count(*) CNT
		from bugs_manager
		group by progress
		union all
		select N'NONE' [TYPE], max(id) - count(*) CNT from bugs
		union all
		select N'TOTAL' [TYPE], max(id) CNT from bugs
	) list
	group by [TYPE]
	RETURN
END;;



------------------------------------------------
-- 현재시스템의 실패한 아이디의 목록을 보여줌
-- 인자 : null = PENDING, FAIL
-- 인자 :    1 = DONE_WITHOUTFILE, PENDING, FAIL
-- 인자 :    2 = APP_ERROR, DONE_WITHOUTFILE, PENDING, FAIL
------------------------------------------------
CREATE Function getFailedList
(
	@range as integer = null
)
RETURNS @Items TABLE
(
	 ID integer
	 ,MSG varchar(128)
)
AS
BEGIN
	
	IF (@range = 1)
	BEGIN
		INSERT INTO @Items
		SELECT bug_id, progress FROM bugs_manager WHERE progress in (N'DONE_WITHOUTFILE', N'PENDING', N'FAIL')
	END
	ELSE IF (@range = 2)
	BEGIN
		INSERT INTO @Items
		SELECT bug_id, progress FROM bugs_manager WHERE progress in (N'APP_ERROR', N'DONE_WITHOUTFILE', N'PENDING', N'FAIL')
	END
	ELSE
	BEGIN
		INSERT INTO @Items
		SELECT bug_id, progress FROM bugs_manager WHERE progress in (N'PENDING', N'FAIL')
	END

	RETURN
END;;




--------------------------------------------------------------
-- 각 product내의 컴포넌트 별 버그 발생율
--------------------------------------------------------------
CREATE Function getBugrateProducts
(
	 @sDate as date = null
	,@eDate as date = null
)
RETURNS @Items TABLE
(
	 ProductName varchar(256)
	,[Count] integer
	,[Percent] decimal(10,2)
)
AS
BEGIN

	--기본 데이터 값이 들어오지 않으면, 가장 빠른날과 늦은날로 선택
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bugs
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bugs

	--비교를 위해서 1일 증가
	SET @eDate = DATEADD(day,1,@eDate)

	--해당 값 insert.
	INSERT INTO @Items
	SELECT
		 [Name]
		,[Count]
		,cast( (([Count] / cast([all] as float))*100) as decimal(10,2)) as [Percent]
	FROM (
		SELECT
			 p.name as [Name]
			,count(b.id) as [Count]
		FROM products p
		LEFT JOIN (SELECT id, product_id from bugs where creation_ts >= @sDate and creation_ts < @eDate) b on b.product_id = p.id
		group by p.id, p.name
	) b
	cross join (
		select count(*) as [all] from bugs 
		where creation_ts >= @sDate and creation_ts < @eDate
	) a 

	--전체 합계 추가
	INSERT INTO @Items
	SELECT
		 N'--TOTAL--'	as [Status]
		,sum([Count])	as [Count]
		,100.00			as [Percent]

	from @Items


	RETURN
END;;



		

--------------------------------------------------------------
-- product내의 등록된버그리포트 중에 컴포넌트 별 버그 발생율
--------------------------------------------------------------
CREATE Function getBugrateInComponents
(
	 @pid	as integer
	,@sDate as date
	,@eDate as date
)
RETURNS @Items TABLE
(
	 ComponentName varchar(256)
	,[Count] integer
	,[Percent] decimal(10,2)
)
AS
BEGIN

	--기본 데이터 값이 들어오지 않으면, 가장 빠른날과 늦은날로 선택
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bugs
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bugs

	--비교를 위해서 1일 증가
	SET @eDate = DATEADD(day,1,@eDate)

	INSERT INTO @Items
	SELECT
		 [Name]
		,[Count]
		,cast( (([Count] / cast([all] as float))*100) as decimal(10,2)) as [Percent]
	FROM (
		SELECT
			c.name as [Name]
			,count(b.id) as [Count]
		FROM (SELECT * FROM components WHERE product_id = @pid) c
		LEFT JOIN (SELECT * FROM bugs where creation_ts >= @sDate and creation_ts < @eDate) b  on c.id = b.component_id
		group by c.id, c.name
	) b
	cross join (
		select count(*) as [all] from bugs 
		where creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
	) a

	--전체 합계 추가
	INSERT INTO @Items
	SELECT
		 N'--TOTAL--'	as [Status]
		,sum([Count])	as [Count]
		,100.00			as [Percent]

	from @Items

	RETURN
END;;



--------------------------------------------------------------
-- 버그리포트 status 비율
-- 각 인자가 null이면 전체 버그리포트를 대상으로 조사.
--------------------------------------------------------------
CREATE Function getBugrateWithStatus
(
	 @pid	as integer = null
	,@sDate as date = null
	,@eDate as date = null
)
RETURNS @Items TABLE
(
	 [Status] varchar(256)
	,[Count] integer
	,[Percent] decimal(10,2)
)
AS
BEGIN

	--기본 데이터 값이 들어오지 않으면, 가장 빠른날과 늦은날로 선택
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bugs
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bugs
	

	--비교를 위해서 1일 증가
	SET @eDate = DATEADD(day,1,@eDate)

	IF (@pid is null)
	BEGIN
		INSERT INTO @Items
		SELECT
			 bug_status	as [Status]
			,CNT		as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(10,2)) as [%]
		from (
			SELECT bug_status, count(*) as CNT	FROM bugs
			where creation_ts >= @sDate and creation_ts < @eDate
			group by bug_status
		) b
		cross join (
			select count(*) as [all] from bugs
			where creation_ts >= @sDate and creation_ts < @eDate
		) a 
	END
	ELSE
	BEGIN
		INSERT INTO @Items
		SELECT
			 bug_status	as [Status]
			,CNT		as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(10,2)) as [%]
		from (
			SELECT bug_status, count(*) as CNT	FROM bugs
			where creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
			group by bug_status
		) b
		cross join (
			select count(*) as [all] from bugs
			where creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
		) a 
	END

	--전체 합계 추가
	INSERT INTO @Items
	SELECT
		 N'--TOTAL--'	as [Status]
		,sum([Count])	as [Count]
		,100.00			as [Percent]

	from @Items

	RETURN
END;;



--------------------------------------------------------------
-- 버그리포트 resolution 비율
--------------------------------------------------------------
CREATE Function getBugrateWithResolution
(
	 @pid	as integer = null
	,@sDate as date = null
	,@eDate as date = null
)
RETURNS @Items TABLE
(
	 Resolution varchar(256)
	,[Count] integer
	,[Percent] decimal(10,2)
)
AS
BEGIN

	--기본 데이터 값이 들어오지 않으면, 가장 빠른날과 늦은날로 선택
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bugs
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bugs
	

	--비교를 위해서 1일 증가
	SET @eDate = DATEADD(day,1,@eDate)

	IF (@pid is null)
	BEGIN
		INSERT INTO @Items
		SELECT
			 resolution as Resolution
			,CNT		as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(10,2)) as [%]
		FROM (
			SELECT resolution, count(*) as CNT	FROM bugs
			where creation_ts >= @sDate and creation_ts < @eDate
			group by resolution
		) b
		CROSS JOIN (
			select count(*) as [all] from bugs
			where creation_ts >= @sDate and creation_ts < @eDate
		) a 
	END
	ELSE
	BEGIN
		INSERT INTO @Items
		SELECT
			 resolution as Resolution
			,CNT		as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(10,2)) as [%]
		FROM (
			SELECT resolution, count(*) as CNT	FROM bugs
			where creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
			group by resolution
		) b
		CROSS JOIN (
			select count(*) as [all] from bugs
			where creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
		) a 
	END

	--전체 합계 추가
	INSERT INTO @Items
	SELECT
		 N'--TOTAL--'	as Resolution
		,sum([Count])	as [Count]
		,100.00			as [Percent]

	from @Items

	RETURN
END;;


--------------------------------------------------------------
-- 버그리포트 Status & resolution 비율
--------------------------------------------------------------
CREATE Function getBugrateWithStatusResolution
(
	 @pid	as integer = null
	,@sDate as date = null
	,@eDate as date = null
)
RETURNS @Items TABLE
(
	 [Status]	varchar(256)
	,Resolution	varchar(256)
	,[Count]	integer
	,[Percent]	decimal(10,2)
)
AS
BEGIN

	--기본 데이터 값이 들어오지 않으면, 가장 빠른날과 늦은날로 선택
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bugs
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bugs
	

	--비교를 위해서 1일 증가
	SET @eDate = DATEADD(day,1,@eDate)

	IF (@pid is null)
	BEGIN
		INSERT INTO @Items
		SELECT
			 [bug_status]	as [Status]
			,resolution		as Resolution
			,CNT			as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(10,2)) as [%]
		FROM (
			select [bug_status], Resolution, count(*) as CNT from bugs 
			where creation_ts >= @sDate and creation_ts < @eDate
			group by Resolution, [bug_status]
		) b
		CROSS JOIN (
			select count(*) as [all] from bugs
			where creation_ts >= @sDate and creation_ts < @eDate
		) a 
	END
	ELSE
	BEGIN
		INSERT INTO @Items
		SELECT
			 [bug_status]	as [Status]
			,resolution		as Resolution
			,CNT			as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(10,2)) as [%]
		FROM (
			select [bug_status], Resolution, count(*) as CNT 
			from bugs 
			where creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
			group by Resolution, [bug_status]
		) b
		CROSS JOIN (
			select count(*) as [all] from bugs
			where creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
		) a 
	END

	--전체 합계 추가
	INSERT INTO @Items
	SELECT
		
		 N'--TOTAL--'	as [Status]
		,N''				as Resolution
		,sum([Count])	as [Count]
		,100.00			as [Percent]

	from @Items

	RETURN
END;;



--------------------------------------------------------------
-- 버그리포트 심각도 비율
--------------------------------------------------------------
CREATE Function getBugrateWithSeverity
(
	 @pid	as integer = null
	,@sDate as date = null
	,@eDate as date = null
)
RETURNS @Items TABLE
(
	 [Severity] varchar(256)
	,[Count] integer
	,[Percent] decimal(10,2)
)
AS
BEGIN

	--기본 데이터 값이 들어오지 않으면, 가장 빠른날과 늦은날로 선택
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bugs
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bugs
	

	--비교를 위해서 1일 증가
	SET @eDate = DATEADD(day,1,@eDate)

	IF (@pid is null)
	BEGIN
		INSERT INTO @Items
		SELECT
			 bug_severity	as [Severity]
			,CNT			as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(5,2)) as [%]
		FROM (
			SELECT bug_severity, count(*) as CNT	FROM bugs
			WHERE creation_ts >= @sDate and creation_ts < @eDate
			GROUP BY bug_severity
		) b
		cross join (
			SELECT count(*) as [all] FROM bugs 
			WHERE creation_ts >= @sDate and creation_ts < @eDate
		) a 
	END
	ELSE
	BEGIN
		INSERT INTO @Items
		SELECT
			 bug_severity	as [Severity]
			,CNT			as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(5,2)) as [%]
		FROM (
			SELECT bug_severity, count(*) as CNT	FROM bugs
			WHERE creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
			GROUP BY bug_severity
		) b
		cross join (
			SELECT count(*) as [all] FROM bugs 
			WHERE creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
		) a 
	END

	--전체 합계 추가
	INSERT INTO @Items
	SELECT
		 N'--TOTAL--'	as [Severity]
		,sum([Count])	as [Count]
		,100.00			as [Percent]

	from @Items

	RETURN
END;;



--------------------------------------------------------------
-- 버그리포트 priority 비율
--------------------------------------------------------------
CREATE Function getBugrateWithPriority
(
	 @pid	as integer = null
	,@sDate as date = null
	,@eDate as date = null
)
RETURNS @Items TABLE
(
	 [priority] varchar(256)
	,[Count] integer
	,[Percent] decimal(10,2)
)
AS
BEGIN

	--기본 데이터 값이 들어오지 않으면, 가장 빠른날과 늦은날로 선택
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bugs
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bugs
	

	--비교를 위해서 1일 증가
	SET @eDate = DATEADD(day,1,@eDate)

	IF (@pid is null)
	BEGIN
		INSERT INTO @Items
		SELECT
			 [priority]	as [Priority]
			,CNT		as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(5,2)) as [%]
		FROM (
			SELECT [priority], count(*) as CNT	FROM bugs
			where creation_ts >= @sDate and creation_ts < @eDate
			group by [priority]
		) b
		cross join (
			select count(*) as [all] from bugs 
			where creation_ts >= @sDate and creation_ts < @eDate
		) a 
		ORDER BY [priority]

	END
	ELSE
	BEGIN
		INSERT INTO @Items
		SELECT
			 [priority]	as [Priority]
			,CNT		as [Count]
			,cast( ((CNT / cast([all]as float))*100) as decimal(5,2)) as [%]
		FROM (
			SELECT [priority], count(*) as CNT	FROM bugs
			where creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
			group by [priority]
		) b
		cross join (
			select count(*) as [all] from bugs 
			where creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
		) a 
		ORDER BY [priority]
	END

	--전체 합계 추가
	INSERT INTO @Items
	SELECT
		 N'--TOTAL--'	as [Priority]
		,sum([Count])	as [Count]
		,100.00			as [Percent]

	from @Items

	RETURN
END;;

--------------------------------------------------------------
-- 2010년 6월 한달간 핫 이슈 리포트
--------------------------------------------------------------
CREATE Function getHotissueBug
(
	 @pid	as integer = null
	,@sDate as date = null
	,@eDate as date = null
	,@status as varchar(64) = null
)
RETURNS @Items TABLE
(
	 ID			integer
	,Summary	varchar(256)
	,[Status]	varchar(64)
	,[Resolution]	varchar(64)
	,NOTE_CNT		integer
	,History_CNT	integer
	,Attach_CNT		integer
	,creation_ts	datetime
)
AS
BEGIN

	--기본 데이터 값이 들어오지 않으면, 가장 빠른날과 늦은날로 선택
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bugs
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bugs
	

	--비교를 위해서 1일 증가
	SET @eDate = DATEADD(day,1,@eDate)

	IF (@pid is null)
	BEGIN
		IF (@status is null)
		BEGIN
			INSERT INTO @Items
			SELECT
				 b.id
				 ,b.short_desc
				 ,b.[bug_status]
				 ,b.[resolution]
				 ,(select count(*) from longdescs where bug_id =  b.id) as NOTE_CNT
				 ,(select count(*) from bugs_activity where bug_id =  b.id) as History_CNT
				 ,(select count(*) from attachments where bug_id =  b.id) as Attach_CNT
				 ,b.creation_ts
			FROM bugs b 
			WHERE creation_ts >= @sDate and creation_ts < @eDate
		END
		ELSE
		BEGIN
			INSERT INTO @Items
			SELECT
				  b.id
				 ,b.short_desc
				 ,b.[bug_status]
				 ,b.[resolution]
				 ,(select count(*) from longdescs where bug_id =  b.id) as NOTE_CNT
				 ,(select count(*) from bugs_activity where bug_id =  b.id) as History_CNT
				 ,(select count(*) from attachments where bug_id =  b.id) as Attach_CNT
				 ,b.creation_ts
			FROM bugs b 
			WHERE creation_ts >= @sDate and creation_ts < @eDate
			and bug_status = @status
		END
		
	END
	ELSE
	BEGIN
		IF (@status is null)
		BEGIN
			INSERT INTO @Items
			SELECT
				  b.id
				 ,b.short_desc
				 ,b.[bug_status]
				 ,b.[resolution]
				 ,(select count(*) from longdescs where bug_id =  b.id) as NOTE_CNT
				 ,(select count(*) from bugs_activity where bug_id =  b.id) as History_CNT
				 ,(select count(*) from attachments where bug_id =  b.id) as Attach_CNT
				 ,b.creation_ts
			FROM bugs b 
			WHERE creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
		END
		ELSE
		BEGIN
			INSERT INTO @Items
			SELECT
				 b.id
				 ,b.short_desc
				 ,b.[bug_status]
				 ,b.[resolution]
				 ,(select count(*) from longdescs where bug_id =  b.id) as NOTE_CNT
				 ,(select count(*) from bugs_activity where bug_id =  b.id) as History_CNT
				 ,(select count(*) from attachments where bug_id =  b.id) as Attach_CNT
				 ,b.creation_ts
			FROM bugs b 
			WHERE creation_ts >= @sDate and creation_ts < @eDate and product_id = @pid
			and bug_status = @status
		END
	END
	RETURN
END;;





--------------------------------------------------------------
-- 특정기간동안 각 날짜별로 개발자가 할당받은 버그리포트 수 
--------------------------------------------------------------
--(프로시저 작동 후, select 문 실행. 프로시저에서 month_developer테이블 생성함)
select * from month_developer
EXEC usp_CreateUserAssignRate null, '2003-06-01', '2003-06-30'


CREATE PROCEDURE usp_CreateUserAssignRate
(
	 @pid	integer = null
	,@sDate	date
	,@eDate	date
)
as
BEGIN 
	IF (@sDate is null) RETURN 
	IF (@eDate is null) RETURN

	--테이블 생성 쿼리 생성
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) = N'create table month_developer(ID integer, Username Nvarchar(255), Realname Nvarchar(255)'
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의

	Declare @date as date = @sDate
	WHILE(@date <= @eDate)
	BEGIN
		SET @sql = @sql + N', [' + cast(@date as nvarchar) + N'] integer'
		SET @date = DATEADD(dd, 1, @date)	--1일 증가
	END
	SET @sql = @sql + N')'


	--테이블 생성
	if object_id(N'month_developer') is not null
		drop table month_developer
	EXEC sp_executesql @sql, @params, @resCnt OUTPUT


	--기본 개발자정보 삽입
	IF (@pid is null)
	BEGIN 
		insert into month_developer(ID, Username, Realname)
		SELECT DISTINCT u.id as [ID], u.login_name as [Username], u.realname as [Realname] 
		FROM [profiles] u join bugs b on u.id = b.assigned_to
		WHERE b.creation_ts >= @sdate and b.creation_ts < DATEADD(dd, 1, @eDate)
	END
	ELSE
	BEGIN
		insert into month_developer(ID, Username, Realname)
		SELECT DISTINCT u.id as [ID], u.login_name as [Username], u.realname as [Realname] 
		FROM [profiles] u join bugs b on u.id = b.assigned_to
		WHERE b.creation_ts >= @sdate and b.creation_ts < DATEADD(dd, 1, @eDate) and b.product_id = @pid
	END


	--일별 할당량 저장.
	SET @date = @sDate
	WHILE(@date <= @eDate)
	BEGIN
		SET @sql =        N'UPDATE m SET ['+cast(@date as nvarchar)+N'] = CASE WHEN c.cnt is null THEN 0 ELSE c.cnt END '
		SET @sql = @sql + N'from month_developer m '
		SET @sql = @sql + N'left join ('
		SET @sql = @sql + N'   select '
		SET @sql = @sql + N'       b.assigned_to as [ID]'
		SET @sql = @sql + N'       , count(*) as cnt '
		SET @sql = @sql + N'   from bugs b '
		SET @sql = @sql + N'   join [profiles] u on u.id = b.assigned_to '
		SET @sql = @sql + N'   where creation_ts >= '''+cast(@date as nvarchar) +N''' and creation_ts < '''+cast(DATEADD(dd,1,@date) as nvarchar) + N''' '
		IF (@pid is not null) SET @sql = @sql + N' and b.product_id = '+@pid
		SET @sql = @sql + N'   group by b.assigned_to, u.login_name'
		SET @sql = @sql + N') c on m.Id = c.ID'

		EXEC sp_executesql @sql, @params, @resCnt OUTPUT
		
		SET @date = DATEADD(dd, 1, @date)	--1일 증가
	END
END

--------------------------------------------------------------
-- 개발자별로 수정 후 패치를 올린 정보
--------------------------------------------------------------
select *
from [user] 
where id in (select submitter_id from attachment where ispatch = 1)



--------------------------------------------------------------
-- 6월 한달간 스크린샷을 가진 버그리포트의 비율
--------------------------------------------------------------
-- [좋은 버그리포트]
-- 정보를 가진 버그리포트의 종류
-- 좋은 버그리포트란?   reprocedure, stack track, 스크린샷
-- 파일 분석해야함.
select
(
	select count(*)
	from bug b
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
	and exists (select id, bug_id, [description], filesize, submitter_id  
				from attachment 
				where bug_id = b.id and mimetype like N'image/%')
) as CNT_Image_IN_BUG
,(
	select count(*)
	from bug b
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
) as CNT_ALL


--------------------------------------------------------------
-- 6월 한달간 스크린샷을 가진 버그리포트들의 스크린샷 개수(전체)
--------------------------------------------------------------
select 
	 b.id
	,b.[status]
	,b.resolution
	,p.name		as Product
	,c.name		as Component
	,(select count(*) from attachment where bug_id = b.id and mimetype like N'image/%') as [Screenshot Count]
from bug b
join product p on b.product_id = p.id
join component c on b.component_id = c.id
where 
creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and (select count(*) from attachment where bug_id = b.id and mimetype like N'image/%') > 0



--------------------------------------------------------------
-- 6월 한달간 스크린샷을 가진 버그리포트들의 상태별 분포
--------------------------------------------------------------
--위의 쿼리들을 서브쿼리로 활용
select 
	 [status]		as [Status]
	,[resolution]	as [Resolution]
	,count(*)		as [Count]
from
(
select 
	 b.id
	,b.[status]
	,b.resolution
	,p.name		as Product
	,c.name		as Component
	,(select count(*) from attachment where bug_id = b.id and mimetype like N'image/%') as [Screenshot Count]
from bug b
join product p on b.product_id = p.id
join component c on b.component_id = c.id
where 
creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and (select count(*) from attachment where bug_id = b.id and mimetype like N'image/%') >5
) s
group by [status], [resolution]
order by [status], [resolution]




--------------------------------------------------------------
--이 시스템의 개발자 정보
--------------------------------------------------------------
--TODO : 여기부터 진행
select * from [profiles] where id in (select distinct assigned_to from bugs)



--------------------------------------------------------------
--특정 기간에 활동한 개발자 정보.
--------------------------------------------------------------
select * 
from [profiles]
where id in (select distinct assigned_to
			 from bugs
			 where creation_ts >= '2003-06-01' and creation_ts <= '2003-07-01'
			 )


--------------------------------------------------------------
-- 2010년 6월 한달간 개발자 별로 할당된 리포트 수   (실제 개발자가 맞는지는 모름)
--------------------------------------------------------------
--한달에 30건 이상 할당받은 개발자
select 
	 u.id		as [ID]
	,u.username	as [Username]
from [profiles] u 
join bugs b on u.id = b.assignee
where creation_ts > '2010-06-01' and creation_ts <= '2010-07-01'
group by u.id, u.realname, u.username
having count(*) > 30
order by u.id




-------TEST CODE -------------------------------
select
	users.ID
	,users.Username
	,[first].[2010-06-01]
from
(	select distinct u.id as [ID], u.username as [Username], u.realname as [Realname] 
	from [user] u join bug b on u.id = b.assignee 
	where creation_ts > '2010-06-01' and creation_ts <= '2010-07-01') users
left join 
(
	select b.assignee as [ID], count(*) as [2010-06-01]
	from bug b join [user] u on u.id = b.assignee
	where creation_ts > '2010-06-01' and creation_ts <= '2010-06-02'
	group by b.assignee, u.username
) [first] on users.id = [first].id



--------------------------------------------------------------
-- 기간내에 중복으로 올라온 버그리포트는?
--------------------------------------------------------------
select *
from bugs b --join [user] u on u.id = b.assignee
where creation_ts >= '2003-06-01' and creation_ts < '2003-07-01'
and resolution ='DUPLICATE'
--764개

select * from duplicates 
where dupe in (select id from bugs where creation_ts >= '2003-06-01' and creation_ts < '2003-07-01')
--764개  (두 쿼리가 동일)


--------------------------------------------------------------
--기간내에 올라온 버그리포트들 중 이전 리스트와의 중복 버그리포트
--------------------------------------------------------------
--문제점 존재 ( 크롤링한 기간보다 앞에있는 리포트는 정보가 없어서 리스트에 안나타남....)
--프로그램 수정해야함.
select
	dest_bug_id
	,count(*)		as [Count]
from bug_relationship 
where relationship_type = 0
and src_bug_id in (select id from bug where creation_ts > '2010-06-01' and creation_ts < '2010-07-01')
and dest_bug_id in (select id from bug where creation_ts < '2010-06-01')
group by dest_bug_id
having count(*)>1


--------------------------------------------------------------
--기간내에 올라온 버그리포트들 중 기간내의 리포트와 중복 버그리포트
--------------------------------------------------------------
select
	dest_bug_id
	,count(*)		as [Count]
from bug_relationship 
where relationship_type = 0
and src_bug_id in (select id from bug where creation_ts > '2010-06-01' and creation_ts < '2010-07-01')
and dest_bug_id in (select id from bug where creation_ts > '2010-06-01' and creation_ts < '2010-07-01')
group by dest_bug_id
having count(*)>1



--------------------------------------------------------------
-- 투표한 사람의 수와 투표의 수를 보여줌.
--------------------------------------------------------------
--버그리포트 투표수와 투표한 사람의 수를 보니 
-- 1인당 1표씩 행사한것으로 나타나서 votes를 투표한 사람의 수로 인식해도 무방할듯. (이 데이터에서만.

select 
	b.id
	,b.votes 
	,v.cnt
from bug b
join (	select bug_id, count(*) as cnt 
		from vote group by bug_id
	) v on b.id = v.bug_id
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and votes > 0



--------------------------------------------------------------
-- 많은 사용자가 수정을 원하는 버그와 심각도와의 관계
--------------------------------------------------------------
--버그리포트 투표수와 투표한 사람의 수를 보니 1인당 1표씩 행사한것으로 나타나서 votes를 투표한 사람의 수로 인식
--미완성

select b.id, b.severity, b.[priority], count(*) Cnt_person 
from bug b left join vote v on b.id = v.bug_id 
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
group by b.id, b.severity, b.[priority]
--having count(*) > 20
order by b.id


select b.severity, count(*) as [Count]--votes
from bug b 
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and votes <> 0
group by b.severity
--having count(*) > 20
order by b.severity




select b.[priority], count(*) as [Count]--votes
from bug b 
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
--and votes <> 0
group by b.[priority]
--having count(*) > 20
order by b.[priority]

--투표는 했는데, Priority가 올라간것.  투표를 했는데 priority가 안올라간것.
--투표가 많앗는데 Priority가 안올라간것.


select 
	b.id
	,b.votes 
	,v.cnt
from bug b
join (	select bug_id, count(*) as cnt 
		from vote group by bug_id
	) v on b.id = v.bug_id
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and votes > 0

select * from [priority]
--우선순위가 높은 리포트들고 투표수
select id, status, resolution, severity, votes
from bug 
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and [priority] in ('P1','P2','P3') 
and votes > 0

select id, status, resolution, severity, votes
from bug 
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and [priority] in ('P1','P2','P3') 
and votes = 0





--사용자가 바라보는 관점과
--개발자가 바라보는 우선순위의 관점


-- Vote가 높으면 사용자의 우선순위가 높아짐
-- Priority는

--Vote는 높은데 Priority가 낮은것?
--Vote 높음

--Vote and priority가 높은것
--Vote 낮고 priority가 높은거  --> 개발자와의 차이
--Vote 낮고 priority 낮은거


select b.id, b.severity, b.[priority], count(*) Cnt_person 
from bug b left join vote v on b.id = v.bug_id 
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
group by b.id, b.severity, b.[priority]
--having count(*) > 20
order by b.id


select b.[priority], count(*) as [Count]--votes
from bug b 
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and votes <> 0
group by b.[priority]
--having count(*) > 20
order by b.[priority]



