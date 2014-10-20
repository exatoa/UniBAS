use UniBAS
------------------------------------------------------------------
--사이트 정보 확인
------------------------------------------------------------------
select * from [site]
select * from analysis


------------------------------------------------------------------
--전체 버그리포트 수
------------------------------------------------------------------
select count(*) from bug where site_id = 5


-------------------------------------------------
--상태별 버그리포트 분포
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
--프로젝트별 버그리포트 제출율
------------------------------------------------------------------
select project_id, p.name, count(*) CNT
from project p 
left join bug b on b.site_id = p.site_id and b.project_id = p.id
where p.site_id = 5
and b.id<= 322027
group by project_id, p.name



------------------------------------------------------------------
-- 분석 대상 프로젝트 선정 (프로젝트별 중복 버그리포트 수,버그 수)
------------------------------------------------------------------
--UniBAS에서 분석 대상 프로젝트 를 검색.
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
--주별 버그리포트 수 확인
-------------------------------------------------
select * from getCountEachWeek(5, 16, null,null)

select * from getCountEachWeek(5, 16, '2004-01-01','2004-12-31')


--전체 버그리포트
DECLARE @site_id	integer = 5
DECLARE @project_id integer = 16
DECLARE @start_date date	= '2004-01-01'
DECLARE @end_date	date	= '2005-01-01'
select id, project_id, creation_ts from bug 
where site_id = @site_id and project_id = @project_id
and creation_ts >= @start_date and creation_ts < @end_date


--site선정 : 5			(MozillaE)
--프로젝트 선정 : 16   (firefox)
--버그 : 30,347
--중복버그 : 11,919
--기간선정 : 2004-01-01   ~ 2005-01-01
--버그리포트의 제출량도 높고 제출량의 변화도 큰 기간 선정

--버그 (선택범위) : 11,742
--중복버그(선택범위) : 4,738
--중복버그(선택범위) : 4,691  (수집범위 밖 제외)
--중복버그(동일프로젝트) : 1,668   

DECLARE @site_id	integer = 5
DECLARE @project_id integer = 16
DECLARE @start_date date	= '2004-01-01'
DECLARE @end_date	date	= '2005-01-01'

-------------------------------------------------------
-- 중복된 프로젝트가 속한 프로젝트 정보 (상세정보)
-------------------------------------------------------
--프로젝트 명칭출력
select
	b.project_id
	,p.name
	,bs.src_bug_id
	,bs.dest_bug_id
from bug b
join (
	--중복된 버그리포트
	select site_id, src_bug_id, dest_bug_id from relationship r
	where site_id = @site_id and relationship_type = 1
	and dest_bug_id <= (select max(id) from bug)
	and src_bug_id in (
		--원하는 버그리포트 선택
		select id from bug 
		where site_id = @site_id and project_id = @project_id
		and creation_ts >= @start_date and creation_ts < @end_date
	)
) bs on b.site_id = bs.site_id and b.id = bs.dest_bug_id
join project p	on p.site_id = @site_id and p.id = b.project_id
order by project_id

-------------------------------------------------------
-- 중복 프로젝트와 다른 프로젝트 간의 관계 (수치)
-------------------------------------------------------

DECLARE @site_id	integer = 5
DECLARE @project_id integer = 16
DECLARE @start_date date	= '2004-01-01'
DECLARE @end_date	date	= '2005-01-01'

--1. 전체 중복 버그리포트 리스트
CREATE TABLE #ids (site_id INT, src_id INT, dest_id INT, dest_pid INT)
INSERT INTO #IDS(site_id, src_id, dest_id)
SELECT	r.site_id, src_bug_id, dest_bug_id 
FROM relationship r
WHERE	site_id = @site_id and relationship_type = 1
AND		dest_bug_id < (SELECT max(id) FROM bug WHERE site_id = @site_id)
AND		src_bug_id in (
	--원하는 버그리포트 선택
	SELECT	id FROM bug 
	WHERE	site_id = @site_id and project_id = @project_id
	AND		creation_ts >= @start_date and creation_ts < @end_date
)		
CREATE NONCLUSTERED INDEX tempidx ON #ids(dest_id)

