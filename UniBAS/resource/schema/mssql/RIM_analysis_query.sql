
--===================================================================================
--    �۾� ���� �Լ�.
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
	--�۾��� ���� �ʱ⼳��
	IF @delimiter is null					--�⺻ DELIMITER ����
		SET @delimiter = ','
	
	SET @strItems = LTRIM(RTRIM(@strItems))

	--�������� delimiter�� ������ �߰�.
	IF CHARINDEX (@delimiter, @strItems, LEN(@strItems)) <=0
		SET @strItems = @strItems + @delimiter		--�������� DELIMITER �߰�

	SELECT @sp_len = LEN(@delimiter)		--DELIMITER ����
	SET @s = 1								--ó�� ������ġ����

	WHILE (1=1)
	BEGIN
		--delimiter �˻�
		SELECT @e = CHARINDEX(@delimiter, @strItems, @s)
		IF @e <= 0 BREAK

		--@s, @e������ ���ڿ� �߶�.
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
-- IDF�� ������ ������ ������. (stopword����)
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
	--IDF�� ������ ������.
	INSERT INTO @table
	SELECT
			DISTINCT value/10.0, count(*)
	from (SELECT term_id, cast((value*10) as integer) as value FROM getIDF(@mode_id, @site_id) ) a
	group by value
	RETURN
END;;

------------------------------------------------------
--   ���׸���Ʈ ���迡�� Ư�� ���� ���������� �ߺ��� �߻��Ǿ����� Ȯ��.
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
	
	--�ߺ� ���׵� �߿� ������ ���׸���Ʈ �ȿ� �ִ� ���׵鸸 ����.
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
	
		--�����ϸ� �߰� �ƴϸ� ����.	
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
-- ���� ���׸���Ʈ�� ���ؼ� �߻����� ���̰��� ����.
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
-- ���� IDF ���ϱ�
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
	--��ü ������ ���ϱ�
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
-- �ڻ��� ���絵 �񱳸� ���� �ʱ�ȭ �Լ�.(���̺� ���� �� IDF�� ����)
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

	--��ü �ܾ� �Է� (vector �׸� ���ϱ�)
	INSERT INTO vector	
	SELECT	t.id,0, 0, 0
	FROM	term t
	WHERE	t.mode_id = @mode_id
	AND		t.value not in (SELECT name FROM stopword WHERE mode_id = @mode_id)


	--�� �� ���� IDF�� �̸� ����.
	DECLARE @N INTEGER
	SELECT @N = COUNT(*) FROM BUG

	UPDATE v 
	SET IDF = t.value
	FROM VECTOR v
	JOIN getIDF(@mode_id, @site_id) t on v.id = t.term_id


	--���� ��ü�� ���� ���� ����.
	IF EXISTS (SELECT * FROM sys.tables WHERE NAME = 'DTF')
		DROP TABLE DTF
	CREATE TABLE DTF
	(
		 bug_id	integer
		,value	Integer				--���� �� �� ��� ��
		,primary key (bug_id)
	)

	--�� ���� ������ �߻��� ��� �� ��
	INSERT INTO DTF
	SELECT bug_id, count(*) from element_term
	WHERE mode_id = @mode_id and site_id = @site_id
	GROUP BY bug_id

END;;


