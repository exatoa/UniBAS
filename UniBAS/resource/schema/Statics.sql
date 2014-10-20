use Bug_InfoDB


select
	 id
	 ,sname as Name
	 ,bts_type
	 ,create_ts
	 ,status
	 ,start_ts
	 ,end_ts
	 ,(select count(*) from Bug_MozillaZ..bug) cnt
from target_info
where id = 3

select
	 * 
from statistic


exec [dbo].[statisticBugs]

use bugzilla_Test10

select count(*) from bug




--------------------------------------------------------------
-- 각 product내의 컴포넌트 별 버그 발생율
--------------------------------------------------------------

select
	 [Name]
	,[Count]
	,cast( (([Count] / cast([all] as float))*100) as decimal(5,2)) as [Percent]
from (
	SELECT
		 p.name as [Name]
		,count(*) as [Count]
	FROM bug b
	JOIN product p  on b.product_id = p.id
	where b.creation_ts > '2010-06-01' and b.creation_ts < '2010-07-01'
	group by b.product_id , p.name

) b
cross join (
	select count(*) as [all] from bug 
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
) a 
order by [Name]



--------------------------------------------------------------
-- product내의 등록된버그리포트 중에 컴포넌트 별 버그 발생율
--------------------------------------------------------------
select
	 [Name]
	,[Count]
	,cast( (([Count] / cast([all] as float))*100) as decimal(5,2)) as [Percent]
from (
	SELECT
		 c.name as [Name]
		,count(*) as [Count]
	FROM bug b
	JOIN component c  on b.component_id = c.id
	where b.creation_ts > '2010-06-01' and b.creation_ts < '2010-07-01'
	and b.product_id = (SELECT id FROM product WHERE name=N'Firefox')
	group by c.name

) b
cross join (
	select count(*) as [all] from bug 
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
	and product_id = (SELECT id FROM product WHERE name=N'Firefox')
) a 
order by [Name]





--------------------------------------------------------------
-- 버그리포트 status 비율
--------------------------------------------------------------
select
	 status	as [Status]
	,CNT	as [Count]
	,cast( ((CNT / cast([all]as float))*100) as decimal(10,5)) as [%]
from (
	SELECT status, count(*) as CNT	FROM bug
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
	group by status
) b
cross join (
	select count(*) as [all] from bug 
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
) a 


--------------------------------------------------------------
-- 버그리포트 resolution 비율
--------------------------------------------------------------
select
	 resolution as Resolution
	,CNT		as [Count]
	,cast( ((CNT / cast([all]as float))*100) as decimal(5,2)) as [%]
from (
	SELECT resolution, count(*) as CNT	FROM bug
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
	group by resolution
) b
cross join (
	select count(*) as [all] from bug 
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
) a 


--------------------------------------------------------------
-- 버그리포트 심각도 비율
--------------------------------------------------------------
select
	 severity	as [Severity]
	,CNT		as [Count]
	,cast( ((CNT / cast([all]as float))*100) as decimal(5,2)) as [%]
from (
	SELECT severity, count(*) as CNT	FROM bug
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
	group by severity
) b
cross join (
	select count(*) as [all] from bug 
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
) a 


--------------------------------------------------------------
-- 버그리포트 priority 비율
--------------------------------------------------------------
select
	 [priority]	as [Priority]
	,CNT		as [Count]
	,cast( ((CNT / cast([all]as float))*100) as decimal(5,2)) as [%]
from (
	SELECT [priority], count(*) as CNT	FROM bug
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
	group by [priority]
) b
cross join (
	select count(*) as [all] from bug 
	where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
) a 
order by [priority]



--------------------------------------------------------------
-- 2010년 6월 한달간 핫 이슈 리포트 (top 15)
--------------------------------------------------------------
select top 15
	 b.id
	 ,b.summary
	 ,b.[status]
	 ,b.[resolution]
	 ,(select count(*) from bugnote where bug_id =  b.id) as NOTE_CNT
	 ,(select count(*) from bug_history where bug_id =  b.id) as History_CNT
	 ,(select count(*) from attachment where bug_id =  b.id) as Attach_CNT
	 ,b.creation_ts
from bug b 
where creation_ts > '2010-06-01' and creation_ts <= '2010-07-01'
--and (select count(*) from bugnote where bug_id =  b.id) >=100
order by NOTE_CNT desc



--------------------------------------------------------------
-- 2010년 6월 한달간 핫 이슈 리포트 (reopened만)
--------------------------------------------------------------
select 
	 b.id
	 ,b.summary
	 ,b.[status]
	 ,b.[resolution]
	 ,(select count(*) from bugnote where bug_id =  b.id) as NOTE_CNT
	 ,(select count(*) from bug_history where bug_id =  b.id) as History_CNT
	 ,(select count(*) from attachment where bug_id =  b.id) as Attach_CNT
	 ,b.creation_ts
