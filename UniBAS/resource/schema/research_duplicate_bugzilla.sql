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
from bug b
join project p on b.site_id = p.site_id and b.project_id = p.id
where b.site_id = 5
group by project_id, p.name


use uniBAS
------------------------------------------------------------------
-- 분석 대상 프로젝트 선정 (프로젝트별 중복 버그리포트 수,버그 수)
------------------------------------------------------------------
--UniBAS에서 분석 대상 프로젝트 를 검색.
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
--주별 버그리포트 수 확인
-------------------------------------------------
select * from getCountEachWeek(5, 7, null,null)

select * from getCountEachWeek(5, 7, '2001-07-01','2002-12-31')
select * from getCountEachMonth(5, 7, '2001-07-01','2002-12-31')


--전체 버그리포트
DECLARE @site_id	integer = 5
DECLARE @project_id integer = 7
DECLARE @start_date date	= '2001-07-01'
DECLARE @end_date	date	= '2003-01-01'
select id, project_id, creation_ts from bug 
where site_id = @site_id and project_id = @project_id
and creation_ts >= @start_date and creation_ts < @end_date


--site선정 : 5			(MozillaE)
--프로젝트 선정 : 7   (Bugzilla)
--버그 : 2962
--중복버그 : 717
--기간선정 : 2001-07-01   ~ 2003-01-01
--버그리포트의 제출량도 높고 제출량의 변화도 큰 기간 선정

--버그 (선택범위) : 2962
--중복버그(선택범위) : 717		(다른 프로젝트에 대해서도 기간 적용)
--중복버그(동일프로젝트) : 178

DECLARE @site_id	integer = 5
DECLARE @project_id integer = 7
DECLARE @start_date date	= '2001-07-01'
DECLARE @end_date	date	= '2003-01-01'

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
	--and dest_bug_id <= (select max(id) from bug) 
		and dest_bug_id in (select id from bug 
			where site_id = @site_id 
			and creation_ts >= @start_date and creation_ts < @end_date)
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
use uniBAS


DECLARE @site_id	integer = 5
DECLARE @project_id integer = 7
DECLARE @start_date date	= '2001-07-01'
DECLARE @end_date	date	= '2003-01-01'

