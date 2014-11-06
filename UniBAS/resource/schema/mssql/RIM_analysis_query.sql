
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


-------------------------------------------------------
-- IDF별 용어들으 분포를 보여줌. (stopword포함)
-------------------------------------------------------
CREATE FUNCTION getDistIDF
(
	 @mode_id integer
	,@site_id integer
)
RETURNS @table TABLE 
(
	 value	decimal(10,5)
	,cnt	integer
)
AS
BEGIN
	--IDF의 분포를 보여줌.
	INSERT INTO @table
	SELECT
			DISTINCT value/10.0, count(*)
	from (SELECT term_id, cast((value*10) as integer) as value FROM getIDF(@mode_id, @site_id) ) a
	group by value
	RETURN
END;;

------------------------------------------------------
--   버그리포트 관계에서 특정 일자 범위내에서 중복이 발생되엇는지 확인.
------------------------------------------------------
CREATE FUNCTION getRangeRalationship
(
	 @site_id as integer
	,@range as integer
)
RETURNS @table TABLE
(
	 id			int
	,dest		int
	,isExist	bIT
)
AS
BEGIN
	
	--중복 버그들 중에 수집한 버그리포트 안에 있는 버그들만 선택.
	DECLARE RIDX  CURSOR FOR
	select r.src_bug_id, r.dest_bug_id
	from relationship r
	join bug b on b.id = r.dest_bug_id
	where relationship_type = 1 and r.site_id = @site_id

	DECLARE @src as integer
	DECLARE @dest as integer
	DECLARE @check as integer

	OPEN RIDX
	FETCH NEXT FROM RIDX INTO @src, @dest

	WHILE @@FETCH_STATUS = 0
	BEGIN
		SET @check = null
		SELECT @check = id 	FROM
			(SELECT id from bug
			 WHERE site_id = @site_id
			 and creation_ts >= dateadd(day, (-1 * @range), (select creation_ts from bug where id = @src))
			 AND creation_ts < dateadd(day, @range, (select creation_ts from bug where id = @src)) 
			) rt
		WHERE id = @dest
	
		--존재하면 추가 아니면 버림.	
		IF @check is not null 
			INSERT INTO @table VALUES (@src, @dest, 1)
		ELSE
			INSERT INTO @table VALUES (@src, @dest, 0)

		FETCH NEXT FROM RIDX INTO @src, @dest
	END
	CLOSE RIDX
	DEALLOCATE RIDX

	RETURN
END;;

----------------------------------------------------
-- 관련 버그리포트에 대해서 발생일의 차이값을 구함.
----------------------------------------------------
CREATE FUNCTION getDiffRelationship
(
	 @site_id as integer
	,@relType as integer
)
RETURNS @table TABLE
(
	 id				int
	,src_bug_id		int
	,desc_bug_id	int
	,srcDate		datetime
	,destDate		datetime
	,diff			int
)
AS
BEGIN
	INSERT INTO @table
	select 
		 *	 
		,datediff(day, srcDate, destDate) Diff
	from
	(
	select
		 site_id
		,src_bug_id
		,dest_bug_id
		,(select creation_ts from bug where site_id = r.site_id and id = r.src_bug_id) as srcDate
		,(select creation_ts from bug where site_id = r.site_id and id = r.dest_bug_id) as destDate
	from [relationship] r
	where r.site_id = @site_id and r.relationship_type = @relType
	) a
	RETURN
END;;




-------------------------------------------------------
-- 문서 IDF 구하기
-------------------------------------------------------
CREATE FUNCTION getIDF
(
	 @mode_id as integer
	,@site_id as integer
)
RETURNS @table TABLE 
(
	 term_id	integer
	,value		decimal(10,5)
	,primary key(term_id)
)
AS
BEGIN
	--전체 문서수 구하기
	DECLARE @N INTEGER
	SELECT @N = COUNT(*) FROM BUG


	INSERT INTO @table
	--SELECT term_id, 1+SQRT(@N / count(distinct bug_id)) IDF
	SELECT term_id, 1+log(@N / count(distinct bug_id)) IDF
	FROM element_term e
	WHERE e.mode_id = @mode_id and e.site_id = @site_id and e.src_type in ('S','D')
	and stopword = 0
	group by e.term_id


	RETURN
END;;