-------------------------------------------------------
-- �ڻ��� ���絵 ��
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

	--���� ���̺� �ʱ�ȭ
	UPDATE v set a = 0.0, B=0.0 FROM vector v

	--�� ���� ������ �߻��� ��� �� ��
	DECLARE @srcCnt as integer
	DECLARE @destCnt as integer
	SELECT	@srcCnt = value		FROM DTF WHERE bug_id = @srcID
	SELECT	@destCnt = value	FROM DTF WHERE bug_id = @destID


	--A����TF��� : A������ ������ ��� �ܾ���� ī��Ʈ / ������ ��� �� ��    (���� ����ī����)
	UPDATE v SET v.A = A + (c.cnt * 2)  FROM vector v
	JOIN (SELECT term_id, count(*)  cnt FROM element_term WHERE mode_id = @modeID and site_id = @siteID and bug_id = @srcID and src_type = 'S' and stopword = 0 group by term_id) c
	on v.id = c.term_id

	UPDATE v SET v.A = ((A + c.cnt) / @srcCnt) FROM vector v
	JOIN (SELECT term_id, count(*)  cnt FROM element_term WHERE mode_id = @modeID and site_id = @siteID and bug_id = @srcID and src_type = 'D' and stopword = 0 group by term_id) c
	on v.id = c.term_id


	--B����TF��� : B������ ������ ��� �ܾ���� ī��Ʈ / ������ ��� �� ��    (���� ����ī����)
	UPDATE v SET v.B = B + (c.cnt * 2) FROM vector v
	JOIN (SELECT term_id, count(*)  cnt FROM element_term WHERE mode_id = @modeID and site_id = @siteID and bug_id = @destID and src_type = 'S' and stopword = 0 group by term_id) c
	on v.id = c.term_id

	UPDATE v SET v.B = ((B + c.cnt) / @srcCnt) FROM vector v
	JOIN (SELECT term_id, count(*)  cnt FROM element_term WHERE mode_id = @modeID and site_id = @siteID and bug_id = @destID and src_type = 'D' and stopword = 0 group by term_id) c
	on v.id = c.term_id

	--������ �̿��� �ڻ��� ���絵 ���
	--�ڿ� �� IDF�� ����  �̸� �������� �Ǵ� �������� �μ����ظ� ���� ������ �� ����
	DECLARE @cos decimal(10,5)
	SELECT @cos =  sum(product) / (sqrt(sum(vA)) * sqrt(sum(vB))) FROM (			
		SELECT  id, A*A AS vA, B*B as vB,A*B as product FROM vector
	) v

	--��� ��ȯ
	PRINT 'Cosine Simility = ' + cast(@cos as varchar(32))
	SET @COST = @COS

	RETURN 1
END;;



-------------------------------------------------------
-- ���� �� ����
-------------------------------------------------------
CREATE PROCEDURE exit_CosineSimility
AS
BEGIN
	if EXISTS (SELECT * FROM SYS.tables WHERE NAME = 'vector')
		DROP TABLE vector
END;;




