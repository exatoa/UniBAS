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
from bug b
join project p on b.site_id = p.site_id and b.project_id = p.id
where b.site_id = 5
group by project_id, p.name


use uniBAS
------------------------------------------------------------------
-- �м� ��� ������Ʈ ���� (������Ʈ�� �ߺ� ���׸���Ʈ ��,���� ��)
------------------------------------------------------------------
--UniBAS���� �м� ��� ������Ʈ �� �˻�.
SELECT 
	 p.id
	,p.name
	,(select count(*) from bug where site_id = p.site_id and project_id = p.id) Bugs
	,PR.dup_cnt Relations
	,PN.dup_cnt Dups
	
FROM project p
JOIN (	SELECT
			 b.site_id
			,b.project_id
			,count(*)   dup_cnt
		FROM bug b
		JOIN relationship r on b.site_id = r.site_id and b.id = r.src_bug_id
		WHERE b.site_id = 5
		AND r.relationship_type=1
		GROUP BY b.site_id, b.project_id
	) PN on p.site_id = PN.site_id and  p.id = PN.project_id
JOIN (	SELECT
			 b.site_id
			,b.project_id
			,count(*)   dup_cnt
		FROM bug b
		JOIN relationship r on b.site_id = r.site_id and b.id = r.src_bug_id
		WHERE b.site_id = 5
		--AND r.relationship_type=1
		GROUP BY b.site_id, b.project_id
	) PR on p.site_id = PR.site_id and  p.id = PR.project_id
WHERE p.site_id = 5
ORDER BY p.id asc


-------------------------------------------------
--�ֺ� ���׸���Ʈ �� Ȯ��
-------------------------------------------------
select * from getCountEachWeek(5, 7, null,null)

select * from getCountEachWeek(5, 7, '2001-07-01','2002-12-31')
select * from getCountEachMonth(5, 7, '2001-07-01','2002-12-31')


--��ü ���׸���Ʈ
DECLARE @site_id	integer = 5
DECLARE @project_id integer = 7
DECLARE @start_date date	= '2001-07-01'
DECLARE @end_date	date	= '2003-01-01'
select id, project_id, creation_ts from bug 
where site_id = @site_id and project_id = @project_id
and creation_ts >= @start_date and creation_ts < @end_date


--site���� : 5			(MozillaE)
--������Ʈ ���� : 7   (Bugzilla)
--���� : 2962
--�ߺ����� : 717
--�Ⱓ���� : 2001-07-01   ~ 2003-01-01
--���׸���Ʈ�� ���ⷮ�� ���� ���ⷮ�� ��ȭ�� ū �Ⱓ ����

--���� (���ù���) : 2962
--�ߺ�����(���ù���) : 717		(�ٸ� ������Ʈ�� ���ؼ��� �Ⱓ ����)
--�ߺ�����(����������Ʈ) : 178

DECLARE @site_id	integer = 5
DECLARE @project_id integer = 7
DECLARE @start_date date	= '2001-07-01'
DECLARE @end_date	date	= '2003-01-01'

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
	--and dest_bug_id <= (select max(id) from bug) 
		and dest_bug_id in (select id from bug 
			where site_id = @site_id 
			and creation_ts >= @start_date and creation_ts < @end_date)
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
use uniBAS


DECLARE @site_id	integer = 5
DECLARE @project_id integer = 7
DECLARE @start_date date	= '2001-07-01'
DECLARE @end_date	date	= '2003-01-01'