--프로젝트 아이디 적용
UPDATE	i 
SET		dest_pid = b.project_id
FROM	bug b 
JOIN	#ids i on b.id = i.dest_id


--2. 선택한 프로젝트 내의 중복 버그리포트 리스트
CREATE TABLE #ids_inProject (site_id INT, src_id INT, dest_id INT, dest_pid INT)
INSERT INTO #IDS_inProject(site_id, src_id, dest_id)
SELECT	r.site_id, src_bug_id, dest_bug_id 
FROM relationship r
WHERE	site_id = @site_id and relationship_type = 1
AND		dest_bug_id in (SELECT id FROM bug WHERE site_id = @site_id AND	creation_ts >= @start_date and creation_ts < @end_date)
AND		src_bug_id in (
	--원하는 버그리포트 선택
	SELECT	id FROM bug 
	WHERE	site_id = @site_id and project_id = @project_id
	AND		creation_ts >= @start_date and creation_ts < @end_date
)
CREATE NONCLUSTERED INDEX tempidx2 ON #ids_inProject(dest_id)

--프로젝트 아이디 적용
UPDATE	i 
SET		dest_pid = b.project_id
FROM	bug b 
JOIN	#ids_inProject i on b.id = i.dest_id

--3. 프로젝트 별 통계
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



--4. 임시테이블 정리.
DROP TABLE #ids
DROP TABLE #ids_inProject


