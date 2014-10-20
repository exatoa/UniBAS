use UniBAS
------------------------------------------------------------------
--����Ʈ ���� Ȯ��
------------------------------------------------------------------
select * from [site]
select * from analysis


------------------------------------------------------------------
--��ü ���׸���Ʈ ��
------------------------------------------------------------------
select count(*) from bug where site_id = 5


-------------------------------------------------
--���º� ���׸���Ʈ ����
-------------------------------------------------
select 
	project_id,
	[status]
	,[resolution]
	,count(*)
from bug
where site_id = 5
group by project_id, [status], [resolution]
order by project_id, [status], [resolution]


------------------------------------------------------------------
--������Ʈ�� ���׸���Ʈ ������
------------------------------------------------------------------
select project_id, p.name, count(*) CNT
from project p 
left join bug b on b.site_id = p.site_id and b.project_id = p.id
where p.site_id = 5
and b.id<= 322027
group by project_id, p.name



------------------------------------------------------------------
-- �м� ��� ������Ʈ ���� (������Ʈ�� �ߺ� ���׸���Ʈ ��,���� ��)
------------------------------------------------------------------
--UniBAS���� �м� ��� ������Ʈ �� �˻�.
SELECT 
	 p.site_id
	,p.id
	,p.name
	,(select count(*) from bug where site_id = p.site_id and project_id = p.id and id<= 322027) Bugs
	,(
		SELECT count(*) 
		FROM bug b 
		JOIN relationship r on b.id = r.src_bug_id and r.site_id = b.site_id
		WHERE b.site_id = p.site_id and b.project_id = p.id
		and b.id <=322027
	 ) Rels
	,(
		SELECT count(*) 
		FROM bug b 
		JOIN relationship r on b.id = r.src_bug_id and r.site_id = b.site_id
		WHERE b.site_id = p.site_id and b.project_id = p.id
		and r.relationship_type = 1
		and b.id <=322027
	 ) Dups
FROM project p
WHERE p.site_id = 5
ORDER BY p.id



-------------------------------------------------
--�ֺ� ���׸���Ʈ �� Ȯ��
-------------------------------------------------
select * from getCountEachWeek(5, 16, null,null)

select * from getCountEachWeek(5, 16, '2004-01-01','2004-12-31')


--��ü ���׸���Ʈ
DECLARE @site_id	integer = 5
DECLARE @project_id integer = 16
DECLARE @start_date date	= '2004-01-01'
DECLARE @end_date	date	= '2005-01-01'
select id, project_id, creation_ts from bug 
where site_id = @site_id and project_id = @project_id
and creation_ts >= @start_date and creation_ts < @end_date


--site���� : 5			(MozillaE)
--������Ʈ ���� : 16   (firefox)
--���� : 30,347
--�ߺ����� : 11,919
--�Ⱓ���� : 2004-01-01   ~ 2005-01-01
--���׸���Ʈ�� ���ⷮ�� ���� ���ⷮ�� ��ȭ�� ū �Ⱓ ����

--���� (���ù���) : 11,742
--�ߺ�����(���ù���) : 4,738
--�ߺ�����(���ù���) : 4,691  (�������� �� ����)
--�ߺ�����(����������Ʈ) : 1,668   

DECLARE @site_id	integer = 5
DECLARE @project_id integer = 16
DECLARE @start_date date	= '2004-01-01'
DECLARE @end_date	date	= '2005-01-01'

-------------------------------------------------------
-- �ߺ��� ������Ʈ�� ���� ������Ʈ ���� (������)
-------------------------------------------------------
--������Ʈ ��Ī���
select
	b.project_id
	,p.name
	,bs.src_bug_id
	,bs.dest_bug_id
from bug b
join (
	--�ߺ��� ���׸���Ʈ
	select site_id, src_bug_id, dest_bug_id from relationship r
	where site_id = @site_id and relationship_type = 1
	and dest_bug_id <= (select max(id) from bug)
	and src_bug_id in (
		--���ϴ� ���׸���Ʈ ����
		select id from bug 
		where site_id = @site_id and project_id = @project_id
		and creation_ts >= @start_date and creation_ts < @end_date
	)
) bs on b.site_id = bs.site_id and b.id = bs.dest_bug_id
join project p	on p.site_id = @site_id and p.id = b.project_id
order by project_id

