use Mantis_Scribus
use Mantis_Phplist
use Mantis_Mantis
use Mantis_Sylohe


------------------------------------------------
-- 수집상황 확인
------------------------------------------------
--전체 상황
SELECT * FROM getWorkStatus()

--에러난 상황
SELECT * FROM getFailedList(null)	--순수 실패리스트
SELECT * FROM getFailedList(1)		--다운에러까지
SELECT * FROM getFailedList(2)		--앱에러 포함



------------------------------------------------
--Preprocessor  데이터 확인.
------------------------------------------------
select * from project
select * from category
select * from [status]
select * from resolution
select * from [priority]
select * from [severity]
select * from reproducibility
select * from os
select * from os_build
select * from [platform]
select * from project_version
select * from tag

select * from bug where [severity] = 81--'@0@'
select * from bugs_manager where bug_id = 15624--[severity] = 81--'@0@'

select count(*) from project
select count(*) from category
select count(*) from [status]
select count(*) from resolution
select count(*) from [priority]
select count(*) from [severity]
select count(*) from reproducibility
select count(*) from os
select count(*) from os_build
select count(*) from [platform]
select count(*) from project_version
select count(*) from tag


------------------------------------------------
--기본 정보 확인.
------------------------------------------------
select top 100 * from bug
select top 100 * from [user]
select top 100 * from bug_file
select top 100 * from bug_tag
select top 100 * from bug_text
select top 100 * from bug_history
select top 100 * from bug_relationship
select top 100 * from bugnote
select top 100 * from bugnote_text
select top 100 * from bug_revision

select count(*) from [user]

------------------------------------------------
-- Function 테스트
------------------------------------------------
use mantis_phplist
use Mantis_Scribus

--주별 버그리포트 제출수 구함
DECLARE @date as date	--각주의 시작일
SET @date = '2010-04-06'
SELECT * from getCountEachWeek(@date,null)
SELECT * from getCountEachWeek(null,null)

SELECT max(date_submitted) from bug



-----------------------------------------------------------------------
--  임시 테스트 코드
-----------------------------------------------------------------------


select c.id, p.name, c.name, c.[description]  from component c left join product p on p.id = c.product_id



select id, creation_ts, updated_ts, * from bug  where id = 11389
select * from bug_history where bug_id = 11389
select * from bugnote where bug_id = 11389

select * from attachment where submitter_id =1
select * from attach_data

select * from [user]


select * from bug where resolution in (N'fixed', N'reopened', N'duplicate')
select * from bug where resolution in (N'reopened')
select * from status
select * from resolution
select id, creation_ts, updated_ts, * from bug
select * from bug_history 
select * from bugnote 

select * from milestone
select * from version





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
		SELECT @sDate = MIN(date_submitted) FROM bug

	IF(@eDate is null)
		SELECT @eDate = MAX(date_submitted) FROM bug

	--시작하는 날의 월요일을 시작주로 결정.
	DECLARE @weeknum as integer = 0
	SET @weeknum = ((DATEPART([weekday],@sDate) + 5)%7)
	SET @start = DATEADD(day,@weeknum*-1,@sDate)


	WHILE @start<=@eDate
	BEGIN
		SET @end = DATEADD(day, 7, @start)

		INSERT INTO @Items 
		SELECT  @start, DATEADD(day,6,@start), COUNT(id) from bug where date_submitted >= @start and date_submitted < @end

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
	 [type] NVARCHAR(64)
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
		select N'TOTAL' [TYPE],count(*) CNT	from bugs_manager
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
	 ,MSG NVARCHAR(128)
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