--------------------------------------------------------------------
-- 코사인 유사도 비교를 위한 초기화 함수.(테이블 생성 및 IDF값 설정)
--------------------------------------------------------------------
CREATE PROCEDURE init_CosineSimility
(
	 @mode_id as integer
	,@site_id as integer
	,@project_id as integer
)
AS
BEGIN
	IF EXISTS (SELECT * FROM sys.tables WHERE NAME = 'vector')
		DROP TABLE vector

	CREATE TABLE vector
	(
		 id		INTEGER				NOT NULL
		,A		DECIMAL(10,5)
		,B		DECIMAL(10,5)
		,IDF	DECIMAL(10,5)
		,primary key(id)
	)

	--전체 단어 입력 (vector 항목 구하기)
	INSERT INTO vector	
	SELECT	t.id,0, 0, 0
	FROM	term t
	WHERE	t.mode_id = @mode_id
	AND		t.value not in (SELECT name FROM stopword WHERE mode_id = @mode_id)


	--각 용어에 대한 IDF를 미리 구함.
	DECLARE @N INTEGER
	SELECT @N = COUNT(*) FROM BUG

	UPDATE v 
	SET IDF = t.value
	FROM VECTOR v
	JOIN getIDF(@mode_id, @site_id) t on v.id = t.term_id


	--문서 전체의 값을 먼저 구함.
	IF EXISTS (SELECT * FROM sys.tables WHERE NAME = 'DTF')
		DROP TABLE DTF
	CREATE TABLE DTF
	(
		 bug_id	integer
		,value	Integer				--문서 내 총 용어 수
		,primary key (bug_id)
	)

	--한 문서 내에서 발생한 모든 텀 수
	INSERT INTO DTF
	SELECT bug_id, count(*) from element_term
	WHERE mode_id = @mode_id and site_id = @site_id
	GROUP BY bug_id

END;;


-------------------------------------------------------
-- 코사인 유사도 비교
-------------------------------------------------------
CREATE PROCEDURE calc_CosineSimility
(
	 @modeID as integer
	,@siteID as integer
	,@srcID as integer
	,@destID as integer
	,@COST	AS DECIMAL(10,5) OUTPUT
)
AS
BEGIN
	IF NOT EXISTS (SELECT * FROM Bug WHERE site_id = @siteID and id = @srcID) or NOT EXISTS (SELECT * FROM Bug WHERE site_id = @siteID and id = @destID)
	BEGIN
		PRINT N'Can not find bug report'
		SET @COST = 0
		RETURN 
	END

	--벡터 테이블 초기화
	UPDATE v set a = 0.0, B=0.0 FROM vector v

	--한 문서 내에서 발생한 모든 텀 수
	DECLARE @srcCnt as integer
	DECLARE @destCnt as integer
	SELECT	@srcCnt = value		FROM DTF WHERE bug_id = @srcID
	SELECT	@destCnt = value	FROM DTF WHERE bug_id = @destID


	--A문서TF계산 : A문서에 나오는 모든 단어들의 카운트 / 문서내 모든 텀 수    (제목 더블카운팅)
	UPDATE v SET v.A = A + (c.cnt * 2)  FROM vector v
	JOIN (SELECT term_id, count(*)  cnt FROM element_term WHERE mode_id = @modeID and site_id = @siteID and bug_id = @srcID and src_type = 'S' and stopword = 0 group by term_id) c
	on v.id = c.term_id

	UPDATE v SET v.A = ((A + c.cnt) / @srcCnt) FROM vector v
	JOIN (SELECT term_id, count(*)  cnt FROM element_term WHERE mode_id = @modeID and site_id = @siteID and bug_id = @srcID and src_type = 'D' and stopword = 0 group by term_id) c
	on v.id = c.term_id


	--B문서TF계산 : B문서에 나오는 모든 단어들의 카운트 / 문서내 모든 텀 수    (제목 더블카운팅)
	UPDATE v SET v.B = B + (c.cnt * 2) FROM vector v
	JOIN (SELECT term_id, count(*)  cnt FROM element_term WHERE mode_id = @modeID and site_id = @siteID and bug_id = @destID and src_type = 'S' and stopword = 0 group by term_id) c
	on v.id = c.term_id

	UPDATE v SET v.B = ((B + c.cnt) / @srcCnt) FROM vector v
	JOIN (SELECT term_id, count(*)  cnt FROM element_term WHERE mode_id = @modeID and site_id = @siteID and bug_id = @destID and src_type = 'D' and stopword = 0 group by term_id) c
	on v.id = c.term_id

	--내적을 이용한 코사인 유사도 계산
	--뒤에 두 IDF의 곱은  미리 곱해져야 되는 값이지만 인수분해를 통해 밖으로 뺀 것임
	DECLARE @cos decimal(10,5)
	SELECT @cos =  sum(product) / (sqrt(sum(vA)) * sqrt(sum(vB))) FROM (			
		SELECT  id, A*A AS vA, B*B as vB,A*B as product FROM vector
	) v

	--결과 반환
	PRINT 'Cosine Simility = ' + cast(@cos as varchar(32))
	SET @COST = @COS

	RETURN 1