-------------------------------------------------------
-- �ߺ� ������Ʈ�� �ٸ� ������Ʈ ���� ���� (��ġ)
-------------------------------------------------------

DECLARE @site_id	integer = 5
DECLARE @project_id integer = 16
DECLARE @start_date date	= '2004-01-01'
DECLARE @end_date	date	= '2005-01-01'

--1. ��ü �ߺ� ���׸���Ʈ ����Ʈ
CREATE TABLE #ids (site_id INT, src_id INT, dest_id INT, dest_pid INT)
INSERT INTO #IDS(site_id, src_id, dest_id)
SELECT	r.site_id, src_bug_id, dest_bug_id 
FROM relationship r
WHERE	site_id = @site_id and relationship_type = 1
AND		dest_bug_id < (SELECT max(id) FROM bug WHERE site_id = @site_id)
AND		src_bug_id in (
	--���ϴ� ���׸���Ʈ ����
	SELECT	id FROM bug 
	WHERE	site_id = @site_id and project_id = @project_id
	AND		creation_ts >= @start_date and creation_ts < @end_date
)		
CREATE NONCLUSTERED INDEX tempidx ON #ids(dest_id)

--������Ʈ ���̵� ����
UPDATE	i 
SET		dest_pid = b.project_id
FROM	bug b 
JOIN	#ids i on b.id = i.dest_id


--2. ������ ������Ʈ ���� �ߺ� ���׸���Ʈ ����Ʈ
CREATE TABLE #ids_inProject (site_id INT, src_id INT, dest_id INT, dest_pid INT)
INSERT INTO #IDS_inProject(site_id, src_id, dest_id)
SELECT	r.site_id, src_bug_id, dest_bug_id 
FROM relationship r
WHERE	site_id = @site_id and relationship_type = 1
AND		dest_bug_id in (SELECT id FROM bug WHERE site_id = @site_id AND	creation_ts >= @start_date and creation_ts < @end_date)
AND		src_bug_id in (
	--���ϴ� ���׸���Ʈ ����
	SELECT	id FROM bug 
	WHERE	site_id = @site_id and project_id = @project_id
	AND		creation_ts >= @start_date and creation_ts < @end_date
)
CREATE NONCLUSTERED INDEX tempidx2 ON #ids_inProject(dest_id)

--������Ʈ ���̵� ����
UPDATE	i 
SET		dest_pid = b.project_id
FROM	bug b 
JOIN	#ids_inProject i on b.id = i.dest_id

--3. ������Ʈ �� ���
select
	p.id
	,p.name
	,r.CNT
	,(CASE WHEN ri.CNT is null THEN 0
	       ELSE ri.CNT 
      END) CNTin
from project p
left join (
	select i.site_id, i.dest_pid project_id, count(*) CNT
	from #ids i
	group by i.site_id, i.dest_pid
) r on p.site_id = r.site_id and p.id = r.project_id
left join
(
	select i.site_id, i.dest_pid project_id, count(*) CNT
	from #ids_inProject i
	group by i.site_id, i.dest_pid
)ri on p.site_id = ri.site_id and p.id = ri.project_id
where r.CNT is not null
order by p.id



--4. �ӽ����̺� ����.
DROP TABLE #ids
DROP TABLE #ids_inProject


--������Ʈ ��Ī���
select
	 T.project_id
	,T.name, count(*) CNT
from
	(
	select
		 b.project_id
		,p.name
		,bs.src_bug_id
		,bs.dest_bug_id
	from bug b
	join (
		--�ߺ��� ���׸���Ʈ
		select site_id, src_bug_id, dest_bug_id from relationship r
		where site_id = @site_id and relationship_type = 1
		and src_bug_id in (select * from @ids)
	) bs on b.site_id = bs.site_id and b.id = bs.dest_bug_id
	join project p	on p.site_id = bs.site_id and p.id = b.project_id
) T 
group by T.project_id, T.name
order by T.project_id asc