--src�� ���Ͽ� �����ϴ� �ߺ� ���׸���Ʈ�� ��ġ�� �ٸ� �� ����. (�������� ���� ������ ���׸���Ʈ�� ����Ű�� ��쵵 �����Ƿ�.

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
		--and dest_bug_id <= (select max(id) from bug) 
		and dest_bug_id in (select id from bug 
			where site_id = @site_id 
			and creation_ts >= @start_date and creation_ts < @end_date)
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
DECLARE @project_id integer = 7
DECLARE @start_date date	= '2002-01-01'
DECLARE @end_date	date	= '2003-01-01'
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


use Analysis_Bugzilla2
select * from similarity1 where src_id = 128422 and dest_id = 128437

-----------------------------------------------------------------
-- ������Ʈ ���� �� �м� �ν��Ͻ� ����.
-----------------------------------------------------------------
-- �ý��� ���ɻ� firefox�� ���ؼ��� �ϱ�� ����.
Use Analysis_Firefox
Use Analysis_Bugzilla2
select * from [site]
select * from bug
SELECT * FROM project WHERE site_id = 5 and id in (7)

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





use Analysis_Bugzilla4

select count(*) from bug

--������� ����
select src_bug_id, dest_bug_id from relationship where relationship_type = 1
and dest_bug_id in (select id from bug)



-----------------------------------------------------------------
-- ��ž���带 ����.
-----------------------------------------------------------------
use Analysis_Bugzilla4


--stopword���̺� �ҿ�� ����� (������ �κе� �߰�)
EXEC makeStopword @mode_id=3, @site_id=5, @initSize=135
EXEC makeStopword @mode_id=4, @site_id=5, @initSize=135
EXEC makeStopword @mode_id=5, @site_id=5, @initSize=135


--��� ��忡 ���� ��ž���������� ����. (������ stopword�� element_term ��  stopword���� ������Ʈ)
DECLARE @mode_id AS INTEGER =3
DECLARE @max AS INTEGER =1
SELECT @max = count(*) FROM TF_MODE
WHILE @mode_id <= @max
BEGIN
	EXEC setStopwords @mode_id
	PRINT N'ID '+CAST(@mode_id AS NVARCHAR(10)) + N' COMPLETED'
	SET @mode_id = @mode_id + 1
END


--python���α׷� ���� (similarity ����)
--: ���������� ����� ����




---------------------------------------
--IDF�� ������ ������  (��ž���� �������� ���� �Ĵ� �ٸ�)
---------------------------------------
--stopword�� üŷ�� �Ͱ� �׷��� ���� ���� ���̰� ����.
select * from element_term where mode_id = 1 and stopword = 1

--���� Ȯ�� ���� (stopword�� ������� ����)
--                stopword�� ���������� ������ ũ��� ��θ� �����ؾ���.
select 
	 value/10.0 as IDF
	,count(*) CNT
from  (select term_id, cast((value*10) as integer) value from getIDF(1,5) ) g
group by value
order by value



select count(*) from term
select count(*) from stopword






select * from tf_mode						--��� ���� Ȯ��
select * from stopword	where mode_id = 1	--�ҿ�� Ȯ��
select	* from	term t where t.mode_id = 1	--�������� ��ü ���

--stopword�� �ִ� �ܾ��� term�� ���� �� ���� �� ���� (�׷��� ������ ���̰� ����.
--�������� ��ž���尡 �ƴ� ���
select	 t.value 
from	term t
WHERE	t.mode_id = 1
and		t.value not in (SELECT name FROM stopword WHERE mode_id =1)




	
--��ü ������Ʈ�� : 255176
--Ȱ����� �� :  78592
--�ҿ���� �� : 176584
select count(*) from element_term where mode_id =2 and site_id = 5 and stopword = 0
select count(*) from element_term where mode_id =2 and site_id = 5 and stopword = 1



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
use Analysis_Bugzilla2

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




--------------------------------------------------------------------------
-- �ߺ����� �Ǻ��� ���׵��� � ���絵�� �������� Ȯ�� (���̽����� �̰�)
--------------------------------------------------------------------------
select * from relationship where relationship_type = 1


--������ IDF��
select * from getIDF(2,5) where term_id in (456,696)


select
	 t.src_id
	,N'https://bugzilla.mozilla.org/show_bug.cgi?id=' + cast(src_id as nvarchar) url_src
from
(
	select src_id from similarity2 where value >= 0.9 and value < 1.0
	union
	select dest_id from similarity2 where value >= 0.9 and value < 1.0
)t



-------------------------------------------------------------------------------------
-- �������� ��� Ȯ��.
-------------------------------------------------------------------------------------
--src�� dest�� �̰͵��� ��Ÿ������ Ȯ��.
--�̹� �ߺ��� ���׵��� ������ ����Ʈ
select
	 src_id
	,dest_id
	,N'https://bugzilla.mozilla.org/show_bug.cgi?id=' + cast(src_id as nvarchar) url_src
	,N'https://bugzilla.mozilla.org/show_bug.cgi?id=' + cast(dest_id as nvarchar) url_dest
	,value 
	,[check]
from similarity3
where [check] = 1 and value >= 0.5
order by value desc



--��ü ���� ���� Ȯ��.
select g, count(*) from 
(select *, cast(value*10 as integer) g from similarity4)s
group by g
order by g desc

select count(*) from similarity1 where  value = 0.0
select count(*) from similarity2 where  value = 0.0
select count(*) from similarity3 where  value = 0.0
select count(*) from similarity4 where  value = 0.0


--���� ���� Ȯ��.
select g, count(*) from 
(select *, cast(value*10 as integer) g from similarity5 where [check] = 1)s
group by g
order by g desc


--���� ���� Ȯ��.
select count(*) from similarity1 where [check] = 0 and value = 0.0
select count(*) from similarity2 where [check] = 0 and value = 0.0
select count(*) from similarity3 where [check] = 0 and value = 0.0
select count(*) from similarity4 where [check] = 0 and value = 0.0

-------------------------------------------------------------------------------------
-- ���׸���Ʈ ��õ Ȯ��
-------------------------------------------------------------------------------------
--�ߺ� ���׸���Ʈ ��õ�ϵ��� ��     ::: ���� TOP 1, threshold = 0   (6�� AM01:54)
EXEC makeRecommandList @site_id=5, @top_size= 1, @target_db=N'similarity3'
EXEC makeRecommandList @site_id=5, @top_size= 3, @target_db=N'similarity3'

--python���� ��ü.
EXEC distSimilarityRecommand



--�ߺ� ��õ ���׸���Ʈ�� ���絵 �� ���� (������ ���� �͵鸸..)
select v, count(*) cnt 
from (
	select distinct src_id, dest_id, cast(value*10 as integer) v 
	from recommand 
	where [check] = 1
	) s
group by v
order by v desc





--������ �ƴ� ���׸���Ʈ�� ���Ͽ� �ߺ�����Ʈ ������
select distinct 
	 src_id
	,dest_id
	,value 
	,N'https://bugzilla.mozilla.org/show_bug.cgi?id=' + cast(src_id as nvarchar) url_src
	,N'https://bugzilla.mozilla.org/show_bug.cgi?id=' + cast(dest_id as nvarchar) url_dest
from recommand where [check] = 1
and src_id = 133283 and dest_id=133425


select * from similarity1 where src_id = 133425 order by value desc


------------------------------------------------------------------
--�ߺ� ��õ ���׸���Ʈ�� ���絵 �� ���� (������ ���� �͵鸸..)
------------------------------------------------------------------
CREATE PROCEDURE distSimilarityRecommand
AS
BEGIN
	select v, count(*) cnt 
	from (
		select distinct src_id, dest_id, cast(value*10 as integer) v 
		from recommand 
		where [check] = 1
		) s
	group by v
	order by v desc
END

------------------------------------------------------
-- ���׸���Ʈ�� ������ ��
------------------------------------------------------
select s.[group]*10, count(*) cnt from 
(
	Select diff, diff/10 [group] 
	from getDiffRelationship(5, 1) where diff is not null
) s
group by s.[group]