--src에 대하여 존재하는 중복 버그리포트와 수치가 다를 수 있음. (수집되지 않은 범위의 버그리포트를 가르키는 경우도 있으므로.

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
		--and dest_bug_id <= (select max(id) from bug) 
		and dest_bug_id in (select id from bug 
			where site_id = @site_id 
			and creation_ts >= @start_date and creation_ts < @end_date)
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
DECLARE @project_id integer = 7
DECLARE @start_date date	= '2002-01-01'
DECLARE @end_date	date	= '2003-01-01'
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


use Analysis_Bugzilla2
select * from similarity1 where src_id = 128422 and dest_id = 128437

-----------------------------------------------------------------
-- 프로젝트 선정 및 분석 인스턴스 생성.
-----------------------------------------------------------------
-- 시스템 성능상 firefox에 대해서만 하기로 결정.
Use Analysis_Firefox
Use Analysis_Bugzilla2
select * from [site]
select * from bug
SELECT * FROM project WHERE site_id = 5 and id in (7)

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





use Analysis_Bugzilla4

select count(*) from bug

--정답셋의 범위
select src_bug_id, dest_bug_id from relationship where relationship_type = 1
and dest_bug_id in (select id from bug)



-----------------------------------------------------------------
-- 스탑워드를 제거.
-----------------------------------------------------------------
use Analysis_Bugzilla4


--stopword테이블에 불용어 재생성 (미흡한 부분들 추가)
EXEC makeStopword @mode_id=3, @site_id=5, @initSize=135
EXEC makeStopword @mode_id=4, @site_id=5, @initSize=135
EXEC makeStopword @mode_id=5, @site_id=5, @initSize=135


--모든 모드에 대해 스탑워드정보를 갱신. (생성된 stopword로 element_term 의  stopword정보 업데이트)
DECLARE @mode_id AS INTEGER =3
DECLARE @max AS INTEGER =1
SELECT @max = count(*) FROM TF_MODE
WHILE @mode_id <= @max
BEGIN
	EXEC setStopwords @mode_id
	PRINT N'ID '+CAST(@mode_id AS NVARCHAR(10)) + N' COMPLETED'
	SET @mode_id = @mode_id + 1
END


--python프로그램 실행 (similarity 생성)
--: 쿼리에서는 결과가 느림




---------------------------------------
--IDF의 분포를 보여줌  (스탑워드 제거전과 제거 후는 다름)
---------------------------------------
--stopword가 체킹된 것과 그렇지 않은 것의 차이가 있음.
select * from element_term where mode_id = 1 and stopword = 1

--분포 확인 쿼리 (stopword와 상관없이 구함)
--                stopword를 제외했지만 문서의 크기는 모두를 포함해야함.
select 
	 value/10.0 as IDF
	,count(*) CNT
from  (select term_id, cast((value*10) as integer) value from getIDF(1,5) ) g
group by value
order by value



select count(*) from term
select count(*) from stopword






select * from tf_mode						--모드 종류 확인
select * from stopword	where mode_id = 1	--불용어 확인
select	* from	term t where t.mode_id = 1	--현재모드의 전체 용어

--stopword에 있는 단어중 term에 없는 게 있을 수 있음 (그래서 개수의 차이가 생김.
--현재모드의 스탑워드가 아닌 용어
select	 t.value 
from	term t
WHERE	t.mode_id = 1
and		t.value not in (SELECT name FROM stopword WHERE mode_id =1)




	
--전체 엘러먼트수 : 255176
--활용어의 수 :  78592
--불용어의 수 : 176584
select count(*) from element_term where mode_id =2 and site_id = 5 and stopword = 0
select count(*) from element_term where mode_id =2 and site_id = 5 and stopword = 1



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
use Analysis_Bugzilla2

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




--------------------------------------------------------------------------
-- 중복으로 판별된 버그들이 어떤 유사도를 가지는지 확인 (파이썬으로 이관)
--------------------------------------------------------------------------
select * from relationship where relationship_type = 1


--어휘의 IDF값
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
-- 데이터의 결과 확인.
-------------------------------------------------------------------------------------
--src나 dest에 이것들이 나타나는지 확인.
--이미 중복인 버그들의 데이터 리스트
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



--전체 값의 분포 확인.
select g, count(*) from 
(select *, cast(value*10 as integer) g from similarity4)s
group by g
order by g desc

select count(*) from similarity1 where  value = 0.0
select count(*) from similarity2 where  value = 0.0
select count(*) from similarity3 where  value = 0.0
select count(*) from similarity4 where  value = 0.0


--값의 분포 확인.
select g, count(*) from 
(select *, cast(value*10 as integer) g from similarity5 where [check] = 1)s
group by g
order by g desc


--값의 분포 확인.
select count(*) from similarity1 where [check] = 0 and value = 0.0
select count(*) from similarity2 where [check] = 0 and value = 0.0
select count(*) from similarity3 where [check] = 0 and value = 0.0
select count(*) from similarity4 where [check] = 0 and value = 0.0

-------------------------------------------------------------------------------------
-- 버그리포트 추천 확인
-------------------------------------------------------------------------------------
--중복 버그리포트 추천하도록 함     ::: 현재 TOP 1, threshold = 0   (6일 AM01:54)
EXEC makeRecommandList @site_id=5, @top_size= 1, @target_db=N'similarity3'
EXEC makeRecommandList @site_id=5, @top_size= 3, @target_db=N'similarity3'

--python으로 대체.
EXEC distSimilarityRecommand



--중복 추천 버그리포트의 유사도 값 분포 (정답을 맞춘 것들만..)
select v, count(*) cnt 
from (
	select distinct src_id, dest_id, cast(value*10 as integer) v 
	from recommand 
	where [check] = 1
	) s
group by v
order by v desc





--정답이 아닌 버그리포트에 대하여 중복리스트 보여줌
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
--중복 추천 버그리포트의 유사도 값 분포 (정답을 맞춘 것들만..)
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
-- 버그리포트의 제출율 비교
------------------------------------------------------
select s.[group]*10, count(*) cnt from 
(
	Select diff, diff/10 [group] 
	from getDiffRelationship(5, 1) where diff is not null
) s
group by s.[group]