END;;



-------------------------------------------------------
-- 삭제 및 정리
-------------------------------------------------------
CREATE PROCEDURE exit_CosineSimility
AS
BEGIN
	if EXISTS (SELECT * FROM SYS.tables WHERE NAME = 'vector')
		DROP TABLE vector
END;;




-----------------------------------------------------------------
-- 중복으로 판별된 버그들이 어떤 유사도를 가지는지 확인
-----------------------------------------------------------------
CREATE PROCEDURE getSimilarity
(
	 @mode_id	as integer
	,@site_id   as integer
	,@project_id as integer
)
AS
BEGIN

	IF NOT EXISTS (SELECT * FROM sys.tables WHERE NAME = 'similarity')
	BEGIN
		CREATE TABLE similarity
		(
			 site_id		integer				not null
			,src_id		INTEGER				NOT NULL
			,dest_id	INTEGER				NOT NULL
			,value		DECIMAL(10,5)
			,[check]	BIT	
		)
	END

	--유사도를 구사기위하여 초기화.
	EXEC init_CosineSimility @mode_id, @site_id, @project_id
	
	DECLARE @srcID AS INTEGER
	DECLARE @destID AS INTEGER
	DECLARE @cost AS DECIMAL(10,5)


	--중복인 버그리포트에 대한 유사도 구하기
	DECLARE DUP_CUR CURSOR FOR    
	select src_bug_id, dest_bug_id from relationship where site_id = @site_id and relationship_type = 1

	OPEN DUP_CUR
	FETCH NEXT FROM DUP_CUR     INTO @srcID, @destID

	WHILE @@FETCH_STATUS = 0
	BEGIN
		
		--실제 정답인지 확인.
		DECLARE @check as integer
		IF EXISTS (select src_bug_id from relationship where site_id = @site_id and src_bug_id = @srcID and dest_bug_id = @destID and relationship_type = 1)
			SET @check = 1
		ELSE 
			SET @check = 0

		EXEC calc_CosineSimility @mode_id, @site_id, @srcID, @DestID, @cost OUTPUT
		INSERT INTO similarity VALUES (@site_id, @srcID, @destID, @cost, @check)

		FETCH NEXT FROM DUP_CUR     INTO @srcID, @destID
	END
                
	CLOSE DUP_CUR
	DEALLOCATE DUP_CUR

	EXEC exit_CosineSimility

	RETURN 1
END;;



