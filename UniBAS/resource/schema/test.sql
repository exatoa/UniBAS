use Mantis_Scribus
use Mantis_Mantis
use Mantis_phplist
use bugzilla_mozilla

select min(creation_ts) from bugs
select max(last_updated) from bug
select count(*) from products
select count(*) from bugs where creation_ts >= '2000-01-01' and creation_ts < '2003-12-31'
select distinct product_id from bugs where creation_ts >= '2000-01-01' and creation_ts < '2003-12-31'

select min(date_submitted) from bug
select max(last_updated) from bug
select count(*) from project
select count(*) from bug

select count(*) from bugs where everconfirmed = 0

--활성화된 버그리포트에 대한 추적
select D.id, dep_CNT, att_CNT from
(
select b.id, count(d.dependson) dep_CNT
from bugs b
join dependencies d on b.id = d.dependson
group by b.id
having count(d.dependson) > 10
) D
join
(select b.id, count(a.attach_id) att_CNT
from bugs b
join attachments a on b.id = a.bug_id
group by b.id
having count(a.attach_id) > 5) A on D.id = A.ID

select * from bugs where id = 167236
--
select
	 attach_id
	,p.login_name
	,p.realname
	,[description]
	,[filename]
	,isobsolete
	,ispatch
	,a.creation_ts
	,a.modification_time
from attachments  a
join [profiles] p on a.submitter_id = p.id
where bug_id = 167236

select * from flags
WHERE bug_id = 167236


--사기 데이터
-- 버그 167236에 대한 목록
INSERT INTO flags VALUES (25995, 98387, 167236, 4, null, 'john', '+', '','')
INSERT INTO flags VALUES (29024, 104715, 167236, 4, null, 'john', '+', '','')
INSERT INTO flags VALUES (29312, 105431, 167236, 4, null, 'john', '+', '','')
INSERT INTO flags VALUES (29313, 105431, 167236, 5, null, 'roc', '+', '','')
INSERT INTO flagtypes VALUES ('1', 1, '','b'),('1', 2, '','b'),('3', 3, '','b'),('review', 4, '','a'),('superreview', 5, '','a')

select * from [profiles]

select * from list_manager 

select * from bug where fixed_in_version <>'' and target_version <>''

select count(*) from bug where fixed_in_version <>''
select count(*) from bug where target_version <>''

select top 10 * from bug where target_version <>''

select * from bug_history where field_name like '%sticky%'

select * from bugnote



--------------------------------------------------
-- status_whiteboard
-- text는 where절에서 비교가 안되어서 varchar로 변경해서 하기위해 임시테이블을 생성하고 테스트.
--------------------------------------------------
CREATE TABLE test_bug
(
 id integer
,bug_status varchar(64)
,resolution varchar(64)
,version varchar(64)
,target_milestone varchar(64)
,short_desc varchar(512)
,status_whiteboard varchar(max)
)
--drop table test_bug
insert into test_bug
select id, bug_status, resolution, version, target_milestone, short_desc, status_whiteboard from bugs where status_whiteboard is not null

select * from test_bug where status_whiteboard <> ''

----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
select 
	id
	,(select descriptions from bug_text where id = b.bug_text_id)
from bug b where id = 10


select count(*) from bugs_manager where bug_id= 214110
select top 10 * from bugs where id = 214110
select * from bugs_activity	where bug_id = 210110

select * from bugs_manager where bug_id= 215289
select top 10 * from bugs where [priority] <> ''

SELECT bug_id FROM bugs_manager

select * from bugs_manager where progress in ('PENDING', 'FAIL') order by bug_id
select * from bug where id = 11703
select * from [platform]


-- 사용자 아이디의 출처 추적 (아이디의 키값 변경이 잘 이루어졌는지 확인 : 통과)
Declare @user_id as integer
select @user_id = id from [user] where username = 'jainbasil'
PRINT @user_id
select * from bug where reporter_id = @user_id
select * from bug where handler_id = @user_id
select * from bugnote where reporter_id = @user_id
select * from bug_history where [user_id] = @user_id
select * from bug_file where [user_id] = @user_id
-- 사용자 아이디의 출처 추적 요기까지.




--태그정보 확인
select id, project_id, category_id, summary, tags from bug where tags <> ''
select * from bug_tag where bug_id = 1321
select id, tags from bug where id= 1321
select * from tag where id in (5,102,103)