--프로젝트 명칭출력
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
		--중복된 버그리포트
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
	--프로젝트 명칭출력
	select
		 b.project_id
		,p.name
		,bs.src_bug_id
		,bs.dest_bug_id
	from bug b
	join (
		--중복된 버그리포트
		select site_id, src_bug_id, dest_bug_id from relationship r
		where site_id = @site_id and relationship_type = 1
		and dest_bug_id in (select id from bug 
			where site_id = @site_id 
			and creation_ts >= @start_date and creation_ts < @end_date
		)
		and src_bug_id in (
			--원하는 버그리포트 선택
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
-- 중복 버그 중 같은 프로젝트에 속한 버그들을 선택.
-------------------------------------------------------
--프로젝트 명칭출력

DECLARE @site_id	integer = 5
DECLARE @project_id integer = 16
DECLARE @start_date date	= '2004-01-01'
DECLARE @end_date	date	= '2005-01-01'
select
	 bs.src_bug_id
	,bs.dest_bug_id
from bug b
join (
	--중복된 버그리포트
	select site_id, src_bug_id, dest_bug_id from relationship r
	where site_id = @site_id and relationship_type = 1
	and dest_bug_id in (
		select id from bug 
		where site_id = @site_id 
		and creation_ts >= @start_date and creation_ts < @end_date
	)
	and src_bug_id in (
		--원하는 버그리포트 선택
		select id from bug 
		where site_id = @site_id and project_id = @project_id
		and creation_ts >= @start_date and creation_ts < @end_date
	)
) bs on b.site_id = bs.site_id and b.id = bs.dest_bug_id
where b.project_id = @project_id



-----------------------------------------------------------------
-- 프로젝트 선정 및 분석 인스턴스 생성.
-----------------------------------------------------------------
-- 시스템 성능상 firefox에 대해서만 하기로 결정.
Use Analysis_Firefox
select * from [site]
select * from bug
SELECT * FROM project WHERE site_id = 5 and id in (16)

--Java 프로그램에서 실행.
-- TF-IDF는 Python프로그램에서 실행
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

--정답셋의 범위
select src_bug_id, dest_bug_id from relationship where relationship_type = 1
and dest_bug_id in (select id from bug)






-----------------------------------------------------------------
-- 스탑워드를 제거.
-----------------------------------------------------------------
use Analysis_Firefox3
select * from tf_mode

select * from stopword	where mode_id = 1	--불용어 확인


--stopword테이블에 불용어 재생성 (미흡한 부분들 추가)
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


--불용어 생성결과 확인.
select t.id, t.value from term t 
left join stopword s on t.mode_id = s.mode_id and t.value = s.name
where s.name is null and t.mode_id = 1

select * from term where mode_id = 1


--모든 모드에 대해 스탑워드정보를 갱신.
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
-- 불용어가 제거된 값들을 살펴봄
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

--MODE 1, ANALYSIS = 4, 불용어 제거
--9343개의 버그아이디

--summary 관련 어휘 : 382개			개정필요
--description 관련 어휘 : 1693개	    개정필요
--comment 포함 어휘 : 15624개	    개정필요
--전체 어휘 : 17699개			    개정필요
--


------------------------------------------------------------------
--타겟 버그 아이디에 대한 중복 버그리포트 수
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
--원본 버그 아이디에 대한 중복 버그리포트 수
------------------------------------------------------------------
select 
	 src_bug_id
	,count(*)
from relationship
where relationship_type = 1
and site_id = 5
group by src_bug_id
having count(*) >1




-- 제출된 버그리포트의 양쪽 +-15일 사이의 버그리포트.
select site_id, id, [status] from bug
where creation_ts >= dateadd(DAY, -15, (select creation_ts from bug where id = 178338))
and creation_ts < dateadd(DAY, +15, (select creation_ts from bug where id = 178338))



-- 제출된 버그리포트의 양쪽 +-15일 사이이에 dest_bug_id가 존재하는지 확인
select * from relationship where src_bug_id = 178338



----------------------------------------------
-- 관련된 버그들이 몇일 사이에 제출된 것인지 확인
----------------------------------------------
use uniBas
use Analysis_Dupe

--3768개의 중복버그
--2037개의 버그만이 현재 수집된 버그리스트 안에 존재.
--아래 함수 정의되어있음
select * from getRangeRalationship(5, 120) where isExist = 1		--120일 이내에 1321 제출
select * from getRangeRalationship(5, 60) where isExist = 1			--60일 이내에 1031 제출  (50%)
select * from getRangeRalationship(5, 30) where isExist = 1			--30일 이내에 843개 제출.
select * from getRangeRalationship(5, 15) where isExist = 1			--15일 이내에 640 제출

select id, creation_ts from bug where id in (178489, 130959, 178538, 86319)

--관계테이블의 중복 버그들의 발생일의 차이
SELECT * FROM getDiffRelationship(6, 1)
SELECT * FROM getDiffRelationship(1, 1)




-----------------------------------------------------------------
-- 중복으로 판별된 버그들이 어떤 유사도를 가지는지 확인
-----------------------------------------------------------------
select * from relationship where relationship_type = 1

EXEC getSimilarity @mode_id = 1, @site_id=5, @project_id = 16





EXEC getSimilarityAll @mode_id = 1, @site_id=5, @project_id = 16


SELECT * FROM similarity ORDER BY VALUE DESC

drop TABLE SIMILARITY



-----------------------------------------------------------------
-- 중복으로 판별된 버그들을 선정
-----------------------------------------------------------------
--None(0),
--DuplicateOf(1), 	// dupe - dupe_of
--HasDuplicate(2), 	// dupe_of - dupe
--ChildOf(3),			// blocked - dependson		
--ParentOf(4),		// dependson - blocked
--RelatedTo(5);		// src - dest

--analysis_bug2로 분석해야겠음.
--중복문서 개수가 너무 적음


select * from relationship

select * from [unibas]..[site]
select * from unibas..analysis
select * from unibas..analysis_project



select * from bug where site_id = 6 and id = 2920

select * from element_term where mode_id = 1 and site_id = 1 and bug_id = 2920



--15번 프로젝트에 속한 중복 버그리포트 리스트
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
--두 문서 간의 유사도 비교를 위한 초기화 함수
----------------------------------------------
--계산을 위한 테이블 생성 (모드에 속한 단어들을 포함하고 있음.)
-- python으로 이관



----------------------------------------------
--추천  (python으로 이관)
----------------------------------------------
--계산을 위한 테이블 생성 (모드에 속한 단어들을 포함하고 있음.)

--정답셋의 분포



select v, count(*) cnt 
from (
	select distinct src_id, dest_id, cast(value*10 as integer) v 
	from recommand 
	where [check] = 1
	) s
group by v
order by v desc


------------------------------------------------------
-- 중복 버그리포트의 분포(similarity값에 따른)
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