select
	T.project_id, T.name, count(*) CNT
from
(
	--������Ʈ ��Ī���
	select
		 b.project_id
		,p.name
		,bs.src_bug_id
		,bs.dest_bug_id
	from bug b
	join (
		--�ߺ��� ���׸���Ʈ
		select site_id, src_bug_id, dest_bug_id from relationship r
		where site_id = @site_id and relationship_type = 1
		and dest_bug_id in (select id from bug 
			where site_id = @site_id 
			and creation_ts >= @start_date and creation_ts < @end_date
		)
		and src_bug_id in (
			--���ϴ� ���׸���Ʈ ����
			select id from bug 
			where site_id = @site_id and project_id = @project_id
			and creation_ts >= @start_date and creation_ts < @end_date
		)		
	) bs on b.site_id = bs.site_id and b.id = bs.dest_bug_id
	join project p	on p.site_id = @site_id and p.id = b.project_id
) T 
group by T.project_id, T.name
order by T.project_id asc



-------------------------------------------------------
-- �ߺ� ���� �� ���� ������Ʈ�� ���� ���׵��� ����.
-------------------------------------------------------
--������Ʈ ��Ī���

DECLARE @site_id	integer = 5
DECLARE @project_id integer = 16
DECLARE @start_date date	= '2004-01-01'
DECLARE @end_date	date	= '2005-01-01'
select
	 bs.src_bug_id
	,bs.dest_bug_id
from bug b
join (
	--�ߺ��� ���׸���Ʈ
	select site_id, src_bug_id, dest_bug_id from relationship r
	where site_id = @site_id and relationship_type = 1
	and dest_bug_id in (
		select id from bug 
		where site_id = @site_id 
		and creation_ts >= @start_date and creation_ts < @end_date
	)
	and src_bug_id in (
		--���ϴ� ���׸���Ʈ ����
		select id from bug 
		where site_id = @site_id and project_id = @project_id
		and creation_ts >= @start_date and creation_ts < @end_date
	)
) bs on b.site_id = bs.site_id and b.id = bs.dest_bug_id
where b.project_id = @project_id



-----------------------------------------------------------------
-- ������Ʈ ���� �� �м� �ν��Ͻ� ����.
-----------------------------------------------------------------
-- �ý��� ���ɻ� firefox�� ���ؼ��� �ϱ�� ����.
Use Analysis_Firefox
select * from [site]
select * from bug
SELECT * FROM project WHERE site_id = 5 and id in (16)

--Java ���α׷����� ����.
-- TF-IDF�� Python���α׷����� ����
-- 
select count(*) from tf_mode where id = 1
select count(*) from element_term where mode_id =1
select count(*) from term where mode_id = 1
--delete from tf_mode where id = 1
--delete from element_term where mode_id = 1
--delete from term where mode_id = 1
--DBCC CHECKIDENT('tf_mode',RESEED, 3)
--DBCC CHECKIDENT('stopword',RESEED, 3)


--delete from tf_mode			
--delete from element_term	
--delete from term			
--DBCC CHECKIDENT('tf_mode',RESEED, 0)
--DBCC CHECKIDENT('stopword',RESEED, 0)

--drop table tf_mode
--drop table element_term
--drop table term
--drop table stopword





use Analysis_Firefox2

select count(*) from bug

--������� ����
select src_bug_id, dest_bug_id from relationship where relationship_type = 1
and dest_bug_id in (select id from bug)






-----------------------------------------------------------------
-- ��ž���带 ����.
-----------------------------------------------------------------
use Analysis_Firefox3
select * from tf_mode

select * from stopword	where mode_id = 1	--�ҿ�� Ȯ��


--stopword���̺� �ҿ�� ����� (������ �κе� �߰�)
DECLARE @mode_id AS INTEGER =10
DECLARE @max AS INTEGER =12
SELECT @max = count(*) FROM TF_MODE
WHILE @mode_id <= @max
BEGIN
	EXEC makeStopword @mode_id=@mode_id, @site_id=5, @initSize=135
	PRINT N'ID '+CAST(@mode_id AS NVARCHAR(10)) + N' COMPLETED'
	SET @mode_id = @mode_id + 1
END
GO


--�ҿ�� ������� Ȯ��.
select t.id, t.value from term t 
left join stopword s on t.mode_id = s.mode_id and t.value = s.name
where s.name is null and t.mode_id = 1

select * from term where mode_id = 1


--��� ��忡 ���� ��ž���������� ����.
DECLARE @mode_id AS INTEGER =1
DECLARE @max AS INTEGER =9
SELECT @max = count(*) FROM TF_MODE
WHILE @mode_id <= @max
BEGIN
	EXEC setStopwords @mode_id
	PRINT N'ID '+CAST(@mode_id AS NVARCHAR(10)) + N' COMPLETED'
	SET @mode_id = @mode_id + 1
END

select * from tf_mode




-----------------------------------------------------------------
-- �ҿ� ���ŵ� ������ ���캽
----------------------------------------------------------------
select * from element_tf	where mode_id = 1 and stopword = 0
select * from bug_tf		where mode_id = 1 and stopword = 0
select * from project_tf	where mode_id = 1 and stopword = 0
select * from term_tf		where mode_id = 1 and stopword = 0 

select count(*) from element_tf	where mode_id = 1 and stopword = 0 and src_type = 'S'
select count(*) from element_tf	where mode_id = 1 and stopword = 0 and src_type = 'D'
select count(*) from element_tf	where mode_id = 1 and stopword = 0 and src_type = 'C'
select count(*) from element_tf	where mode_id = 1 and stopword = 0
select count(*) from bug_tf		where mode_id = 1 and stopword = 0
select count(*) from project_tf	where mode_id = 1 and stopword = 0
select count(*) from term_tf	where mode_id = 1 and stopword = 0

select count(*) from bug

--MODE 1, ANALYSIS = 4, �ҿ�� ����
--9343���� ���׾��̵�

--summary ���� ���� : 382��			�����ʿ�
--description ���� ���� : 1693��	    �����ʿ�
--comment ���� ���� : 15624��	    �����ʿ�
--��ü ���� : 17699��			    �����ʿ�
--


------------------------------------------------------------------
--Ÿ�� ���� ���̵� ���� �ߺ� ���׸���Ʈ ��
------------------------------------------------------------------
select 
	 dest_bug_id
	,count(*)
from relationship
where relationship_type = 1
and site_id = 5
group by dest_bug_id
having count(*) >1


------------------------------------------------------------------
--���� ���� ���̵� ���� �ߺ� ���׸���Ʈ ��
------------------------------------------------------------------
select 
	 src_bug_id
	,count(*)
from relationship
where relationship_type = 1
and site_id = 5
group by src_bug_id
having count(*) >1




-- ����� ���׸���Ʈ�� ���� +-15�� ������ ���׸���Ʈ.
select site_id, id, [status] from bug
where creation_ts >= dateadd(DAY, -15, (select creation_ts from bug where id = 178338))
and creation_ts < dateadd(DAY, +15, (select creation_ts from bug where id = 178338))



-- ����� ���׸���Ʈ�� ���� +-15�� �����̿� dest_bug_id�� �����ϴ��� Ȯ��
select * from relationship where src_bug_id = 178338



----------------------------------------------
-- ���õ� ���׵��� ���� ���̿� ����� ������ Ȯ��
----------------------------------------------
use uniBas
use Analysis_Dupe

--3768���� �ߺ�����
--2037���� ���׸��� ���� ������ ���׸���Ʈ �ȿ� ����.
--�Ʒ� �Լ� ���ǵǾ�����
select * from getRangeRalationship(5, 120) where isExist = 1		--120�� �̳��� 1321 ����
select * from getRangeRalationship(5, 60) where isExist = 1			--60�� �̳��� 1031 ����  (50%)
select * from getRangeRalationship(5, 30) where isExist = 1			--30�� �̳��� 843�� ����.
select * from getRangeRalationship(5, 15) where isExist = 1			--15�� �̳��� 640 ����

select id, creation_ts from bug where id in (178489, 130959, 178538, 86319)

--�������̺��� �ߺ� ���׵��� �߻����� ����
SELECT * FROM getDiffRelationship(6, 1)
SELECT * FROM getDiffRelationship(1, 1)




-----------------------------------------------------------------
-- �ߺ����� �Ǻ��� ���׵��� � ���絵�� �������� Ȯ��
-----------------------------------------------------------------
select * from relationship where relationship_type = 1

EXEC getSimilarity @mode_id = 1, @site_id=5, @project_id = 16





EXEC getSimilarityAll @mode_id = 1, @site_id=5, @project_id = 16


SELECT * FROM similarity ORDER BY VALUE DESC

drop TABLE SIMILARITY



-----------------------------------------------------------------
-- �ߺ����� �Ǻ��� ���׵��� ����
-----------------------------------------------------------------
--None(0),
--DuplicateOf(1), 	// dupe - dupe_of
--HasDuplicate(2), 	// dupe_of - dupe
--ChildOf(3),			// blocked - dependson		
--ParentOf(4),		// dependson - blocked
--RelatedTo(5);		// src - dest

--analysis_bug2�� �м��ؾ߰���.
--�ߺ����� ������ �ʹ� ����


select * from relationship

select * from [unibas]..[site]
select * from unibas..analysis
select * from unibas..analysis_project



select * from bug where site_id = 6 and id = 2920

select * from element_term where mode_id = 1 and site_id = 1 and bug_id = 2920



--15�� ������Ʈ�� ���� �ߺ� ���׸���Ʈ ����Ʈ
select * from relationship  r
where	site_id = 6
and		relationship_type = 1
and		src_bug_id in (select id from bug where site_id = r.site_id and project_id = 15)


select top 10 * from element_term

select * from bug_tf where mode_id = 1 and site_id = 6 and project_id = 15 and bug_id = 273
select * from bug_tf where mode_id = 1 and site_id = 6 and project_id = 15 and bug_id = 24514



EXEC setStopwords 1

select * from bug_tf 
where mode_id = 1 and site_id = 6 and project_id = 15 and bug_id = 414
and frequency > 2 and frequency <=5
select * from bug_tf 
where mode_id = 1 and site_id = 6 and project_id = 15 and bug_id = 415
and frequency > 2 and frequency <=5


select * from element_tf 
where mode_id = 1 and site_id = 6 and project_id = 15 and bug_id = 414 and src_type = 'S'
--and frequency > 2 and frequency <=5

select * from element_tf 
where mode_id = 1 and site_id = 6 and project_id = 15 and bug_id = 415 and src_type = 'S'




----------------------------------------------
--�� ���� ���� ���絵 �񱳸� ���� �ʱ�ȭ �Լ�
----------------------------------------------
--����� ���� ���̺� ���� (��忡 ���� �ܾ���� �����ϰ� ����.)
-- python���� �̰�



----------------------------------------------
--��õ  (python���� �̰�)
----------------------------------------------
--����� ���� ���̺� ���� (��忡 ���� �ܾ���� �����ϰ� ����.)

--������� ����



select v, count(*) cnt 
from (
	select distinct src_id, dest_id, cast(value*10 as integer) v 
	from recommand 
	where [check] = 1
	) s
group by v
order by v desc


------------------------------------------------------
-- �ߺ� ���׸���Ʈ�� ����(similarity���� ����)
------------------------------------------------------
select * from tf_mode


select T.value, count(*)
from (select cast((value * 10) as integer) value from similarity1)	T
group by T.value
order by T.value desc


select T.value, count(*)
from (select cast((value * 10) as integer) value from similarity1 where [check] = 1)	T
group by T.value
order by T.value desc



select count(*)
from similarity1 T
where T.value = 0


select count(*)
from similarity1 T
where T.value = 0
and [check] = 1