-----------------------------------------------------------------
-- 모든 버그에 대하여 현재 버그의 유사도구하기.
-----------------------------------------------------------------
CREATE PROCEDURE getSimilarityAll
(
	 @mode_id	as integer
	,@site_id   as integer
	,@project_id as integer
)
AS
BEGIN

	IF NOT EXISTS (SELECT * FROM sys.tables WHERE NAME = 'similarity')
	BEGIN
		CREATE TABLE similarity
		(
			 site_id		integer				not null
			,src_id		INTEGER				NOT NULL
			,dest_id	INTEGER				NOT NULL
			,value		DECIMAL(10,5)
			,[check]	BIT	
		)
	END
	

	--유사도를 구사기위하여 초기화.
	EXEC init_CosineSimility @mode_id, @site_id, @project_id
	
	DECLARE @srcID AS INTEGER
	DECLARE @destID AS INTEGER
	DECLARE @cost AS DECIMAL(10,5)

	--전체 버그리포트 수.
	DECLARE @bugs as INTEGER
	SELECT @bugs = count(*) FROM bug


	--소스 버그 아이디에 대한 커서 생성
	DECLARE BUG_CUR CURSOR FOR	
	select id from bug With (nolock) 
	where site_id = @site_id and project_id = @project_id 
	ORDER BY ID
	OPEN BUG_CUR	FETCH NEXT FROM BUG_CUR     INTO @srcID

	--커서 회전
	WHILE @@FETCH_STATUS = 0
	BEGIN
		PRINT N'BUG ' + CAST(@srcID as nvarchar(100)) + N'/' + CAST(@bugs AS NVARCHAR(100)) + N' working...'

		--타겟 버그 아이디에 대한 커서 생성
		DECLARE DEST_CUR CURSOR FOR		
		SELECT id FROM bug With (nolock) 
		WHERE site_id = @site_id and project_id = @project_id and id <> @srcID
		ORDER BY ID

		OPEN DEST_CUR	FETCH NEXT FROM DEST_CUR     INTO @destID

		--커서 회전
		DECLARE @idx as integer = 1
		WHILE @@FETCH_STATUS = 0
		BEGIN
			--실제 정답인지 확인.
			DECLARE @check as integer
			IF EXISTS (select src_bug_id from relationship where site_id = @site_id and src_bug_id = @srcID and dest_bug_id = @destID and relationship_type = 1)
				SET @check = 1
			ELSE 
				SET @check = 0

			--유사도 계산.
			EXEC calc_CosineSimility @mode_id, @site_id, @srcID, @DestID, @cost OUTPUT
			INSERT INTO similarity VALUES (@site_id, @srcID, @destID, @cost, @check)

			--다음으로 이동
			PRINT N'[' + CAST(@idx as nvarchar(100)) + N'/' + CAST((@bugs-1) AS NVARCHAR(100)) + N'] Compare ' + CAST(@srcID as nvarchar(100)) + N' with ' + CAST(@destID AS NVARCHAR(100)) + N' Result : ' + CAST(@cost as nvarchar(100))
			
			FETCH NEXT FROM DEST_CUR     INTO @destID			
		END                
		CLOSE DEST_CUR
		DEALLOCATE DEST_CUR
		
		PRINT N'BUG ' + CAST(@srcID as nvarchar(100)) + N'/' + CAST(@bugs AS NVARCHAR(100)) + N' working...Done'
		FETCH NEXT FROM BUG_CUR     INTO @srcID
	END
                
	CLOSE BUG_CUR
	DEALLOCATE BUG_CUR

	EXEC exit_CosineSimility


	RETURN 1
END;;