-----------------------------------------------------------------
-- �ߺ����� �Ǻ��� ���׵��� � ���絵�� �������� Ȯ��
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

	--���絵�� ��������Ͽ� �ʱ�ȭ.
	EXEC init_CosineSimility @mode_id, @site_id, @project_id
	
	DECLARE @srcID AS INTEGER
	DECLARE @destID AS INTEGER
	DECLARE @cost AS DECIMAL(10,5)


	--�ߺ��� ���׸���Ʈ�� ���� ���絵 ���ϱ�
	DECLARE DUP_CUR CURSOR FOR    
	select src_bug_id, dest_bug_id from relationship where site_id = @site_id and relationship_type = 1

	OPEN DUP_CUR
	FETCH NEXT FROM DUP_CUR     INTO @srcID, @destID

	WHILE @@FETCH_STATUS = 0
	BEGIN
		
		--���� �������� Ȯ��.
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
-- ��� ���׿� ���Ͽ� ���� ������ ���絵���ϱ�.
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
	

	--���絵�� ��������Ͽ� �ʱ�ȭ.
	EXEC init_CosineSimility @mode_id, @site_id, @project_id
	
	DECLARE @srcID AS INTEGER
	DECLARE @destID AS INTEGER
	DECLARE @cost AS DECIMAL(10,5)

	--��ü ���׸���Ʈ ��.
	DECLARE @bugs as INTEGER
	SELECT @bugs = count(*) FROM bug


	--�ҽ� ���� ���̵� ���� Ŀ�� ����
	DECLARE BUG_CUR CURSOR FOR	
	select id from bug With (nolock) 
	where site_id = @site_id and project_id = @project_id 
	ORDER BY ID
	OPEN BUG_CUR	FETCH NEXT FROM BUG_CUR     INTO @srcID

	--Ŀ�� ȸ��
	WHILE @@FETCH_STATUS = 0
	BEGIN
		PRINT N'BUG ' + CAST(@srcID as nvarchar(100)) + N'/' + CAST(@bugs AS NVARCHAR(100)) + N' working...'

		--Ÿ�� ���� ���̵� ���� Ŀ�� ����
		DECLARE DEST_CUR CURSOR FOR		
		SELECT id FROM bug With (nolock) 
		WHERE site_id = @site_id and project_id = @project_id and id <> @srcID
		ORDER BY ID

		OPEN DEST_CUR	FETCH NEXT FROM DEST_CUR     INTO @destID

		--Ŀ�� ȸ��
		DECLARE @idx as integer = 1
		WHILE @@FETCH_STATUS = 0
		BEGIN
			--���� �������� Ȯ��.
			DECLARE @check as integer
			IF EXISTS (select src_bug_id from relationship where site_id = @site_id and src_bug_id = @srcID and dest_bug_id = @destID and relationship_type = 1)
				SET @check = 1
			ELSE 
				SET @check = 0

			--���絵 ���.
			EXEC calc_CosineSimility @mode_id, @site_id, @srcID, @DestID, @cost OUTPUT
			INSERT INTO similarity VALUES (@site_id, @srcID, @destID, @cost, @check)

			--�������� �̵�
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
-- �ֺ� ���׸���Ʈ ���� �� 
-- ���� �ָ� ��¥�� ����
-- ���� �ָ� �������� ������� bugs ���� ���� ������ ����.
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
	DECLARE @start as date		-- ������ ������
	DECLARE @end as datetime	-- ������ ������

	--�⺻ ������ ���� ������ ������, ���� ���� ��¥�� ���Ե� �������� ����.
	IF(@sDate is null)
		SELECT @sDate = MIN(creation_ts) FROM bug where site_id = @site_id and project_id = @project_id

	IF(@eDate is null)
		SELECT @eDate = MAX(creation_ts) FROM bug where site_id = @site_id and project_id = @project_id

	--�����ϴ� ���� �������� �����ַ� ����.
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
-- ���� ���׸���Ʈ ���� �� 
-- ���� �ָ� ��¥�� ����
-- ���� �ָ� �������� ������� bugs ���� ���� ������ ����.
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
	DECLARE @start as date		-- ������ ������
	DECLARE @end as datetime	-- ������ ������

	--�⺻ ������ ���� ������ ������, ���� ���� ��¥�� ���Ե� �������� ����.
	IF(@sDate is null)
		SELECT @sDate = MIN(creation_ts) FROM bug where site_id = @site_id and project_id = @project_id

	IF(@eDate is null)
		SELECT @eDate = MAX(creation_ts) FROM bug where site_id = @site_id and project_id = @project_id

	--�����ϴ� ���� 1���� ����
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
-- 2010�� 6�� �Ѵް� �� �̽� ����Ʈ
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

	--�⺻ ������ ���� ������ ������, ���� �������� �������� ����
	IF (@sDate is null)		SELECT @sDate = MIN(creation_ts) FROM bug
	IF (@eDate is null)		SELECT @eDate = MAX(creation_ts) FROM bug
	

	--�񱳸� ���ؼ� 1�� ����
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
-- ���ڿ� �ش��ϴ� elementTerm�������� ��ȯ
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
-- ���ڿ� ���Ե� �������� ��ü ��� ���� ����� ��ȯ (stopword ����)
--------------------------------------------------------------
CREATE PROCEDURE getDocumentTF
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	--bug_id, count ���̺�� ����Ʈ�� ��ȯ
	IF @mode_id is null or @mode_id is null
		RETURN -1

	SELECT bug_id, count(*) [count] from element_term
	WHERE mode_id = @mode_id and site_id = @site_id
	GROUP BY bug_id

	RETURN 1
END;;



--------------------------------------------------------------
-- ���ڿ� ���Ե� �������� ��ü ��� ���� ����� ��ȯ (stopword ������, Summary��)
--------------------------------------------------------------
CREATE PROCEDURE getSummaryTF
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	--bug_id, count ���̺�� ����Ʈ�� ��ȯ
	IF @mode_id is null or @mode_id is null
		RETURN -1

	SELECT bug_id, count(*) [count] from element_term
	WHERE mode_id = @mode_id and site_id = @site_id and src_type = 'S' and stopword = 0
	GROUP BY bug_id

	RETURN 1
END;;