--관계정보 확인
select * from bug_relationship where relationship_type <> 5

select * from bug where id = 5662
select * from [user] where id = 4
select * from project where id = 1
select * from category where id = 6
select * from [priority] where id = 20
select * from severity where id = 10
select * from [status] where id = 90
select * from resolution where id = 90
select * from reproducibility where id = 100
select * from project_version



select * from tag where name in ('export', 'line-art', 'pdf')


use Mantis_Mantis
select * from bugs_manager

--------------------------------------------------
-- unified BTS 조사
--------------------------------------------------
use Bugzilla_MozillaB


select * from bugs




--------------------------------------------------
-- 태호 데이터 추출
--------------------------------------------------
use Bugzilla_MozillaA

--Firefox, General   Fixed   >> comment 10개 이상
Declare @product_id as integer
Declare @component_id as integer
SELECT @product_id = id FROM products where name=N'firefox'
SELECT @component_id = id FROM components where name=N'General' and product_id = @product_id

SELECT b.id from bugs b
join longdescs c on b.id = c.bug_id
where product_id =  @product_id
and component_id = @component_id
and resolution in ('fixed')
--and bug_status in ('resolved','verified')
group by b.id
having count(c.comment_id) > 10



--보기는 좋으나 속도는 느림 (조인 비용이 큼)
select distinct b.id 
from bugs b
join longdescs c on b.id = c.bug_id
join products p on b.product_id = p.id and p.name = 'firefox'
join components co on b.component_id = co.id and co.name = 'General'
where resolution in ('fixed')
group by b.id
having count(c.comment_id) > 10




------------------------------------------------------------------------------------------------------------------------------------------
-- 시작 순간 복구를 위한 작업.
------------------------------------------------------------------------------------------------------------------------------------------



----------------------------------------------
-- 가장 마지막에 작업하던 날을 구함
----------------------------------------------
CREATE PROCEDURE getLastDate 
AS
BEGIN
	RETURN SELECT max([date]) FROM list_manager WHERE [type]=0
END


----------------------------------------------
-- 마지막날의 대기중인 버그리포트들을 반환.
----------------------------------------------
CREATE PROCEDURE getLastPendingBugs 
(
	@last_date	as datetime
)
AS
BEGIN

	--가장 마지막 날의 수집되지 않은 버그리포트들.
	--대기중, type=0
	SELECT bug_id FROM bugs_manager 
	WHERE list_id = (SELECT id FROM list_manager 
					WHERE [type]=0 and [date] = @last_date)		--(select max([date]) from list_manager where [type]=0))
	and progress = 'PENDING'

	RETURN 
END


----------------------------------------------
-- 마지막날의 대기중인 버그리포트들을 반환. (확장)
----------------------------------------------
CREATE PROCEDURE getLastPendingBugsExpend
(
	@last_date	as datetime
)
AS
BEGIN

	--가장 마지막 날의 수집되지 않은 버그리포트들. (확장영역)
	SELECT bug_id FROM bugs_manager 
	WHERE list_id in (SELECT id FROM list_manager 
					WHERE [type]=1 and [date] = @last_date)
	and progress = N'PENDING'

	RETURN 
END




DECLARE @strID as varchar(MAX)

DECLARE @bug_id as integer
DECLARE ID_LIST CURSOR FOR
			SELECT bug_id FROM bugs_manager 
			WHERE list_id in (SELECT id FROM list_manager 
							WHERE [type]=1 and [date] = '2010-01-01')
			and progress = 'PENDING'

--커서 실행
OPEN ID_LIST;

--커서 한 아이템 선정
FETCH NEXT FROM ID_LIST
INTO @bug_id

-- Check @@FETCH_STATUS to see if there are any more rows to fetch.
WHILE @@FETCH_STATUS = 0
BEGIN

	SET @strID = 
   -- Concatenate and display the current values in the variables.
   PRINT 'Contact Name: ' + @FirstName + ' ' +  @LastName



	FETCH NEXT FROM ID_LIST
	INTO @bug_id
END





-----------------------------------------------------
use Bug_MozillaT
select * from bug where [description] is null




use Bugzilla_Mozilla

use bug_mozillaT

select * from longdescs where bug_id in (8358,16198,16199,16532,14616)

select * from summary_word
select * from word where id >=50

SELECT TOP 10 id, summary FROM BUG
delete from summary_word
delete from word

select * from bug where [description] is null