------------------------------------------------
-- 주별 버그리포트 제출 수 
-- 시작 주를 날짜로 지정
-- 시작 주를 지정하지 않은경우 bugs 가장 빠른 날부터 시작.
------------------------------------------------
CREATE FUNCTION getCountEachWeek
(
	 @site_id as integer
	,@project_id as integer
	,@sDate as date = null
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
		SELECT @sDate = MIN(creation_ts) FROM bug where site_id = @site_id and project_id = @project_id

	IF(@eDate is null)
		SELECT @eDate = MAX(creation_ts) FROM bug where site_id = @site_id and project_id = @project_id

	--시작하는 날의 월요일을 시작주로 결정.
	DECLARE @weeknum as integer = 0
	SET @weeknum = ((DATEPART([weekday],@sDate) + 5)%7)
	SET @start = DATEADD(day,@weeknum*-1,@sDate)


	WHILE @start<=@eDate
	BEGIN
		SET @end = DATEADD(day, 7, @start)

		INSERT INTO @Items 
		SELECT  @start, DATEADD(day,6,@start), COUNT(id) from bug 
		where site_id = @site_id and project_id = @project_id and  creation_ts >= @start and creation_ts < @end

		SET @start = @end
	END 
	RETURN
END;;


------------------------------------------------
-- 월별 버그리포트 제출 수 
-- 시작 주를 날짜로 지정
-- 시작 주를 지정하지 않은경우 bugs 가장 빠른 날부터 시작.
------------------------------------------------
CREATE FUNCTION getCountEachMonth
(
	 @site_id as integer
	,@project_id as integer
	,@sDate as date = null
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
		SELECT @sDate = MIN(creation_ts) FROM bug where site_id = @site_id and project_id = @project_id

	IF(@eDate is null)
		SELECT @eDate = MAX(creation_ts) FROM bug where site_id = @site_id and project_id = @project_id

	--시작하는 달의 1일을 지정
	SET @start = cast(DATEPART(yy, @sDate) as varchar) + '-' + cast(DATEPART(mm, @sDate) as varchar) + N'-01'

	WHILE @start<=@eDate
	BEGIN
		SET @end = DATEADD(month, 1, @start)

		INSERT INTO @Items 
		SELECT  @start, DATEADD(day,-1,@end), COUNT(id) from bug 
		where site_id = @site_id and project_id = @project_id and  creation_ts >= @start and creation_ts < @end

		SET @start = @end
	END 
	RETURN
END;;

--------------------------------------------------------------
-- 2010년 6월 한달간 핫 이슈 리포트
--------------------------------------------------------------
CREATE Function getHotissueBug
(
	 @site_id as integer
	,@pid	as integer = null
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
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bug
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bug
	

	--비교를 위해서 1일 증가
	SET @eDate = DATEADD(day,1,@eDate)

	IF (@pid is null)
	BEGIN
		IF (@status is null)
		BEGIN
			INSERT INTO @Items
			SELECT
				 b.id
				 ,b.summary
				 ,b.[status]
				 ,b.[resolution]
				 ,(select count(*) from comment where bug_id =  b.id) as NOTE_CNT
				 ,(select count(*) from history where bug_id =  b.id) as History_CNT
				 ,(select count(*) from attachment where bug_id =  b.id) as Attach_CNT
				 ,b.creation_ts
			FROM bug b 
			WHERE site_id = @site_id and creation_ts >= @sDate and creation_ts < @eDate
		END
		ELSE
		BEGIN
			INSERT INTO @Items
			SELECT
				  b.id
				 ,b.summary
				 ,b.[status]
				 ,b.[resolution]
				 ,(select count(*) from comment where bug_id =  b.id) as NOTE_CNT
				 ,(select count(*) from history where bug_id =  b.id) as History_CNT
				 ,(select count(*) from attachment where bug_id =  b.id) as Attach_CNT
				 ,b.creation_ts
			FROM bug b 
			WHERE site_id = @site_id and creation_ts >= @sDate and creation_ts < @eDate
			and [status] = @status
		END
		
	END
	ELSE
	BEGIN
		IF (@status is null)
		BEGIN
			INSERT INTO @Items
			SELECT
				  b.id
				 ,b.summary
				 ,b.[status]
				 ,b.[resolution]
				 ,(select count(*) from comment where bug_id =  b.id) as NOTE_CNT
				 ,(select count(*) from history where bug_id =  b.id) as History_CNT
				 ,(select count(*) from attachment where bug_id =  b.id) as Attach_CNT
				 ,b.creation_ts
			FROM bug b 
			WHERE site_id = @site_id and creation_ts >= @sDate and creation_ts < @eDate and project_id = @pid
		END
		ELSE
		BEGIN
			INSERT INTO @Items
			SELECT
				 b.id
				 ,b.[summary]
				 ,b.[status]
				 ,b.[resolution]
				 ,(select count(*) from comment where bug_id =  b.id) as NOTE_CNT
				 ,(select count(*) from history where bug_id =  b.id) as History_CNT
				 ,(select count(*) from attachment where bug_id =  b.id) as Attach_CNT
				 ,b.creation_ts
			FROM bug b 
			WHERE site_id = @site_id and creation_ts >= @sDate and creation_ts < @eDate and project_id = @pid
			and [status] = @status
		END
	END
	RETURN
END;;


--------------------------------------------------------------
-- 인자에 해당하는 elementTerm정보들을 반환
--------------------------------------------------------------
CREATE PROCEDURE getElementTerm
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	IF @mode_id is null or @mode_id is null
		RETURN -1

    SELECT    bug_id, src_type, term_id, count(*) cnt from element_term
    WHERE     mode_id = @mode_id and site_id = @site_id and stopword = 0
    GROUP BY  bug_id, src_type, term_id
    ORDER BY  bug_id, src_type, term_id

	RETURN 1
END;;




--------------------------------------------------------------
-- 인자에 포함된 문서들의 전체 용어 수의 목록을 반환 (stopword 포함)
--------------------------------------------------------------
CREATE PROCEDURE getDocumentTF
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	--bug_id, count 테이블로 리스트를 반환
	IF @mode_id is null or @mode_id is null
		RETURN -1

	SELECT bug_id, count(*) [count] from element_term
	WHERE mode_id = @mode_id and site_id = @site_id
	GROUP BY bug_id

	RETURN 1
END;;



--------------------------------------------------------------
-- 인자에 포함된 문서들의 전체 용어 수의 목록을 반환 (stopword 비포함, Summary만)
--------------------------------------------------------------
CREATE PROCEDURE getSummaryTF
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	--bug_id, count 테이블로 리스트를 반환
	IF @mode_id is null or @mode_id is null
		RETURN -1

	SELECT bug_id, count(*) [count] from element_term
	WHERE mode_id = @mode_id and site_id = @site_id and src_type = 'S' and stopword = 0
	GROUP BY bug_id

	RETURN 1
END;;


--------------------------------------------------------------
-- 인자에 포함된 문서들의 전체 용어 수의 목록을 반환 (stopword 비포함)
--------------------------------------------------------------
CREATE PROCEDURE getDescriptionTF
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	--bug_id, count 테이블로 리스트를 반환
	IF @mode_id is null or @mode_id is null
		RETURN -1

	SELECT bug_id, count(*) [count] from element_term
	WHERE mode_id = @mode_id and site_id = @site_id and src_type = 'D' and stopword = 0
	GROUP BY bug_id

	RETURN 1
END;;


--------------------------------------------------------------
-- 계산할 전체 버그리포트 리스트반환
--------------------------------------------------------------
CREATE PROCEDURE getBugList
(
	@site_id as integer
)
AS
BEGIN
	--id 테이블로 리스트를 반환
	IF @site_id is null
		RETURN -1

	SELECT id from bug WHERE site_id = @site_id order by id

	RETURN 1
END;;




--------------------------------------------------------------
-- 이미 정답으로 알려진 버그리포트 목록을 반환
--------------------------------------------------------------
CREATE PROCEDURE getAnswerSet
(
	@site_id as integer
)
AS
BEGIN
	--src_bug_id, dest_bug_id 테이블로 리스트를 반환
	IF @site_id is null
		RETURN -1
		
	SELECT src_bug_id, dest_bug_id from relationship
	WHERE site_id = @site_id and relationship_type = 1
	
	RETURN 1
END;;




-------------------------------------------------------
-- 문서 IDF 구하기
-------------------------------------------------------
CREATE PROCEDURE getIDFproc
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	--전체 문서수 구하기
	DECLARE @N INTEGER
	SELECT @N = COUNT(*) FROM BUG

	--SELECT term_id, 1+SQRT(@N / count(distinct bug_id)) IDF
	SELECT term_id, 1+log(@N / count(distinct bug_id)) IDF
	FROM element_term e
	WHERE e.mode_id = @mode_id and e.site_id = @site_id and e.src_type in ('S','D')
	and stopword = 0
	group by e.term_id

	RETURN 1
END;;




-----------------------------------------------------------------
-- 유사도 계산을 위한 테이블 준비
-----------------------------------------------------------------
CREATE PROCEDURE initSimilarity
(
	@table_name	as NVARCHAR(256)
)
AS
BEGIN
	DECLARE @sql       AS NVARCHAR(MAX )
    DECLARE @ret       AS INTEGER               

	--이미 존재하는 테이블 명.
	IF EXISTS (SELECT * FROM sys.indexes WHERE NAME = @table_name)
		RETURN -1

	SET @sql =			N'CREATE TABLE '+ @table_name
	SET @sql = @sql +	N' ('
	SET @sql = @sql +	N'		 site_id	INTEGER				NOT NULL'
	SET @sql = @sql +	N'		,src_id		INTEGER				NOT NULL'
	SET @sql = @sql +	N'		,dest_id	INTEGER				NOT NULL'
	SET @sql = @sql +	N'		,value		DECIMAL(10,5)'
	SET @sql = @sql +	N'		,[check]	BIT	'
	SET @sql = @sql +	N' )'
	EXEC @ret = sp_executesql @sql, N''
	IF @ret <> 0 RETURN -2

	RETURN 1
END;;


-----------------------------------------------------------------
-- 유사도 계산완료시 인덱스 생성
-----------------------------------------------------------------
CREATE PROCEDURE finalSimilarity
(
	@table_name	as NVARCHAR(256)
)
AS
BEGIN

	DECLARE @sql       AS NVARCHAR(MAX )
    DECLARE @ret       AS INTEGER               

	--테이블이 존재하지 않으면 에러.
	IF NOT EXISTS (SELECT * FROM sys.indexes WHERE NAME = @table_name)
		RETURN -1
	

	SET  @sql = N'CREATE NONCLUSTERED INDEX ' + @table_name + N'_idx_ids  ON ' + @table_name + N'(site_id, src_id, dest_id)'
	EXEC @ret = sp_executesql @sql, N''
	IF (@ret <>0) RETURN -2

	SET  @sql = N'CREATE NONCLUSTERED INDEX ' + @table_name + N'_idx_check  ON ' + @table_name + N'([check])'
	EXEC @ret = sp_executesql @sql,  N''
	IF (@ret <>0) RETURN -2

	SET  @sql = N'CREATE NONCLUSTERED INDEX ' + @table_name + N'_idx_value  ON ' + @table_name + N'(value)'
	EXEC @ret = sp_executesql @sql,  N''
	IF (@ret <>0) RETURN -2

	SET  @sql = N'CREATE NONCLUSTERED INDEX ' + @table_name + N'_idx_src  ON ' + @table_name + N'(src_id)'
	EXEC @ret = sp_executesql @sql,  N''
	IF (@ret <>0) RETURN -2

	SET  @sql = N'CREATE NONCLUSTERED INDEX ' + @table_name + N'_idx_dest  ON ' + @table_name + N'(dest_id)'
	EXEC @ret = sp_executesql @sql,  N''
	IF (@ret <>0) RETURN -2

	RETURN 1
END;;



-----------------------------------------------------
-- 제목의 첫번째 명사가 review인 버그리포트의 ID를 추출.
-----------------------------------------------------
CREATE PROCEDURE getReviewList
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	select bug_id from element_term 
	where mode_id = @mode_id and site_id = @site_id and src_type = 'S' and seq = 0 and term_id = 
	(select id from term where value = 'review')
END;;



-----------------------------------------------------
-- 중복에 대한 추천리스트 5개를 추출
-----------------------------------------------------
CREATE PROCEDURE makeRecommandList
(
	 @site_id as integer
	,@top_size as integer
	,@result_table as nvarchar(256)
	,@target_table as nvarchar(256)
)
AS
BEGIN
	DECLARE @sql       as NVARCHAR(MAX )
    DECLARE @ret       as integer        


	--이미 존재하는 테이블 명.
	IF EXISTS (SELECT * FROM sys.tables WHERE NAME = @result_table)
	BEGIN
		SET @sql =	N'DROP TABLE ' + @result_table
		EXEC @ret = sp_executesql @sql , N''
		IF (@ret <>0) RETURN -1
	END
		
	--테이블 생성
	SET @sql =			N'CREATE TABLE ' + @result_table
	SET @sql =	@sql +	N' ('
	SET @sql =	@sql +	N'	 bug_id		integer'
	SET @sql =	@sql +	N'	,src_id		integer'
	SET @sql =	@sql +	N'	,dest_id	integer'
	SET @sql =	@sql +	N'	,value		decimal(10,5)'
	SET @sql =	@sql +	N'	,[check]	BIT'
	SET @sql =	@sql +	N' )'
	
	EXEC @ret = sp_executesql @sql , N''
	IF (@ret <>0) RETURN -1
       

	--커서 생성
	DECLARE @id AS INTEGER
	DECLARE BUG_CUR CURSOR FOR     
	SELECT id FROM bug where site_id = @site_id

	--커서 이동
	OPEN BUG_CUR
	FETCH NEXT FROM BUG_CUR     INTO @id
	WHILE @@FETCH_STATUS = 0
	BEGIN
		--work
		
		SET @sql = N''
		SET @sql += N' INSERT INTO ' + @result_table
		SET @sql += N' SELECT	top ' + cast(@top_size as nvarchar)
		SET @sql += N' 	 ' +  cast(@id as nvarchar) + N'		bug_id'
		SET @sql += N' 	,src_id '
		SET @sql += N' 	,dest_id '
		SET @sql += N' 	,value '
		SET @sql += N' 	,[check]'
		SET @sql += N' from ' + @target_table + N' where (src_id = '+ cast(@id as nvarchar) + N' or dest_id = '+ cast(@id as nvarchar) + N') and value >= 0.00001'
		SET @sql += N' order by value desc'

		EXEC @ret = sp_executesql @sql , N''
		IF (@ret <>0) RETURN -1

		PRINT N'inserted relating ' + cast(@id  as nvarchar)

		FETCH NEXT FROM BUG_CUR     INTO @id
	END
                
	CLOSE BUG_CUR
	DEALLOCATE BUG_CUR

	SET  @sql = N'CREATE NONCLUSTERED INDEX ' + @result_table + N'_idx_bug_id  ON ' + @result_table + N'([bug_id])'
	EXEC @ret = sp_executesql @sql,  N''
	IF (@ret <>0) RETURN -2

	SET  @sql = N'CREATE NONCLUSTERED INDEX ' + @result_table + N'_idx_ids  ON ' + @result_table + N'(src_id, dest_id)'
	EXEC @ret = sp_executesql @sql,  N''
	IF (@ret <>0) RETURN -2

	SET  @sql = N'CREATE NONCLUSTERED INDEX ' + @result_table + N'_idx_check  ON ' + @result_table + N'([check])'
	EXEC @ret = sp_executesql @sql,  N''
	IF (@ret <>0) RETURN -2

	RETURN 1
END;;


------------------------------------------------------------------
--중복 추천 버그리포트의 유사도 값 분포 (정답을 맞춘 것들만..)
------------------------------------------------------------------
CREATE PROCEDURE distSimilarityRecommand
(
	 @result_table as nvarchar(256)
)
AS
BEGIN
	DECLARE @sql       as NVARCHAR(MAX )
    DECLARE @ret       as integer        

	--테이블 명이 존재하지 않으면 백
	IF NOT EXISTS (SELECT * FROM sys.tables WHERE NAME = @result_table)
	BEGIN
		RETURN -1
	END
		
	--테이블 생성
	SET @sql =			N' select v, count(*) cnt '
	SET @sql =	@sql +	N' from ('
	SET @sql =	@sql +	N' 	select distinct src_id, dest_id, cast(value*10 as integer) v '
	SET @sql =	@sql +	N' 		from ' + @result_table
	SET @sql =	@sql +	N' 		where [check] = 1'
	SET @sql =	@sql +	N' ) s'
	SET @sql =	@sql +	N' group by v'
	SET @sql =	@sql +	N' order by v desc'
	
	EXEC @ret = sp_executesql @sql , N''
	IF (@ret <>0) RETURN -1
	
	RETURN 1
END;;


------------------------------------------------------------------
--문서들의 복잡도를 구함
------------------------------------------------------------------
CREATE PROCEDURE getComplexity
(
	 @modeID	integer
	,@siteID integer
	,@ids NVARCHAR(max)
)
AS
BEGIN

	--total voc count
	DECLARE @total decimal(10,3)
	DECLARE @unique decimal(10,3)

	select @total = count(*) from element_term 
	where	bug_id in (select item from getIntArray(@ids, ','))
	and		src_type in ('D', 'S') 
	and		site_id = @siteID
	and		mode_id = @modeID
	and		stopword = 0

	--unique voc count
	select @unique = count(distinct term_id) from element_term 
	where	bug_id in (select item from getIntArray(@ids, ','))
	and		src_type in ('D', 'S') 
	and		site_id = @siteID
	and		mode_id = @modeID
	and		stopword = 0

	--PRINT @unique / @total
	RETURN @unique / @total
END;;


---------------------------------------------------------
--버그리포트들의 용어 크기에 관한 정보를 반환
---------------------------------------------------------
CREATE PROCEDURE getReports_Terminfo
(
	 @siteID	integer
	,@modeID	integer
	,@unique	BIT = 0
)
AS
BEGIN
	--이미 필터링 되어있어서 범위 필요없음.
	--and creation_ts >= '2004-01-01' and creation_ts < '2004-01-10'
	--
	IF @unique = 0 
	BEGIN
		SELECT id, summary, [description], creation_ts, volume
		FROM bug  b
		join (
			select bug_id, count(term_id) as volume from element_term where mode_id = @modeID and site_id = @siteID
			group by bug_id
		) t on b.id = t.bug_id
		WHERE site_id = @siteID
	END
	ELSE
	BEGIN
		SELECT id, summary, [description], creation_ts, volume
		FROM bug  b
		join (
			select bug_id, count(distinct term_id) as volume from element_term where mode_id = @modeID and site_id = @siteID
			group by bug_id
		) t on b.id = t.bug_id
		WHERE site_id = @siteID
	END
END;;