--------------------------------------------------------------
-- ���ڿ� ���Ե� �������� ��ü ��� ���� ����� ��ȯ (stopword ������)
--------------------------------------------------------------
CREATE PROCEDURE getDescriptionTF
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	--bug_id, count ���̺�� ����Ʈ�� ��ȯ
	IF @mode_id is null or @mode_id is null
		RETURN -1

	SELECT bug_id, count(*) [count] from element_term
	WHERE mode_id = @mode_id and site_id = @site_id and src_type = 'D' and stopword = 0
	GROUP BY bug_id

	RETURN 1
END;;


--------------------------------------------------------------
-- ����� ��ü ���׸���Ʈ ����Ʈ��ȯ
--------------------------------------------------------------
CREATE PROCEDURE getBugList
(
	@site_id as integer
)
AS
BEGIN
	--id ���̺�� ����Ʈ�� ��ȯ
	IF @site_id is null
		RETURN -1

	SELECT id from bug WHERE site_id = @site_id order by id

	RETURN 1
END;;




--------------------------------------------------------------
-- �̹� �������� �˷��� ���׸���Ʈ ����� ��ȯ
--------------------------------------------------------------
CREATE PROCEDURE getAnswerSet
(
	@site_id as integer
)
AS
BEGIN
	--src_bug_id, dest_bug_id ���̺�� ����Ʈ�� ��ȯ
	IF @site_id is null
		RETURN -1
		
	SELECT src_bug_id, dest_bug_id from relationship
	WHERE site_id = @site_id and relationship_type = 1
	
	RETURN 1
END;;




-------------------------------------------------------
-- ���� IDF ���ϱ�
-------------------------------------------------------
CREATE PROCEDURE getIDFproc
(
	 @mode_id as integer
	,@site_id as integer
)
AS
BEGIN
	--��ü ������ ���ϱ�
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
-- ���絵 ����� ���� ���̺� �غ�
-----------------------------------------------------------------
CREATE PROCEDURE initSimilarity
(
	@table_name	as NVARCHAR(256)
)
AS
BEGIN
	DECLARE @sql       AS NVARCHAR(MAX )
    DECLARE @ret       AS INTEGER               

	--�̹� �����ϴ� ���̺� ��.
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
-- ���絵 ���Ϸ�� �ε��� ����
-----------------------------------------------------------------
CREATE PROCEDURE finalSimilarity
(
	@table_name	as NVARCHAR(256)
)
AS
BEGIN

	DECLARE @sql       AS NVARCHAR(MAX )
    DECLARE @ret       AS INTEGER               

	--���̺��� �������� ������ ����.
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
-- ������ ù��° ��簡 review�� ���׸���Ʈ�� ID�� ����.
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
-- �ߺ��� ���� ��õ����Ʈ 5���� ����
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


	--�̹� �����ϴ� ���̺� ��.
	IF EXISTS (SELECT * FROM sys.tables WHERE NAME = @result_table)
	BEGIN
		SET @sql =	N'DROP TABLE ' + @result_table
		EXEC @ret = sp_executesql @sql , N''
		IF (@ret <>0) RETURN -1
	END
		
	--���̺� ����
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
       

	--Ŀ�� ����
	DECLARE @id AS INTEGER
	DECLARE BUG_CUR CURSOR FOR     
	SELECT id FROM bug where site_id = @site_id

	--Ŀ�� �̵�
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
--�ߺ� ��õ ���׸���Ʈ�� ���絵 �� ���� (������ ���� �͵鸸..)
------------------------------------------------------------------
CREATE PROCEDURE distSimilarityRecommand
(
	 @result_table as nvarchar(256)
)
AS
BEGIN
	DECLARE @sql       as NVARCHAR(MAX )
    DECLARE @ret       as integer        

	--���̺� ���� �������� ������ ��
	IF NOT EXISTS (SELECT * FROM sys.tables WHERE NAME = @result_table)
	BEGIN
		RETURN -1
	END
		
	--���̺� ����
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
--�������� ���⵵�� ����
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
--���׸���Ʈ���� ��� ũ�⿡ ���� ������ ��ȯ
---------------------------------------------------------
CREATE PROCEDURE getReports_Terminfo
(
	 @siteID	integer
	,@modeID	integer
	,@unique	BIT = 0
)
AS
BEGIN
	--�̹� ���͸� �Ǿ��־ ���� �ʿ����.
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