from bug b 
where creation_ts > '2010-06-01' and creation_ts <= '2010-07-01'
and [status] ='REOPENED'
order by NOTE_CNT desc




--------------------------------------------------------------
-- 2010년 6월 한달간 핫 이슈 리포트 (new인 리포트만)
--------------------------------------------------------------
select 
	 b.id
	 ,b.summary
	 ,b.[status]
	 ,b.[resolution]
	 ,(select count(*) from bugnote where bug_id =  b.id) as NOTE_CNT
	 ,(select count(*) from bug_history where bug_id =  b.id) as History_CNT
	 ,(select count(*) from attachment where bug_id =  b.id) as Attach_CNT
	 ,b.creation_ts
from bug b 
where creation_ts > '2010-06-01' and creation_ts <= '2010-07-01'
and [status] ='NEW'
order by NOTE_CNT desc



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
				where bug_id = b.id and mimetype like 'image/%')
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
select * from [user] where id in (select distinct assignee from bug)



--------------------------------------------------------------
--특정 기간에 활동한 개발자 정보.
--------------------------------------------------------------
select * 
from [user] 
where id in (select distinct assignee 
			 from bug
			 where creation_ts > '2010-06-01' and creation_ts <= '2010-07-01'
			 )


--------------------------------------------------------------
-- 2010년 6월 한달간 개발자 별로 할당된 리포트 수   (실제 개발자가 맞는지는 모름)
--------------------------------------------------------------
--한달에 30건 이상 할당받은 개발자
select 
	 u.id		as [ID]
	,u.username	as [Username]
from [user] u 
join bug b on u.id = b.assignee
where creation_ts > '2010-06-01' and creation_ts <= '2010-07-01'
group by u.id, u.realname, u.username
having count(*) > 30
order by u.id


--한달동안 각 날짜별로 개발자가 할당받은 버그리포트 수 
--(프로시저 작동 후, select 문 실행. 프로시저에서 month_developer테이블 생성함)
select * from month_developer
EXEC usp_CreateUserAssignRate '2010-06-01', '2010-06-30'

create procedure usp_CreateUserAssignRate
(
	@sdate	date
	,@eDate	date
)
as
BEGIN 
	--테이블 생성 쿼리 생성
	Declare @resCnt as integer
	Declare @sql as nvarchar(max) = N'create table month_developer(ID integer, Username Nvarchar(255), Realname Nvarchar(255)'
	Declare @params as nvarchar(256)= N'@resCnt as int OUTPUT' -- OUTPUT 키워드에 주의

	Declare @date as date = @sdate
	WHILE(@date <= @eDate)
	BEGIN
		SET @sql = @sql + N', [' + cast(@date as nvarchar) + N'] integer'
		SET @date = DATEADD(dd, 1, @date)	--1일 증가
	END
	SET @sql = @sql + N')'

	--테이블 생성
	if object_id('month_developer') is not null
		drop table month_developer
	EXEC sp_executesql @sql, @params, @resCnt OUTPUT


	--기본 개발자정보 삽입
	insert into month_developer(ID, Username, Realname)
	SELECT DISTINCT u.id as [ID], u.username as [Username], u.realname as [Realname] 
	FROM [user] u join bug b on u.id = b.assignee 
	WHERE creation_ts > @sdate and creation_ts <= DATEADD(dd, 1, @edate)

	--일별 할당량 저장.
	SET @date = @sdate
	WHILE(@date <= @eDate)
	BEGIN
		SET @sql =        N'UPDATE m SET ['+cast(@date as nvarchar)+N'] = c.cnt '
		SET @sql = @sql + N'from month_developer m '
		SET @sql = @sql + N'join ('
		SET @sql = @sql + N'   select b.assignee as [ID], count(*) as cnt from bug b join [user] u on u.id = b.assignee '
		SET @sql = @sql + N'   where creation_ts > '''+cast(@date as nvarchar) +N''' and creation_ts <= '''+cast(DATEADD(dd,1,@date) as nvarchar) + N''' '
		SET @sql = @sql + N'   group by b.assignee, u.username '
		SET @sql = @sql + N') c on m.Id = c.ID'

		print @sql

		EXEC sp_executesql @sql, @params, @resCnt OUTPUT
		
		SET @date = DATEADD(dd, 1, @date)	--1일 증가
	END
END


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
from bug b --join [user] u on u.id = b.assignee
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and resolution =N'DUPLICATE'
--764개

select * from bug_relationship 
where relationship_type = 0
and src_bug_id in (select id from bug where creation_ts > '2010-06-01' and creation_ts < '2010-07-01')
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
and [priority] in (N'P1',N'P2',N'P3') 
and votes > 0

select id, status, resolution, severity, votes
from bug 
where creation_ts > '2010-06-01' and creation_ts < '2010-07-01'
and [priority] in (N'P1',N'P2',N'P3') 
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



