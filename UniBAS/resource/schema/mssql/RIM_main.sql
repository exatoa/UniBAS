----------------------------------------------
-- 사이트  
----------------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'site')
BEGIN
	CREATE TABLE [site]
	(
		 id					int											-- key
		,name				NVARCHAR(64)								-- 사이트에 대한 명칭
		,[description]		NVARCHAR(256)	DEFAULT('')					-- Memo
		,[site_type]		NVARCHAR(64)	DEFAULT('')		NOT NULL	-- BTS 시스템의 종류(Bugzilla, Mantis, Trac 중 1)
		,[schema_name]		NVARCHAR(64)								-- RSM Name (It's Database Name)   -- There is no constraint but, It has to unique
		,base_url			NVARCHAR(256)					NOT NULL	-- 타겟의 기본 url
		,log_path			NVARCHAR(256)					NOT NULL	-- 분석에 사용된 로그파일 위치
		,cache_path			NVARCHAR(256)					NOT NULL	-- 캐쉬를 저장하고 있는 위치
		,create_ts			datetime									-- 데이터베이스 생성시간.
		,delta_ts			datetime									-- 마지막으로 변경한 시간
		,[status]			NVARCHAR(64)								-- 수집 상태 표시. 수집작업 완료일자 (완료가 안된경우 null)
		,PRIMARY KEY (id)
	);
	CREATE UNIQUE NONCLUSTERED INDEX	site_name_idx	ON [site](name);
END;;

----------------------------------------------
-- 사이트에 존재하는 타입들의 매핑정보
----------------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'enum_map')
BEGIN
	CREATE TABLE [enum_map]
	(
		 site_id			int		
		,id					integer
		,[type]				NVARCHAR(64)								-- 
		,[value]			NVARCHAR(256)	DEFAULT('')					-- 
		,[map]				NVARCHAR(256)	DEFAULT('')		NOT NULL	-- 
	)
	--CREATE UNIQUE NONCLUSTERED INDEX	site_name_idx	ON [enum](name)
END;;

----------------------------------------
-- 분석용 쿼리 저장소
----------------------------------------
IF NOT EXISTS (SELECT name FROM sys.tables WHERE name=N'analysis_query')
BEGIN
	CREATE TABLE analysis_query
	(
		 id					int				NOT NULL		-- 식별자
		,[type]				NVARCHAR(32)	NOT NULL		-- 쿼리 종류(SYSTEM PROCDDURE, SYSREM FUNCTION, USER PROCEDURE, USER FUNCTION, QUERY)
		,title				NVARCHAR(256)	NOT NULL		-- 쿼리 제목
		,[description]		NVARCHAR(max)					-- 쿼리 설명
		,[name]				NVARCHAR(256)					-- 시스템에 등록된 PROCEDURE OR FUNCTION의 명칭
		,[query]			NVARCHAR(MAX)	NOT NULL		-- 쿼리 내용
		,creation_ts		datetime						-- 데이터 시작시간
		,modification_ts	datetime						-- 데이터 종료시간
		,PRIMARY KEY(id)
	)
END;;


--###################################################################################################
--# Bug 관련 테이블들.
--###################################################################################################
----------------------------------------------
-- 사이트별 버그리포트 테이블
----------------------------------------------
CREATE TABLE bug
(
	 site_id			INTEGER			NOT NULL
	,id					INTEGER			NOT NULL					--
	,reporter_id		INTEGER			NOT NULL	DEFAULT(0)		--보고자ID
	,assignee_id		INTEGER										--할당자ID
	,qa_id				INTEGER										--검토자ID
	,project_id			INTEGER			NOT NULL	DEFAULT(0)		--제품ID
	,component_id		INTEGER 		NOT NULL	DEFAULT(0)		--컴포넌트ID
	,[status]			NVARCHAR(64)	NOT NULL	DEFAULT(N'')	--버그상태
	,resolution			NVARCHAR(64)	NOT NULL	DEFAULT(N'')	--수정상태
	,everconfirmed		TINYINT						DEFAULT(0)		--Unknown (초기에는 없을수 있으므로 NULL 허용)
	,[priority]			NVARCHAR(64)								--우선순위
	,severity			NVARCHAR(64)								--심각도
	,[platform]			NVARCHAR(64)								--플랫폼
	,os					NVARCHAR(64)								--OS
	,[version]			NVARCHAR(64)								--product_version
	,fixed_in_version	NVARCHAR(64)				DEFAULT(N'')	--수정된 버전
	,milestone			NVARCHAR(64)								--목표된 마일스톤
	,summary			NVARCHAR(255)	NOT NULL	DEFAULT(N'')	--요약 (제목)
	,[description]		NVARCHAR(MAX)				DEFAULT(N'')	--전체 문제 내용.
	--,tags				TEXT										--Tags, Keywords
	
	,creation_ts		DATETIME		NOT NULL	DEFAULT('0000-00-00 00:00:00') 	--생성시간
	,modification_ts	DATETIME		NOT NULL	DEFAULT('0000-00-00 00:00:00') 	--수정시간	
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_bug_component_id	ON bug (component_id);;
CREATE NONCLUSTERED INDEX idx_bug_product_id	ON bug (project_id);;
CREATE NONCLUSTERED INDEX idx_bug_reporter	ON bug (reporter_id);;
CREATE NONCLUSTERED INDEX idx_bug_assignee	ON bug (assignee_id);;
CREATE NONCLUSTERED INDEX idx_bug_qa	ON bug (qa_id);;
CREATE NONCLUSTERED INDEX idx_bug_creation_ts	ON bug (creation_ts);;
CREATE NONCLUSTERED INDEX idx_bug_updated_ts	ON bug (modification_ts);;
CREATE NONCLUSTERED INDEX idx_bug_status	ON bug ([status]);;
CREATE NONCLUSTERED INDEX idx_bug_resolution	ON bug (resolution);;
CREATE NONCLUSTERED INDEX idx_bug_priority	ON bug ([priority]);;
CREATE NONCLUSTERED INDEX idx_bug_severity	ON bug (severity);;
CREATE NONCLUSTERED INDEX idx_bug_os	ON bug (os);;
CREATE NONCLUSTERED INDEX idx_bug_target_milestone	ON bug (milestone);;
CREATE NONCLUSTERED INDEX idx_bug_version	ON bug ([version]);;






----------------------------------------------
-- 사이트별 버그리포트의 코멘트들
----------------------------------------------
CREATE TABLE comment
(
	 site_id			INTEGER			NOT NULL
	,id					INTEGER			NOT NULL					--comment_id
	,bug_id				INTEGER			NOT NULL 	DEFAULT(0)  	--버그ID
	,parent_comment_id	INTEGER										--부모 comment id
	,submitter_id		INTEGER			NOT NULL 	DEFAULT(0)  	--작성자 ID
	,[type]				NVARCHAR(64)	NOT NULL 	DEFAULT(0)  	--종류 (desc, setp, additional, comment)
	,creation_ts		DATETIME		NOT NULL 	DEFAULT('0000-00-00 00:00:00')	--생성시간
	,modification_ts	DATETIME		NOT NULL	DEFAULT('0000-00-00 00:00:00')	--수정시간
	,thetext			NVARCHAR(MAX)	NOT NULL	DEFAULT(N'') 	--노트내용
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_comment_bug_id				ON comment (bug_id);;
CREATE NONCLUSTERED INDEX idx_comment_updated_ts			ON comment (modification_ts);;
CREATE NONCLUSTERED INDEX idx_comment_submitter_id_bug_id	ON comment (submitter_id,bug_id);;




----------------------------------------------
-- 사이트별 버그리포트의 첨부파일(메타데이터)
----------------------------------------------
CREATE TABLE attachment
(
	 site_id			INTEGER			NOT NULL
	,id					INTEGER			NOT NULL					--
	,bug_id				INTEGER			NOT NULL	DEFAULT(0)		--
	,submitter_id		INTEGER			NOT NULL	DEFAULT(0)		--제출자
	,ispatch			TINYINT						DEFAULT(0)		--
	,[filename]			NVARCHAR(100)	NOT NULL	DEFAULT(N'')	--파일명
	,mimetype			NVARCHAR(100)	NOT NULL	DEFAULT(N'')	--마임타입
	,filesize			INTEGER			NOT NULL	DEFAULT(0)		--파일크기
	,creation_ts		DATETIME		NOT NULL	DEFAULT('0000-00-00 00:00:00') 	--생성시간
	,modification_ts	DATETIME		NOT NULL	DEFAULT('0000-00-00 00:00:00') 	--수정시간
	--,isobsolete			TINYINT						DEFAULT(0)	--쓸모없는가?
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_attachment_updated_ts	ON attachment (modification_ts);;
CREATE NONCLUSTERED INDEX idx_attachment_submitter_id_bug_id	ON attachment (submitter_id,bug_id);;
CREATE NONCLUSTERED INDEX idx_attachment_bug_id	ON attachment (bug_id);;
CREATE NONCLUSTERED INDEX idx_attachment_creation_ts	ON attachment (creation_ts);;


----------------------------------------------
-- 사이트별 버그리포트의 첨부파일 (실제데이터)
----------------------------------------------
CREATE TABLE attach_data
(
	 site_id			INTEGER			NOT NULL
	,attach_id			INTEGER    		NOT NULL 	 	--
	,thedata			VARBINARY(max)	NOT NULL 		--
	,PRIMARY KEY (site_id, attach_id)
);;



----------------------------------------------
-- 사이트별 버그리포트의 변경사항
----------------------------------------------
CREATE TABLE history
(
	 site_id		INTEGER			NOT NULL
	,bug_id			INTEGER			NOT NULL 	DEFAULT(0)		--
	,[who_id]		INTEGER			NOT NULL 	DEFAULT(0)		--
	,[when]			DATETIME		NOT NULL 	DEFAULT('0000-00-00 00:00:00') 	--
	,what			NVARCHAR(256)	NOT NULL 	DEFAULT(0)		--
	,old_value		NVARCHAR(MAX)     			--
	,new_value		NVARCHAR(MAX)     			--
--	,attach_id		INTEGER						--
);;
CREATE NONCLUSTERED INDEX idx_history_bug_id	ON history (bug_id);;
CREATE NONCLUSTERED INDEX idx_history_changed_ts	ON history ([when]);;
CREATE NONCLUSTERED INDEX idx_history_user_id	ON history ([who_id]);;
CREATE NONCLUSTERED INDEX idx_history_field	ON history (what);;


----------------------------------------------
-- 사이트별 버그리포트의 관계
----------------------------------------------
CREATE TABLE relationship
(
	 site_id			INTEGER		NOT NULL
	,src_bug_id			INTEGER		NOT NULL	--버그리포트 ID
	,dest_bug_id		INTEGER		NOT NULL	--버그리포트 ID
	,relationship_type	SMALLINT	NOT NULL	--duplicate of (src:dupe - desc:dupe_of), has duplicate (src:dupe_of - desc:dupe), child of (src:blocked - dest:depends on), parent of (src:depends on - dest:blocked), related to (src:src - desc:dest)
);;
CREATE UNIQUE NONCLUSTERED INDEX idx_relationship_all			ON relationship (site_id, src_bug_id, dest_bug_id, relationship_type);;
CREATE NONCLUSTERED INDEX		 idx_relationship_src_bug_id	ON relationship (src_bug_id);;
CREATE NONCLUSTERED INDEX		 idx_relationship_dest_bug_id	ON relationship (dest_bug_id);;
CREATE NONCLUSTERED INDEX		 idx_relationship_relationship_type	ON relationship (relationship_type);;



----------------------------------------------
-- 사이트별 버그리포트의 참고사항 see_also의 통합버전
----------------------------------------------
CREATE TABLE [reference]
(
	 site_id	INTEGER			NOT NULL
	,bug_id		INTEGER			NOT NULL	--버그리포트 ID
	,value		NVARCHAR(max)	NOT NULL	--값
);;





----------------------------------------------
-- 사이트별 사용자의 관심 버그리포트
----------------------------------------------
CREATE TABLE monitor
(
	 site_id	INTEGER		NOT NULL
	,bug_id 	INTEGER		NOT NULL  	DEFAULT(0) 	--
	,[user_id]	INTEGER		NOT NULL 	DEFAULT(0) 	--
);;
CREATE UNIQUE NONCLUSTERED INDEX idx_monitor_bug_id_user_id	ON monitor (site_id, bug_id, [user_id]);;
CREATE NONCLUSTERED INDEX idx_monitor_user_id	ON monitor ([user_id]);;




----------------------------------------------
-- 사이트별 존재하는 사용자
----------------------------------------------
CREATE TABLE [user]
(
	 site_id		INTEGER			NOT NULL
	,id				INTEGER										--버그리포트는 아이디 자동생성
	,username		NVARCHAR(255)   NOT NULL	DEFAULT(N'')   	--로그인 아이디
	,realname		NVARCHAR(255)   NOT NULL	DEFAULT(N'')   	--실제 아이디
	,[role]			NVARCHAR(32)				DEFAULT(N'')		--사용자의 역할
	,creation_ts	DATETIME					DEFAULT('0000-00-00 00:00:00') 	--생성시간
	,lastchange_ts	DATETIME					DEFAULT('0000-00-00 00:00:00') 	--수정시간
	,PRIMARY KEY (site_id, id)
);;
CREATE UNIQUE NONCLUSTERED INDEX idx_user_username	ON [user] (site_id, username);;



----------------------------------------------
-- 사이트별 존재하는 프로젝트분류 (삭제됨)
----------------------------------------------
-- CREATE TABLE classification
-- (
-- 	 id				SMALLINT									--
-- 	--,sortkey		SMALLINT		NOT NULL	DEFAULT(0)    	--
-- 	,name			NVARCHAR(64)  	NOT NULL	DEFAULT(N'')   	--
-- 	,[description]	TEXT         								--
-- 	,PRIMARY KEY (id)
-- );;
-- GO
-- CREATE UNIQUE NONCLUSTERED INDEX idx_classification_name	ON classification (name);;
-- GO



----------------------------------------------
-- 사이트별 존재하는 프로젝트
----------------------------------------------
CREATE TABLE project
(
	 site_id			INTEGER			NOT NULL
	,id					INTEGER			--
	,parent_project_id	INTEGER
	,name				NVARCHAR(64)	NOT NULL	DEFAULT(N'')    	--
	,[description]		NVARCHAR(MAX)									--설명
	,PRIMARY KEY (site_id, id)
);;
CREATE UNIQUE NONCLUSTERED INDEX idx_project_name	ON project (site_id, name);;




----------------------------------------------
-- 사이트별 존재하는 컴포넌트
----------------------------------------------
CREATE TABLE component
(
	 site_id		INTEGER			NOT NULL					-- site_id
	,id				INTEGER			NOT NULL 					--
	,project_id		INTEGER    					DEFAULT(0) 		-- 프로젝트 id
	,name			NVARCHAR(64)	NOT NULL	DEFAULT(N'') 	--
	,[description]	NVARCHAR(MAX)	NOT NULL	DEFAULT(N'') 	--
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_component_name	ON component (name);;
CREATE UNIQUE NONCLUSTERED INDEX idx_component_product_id_name	ON component (site_id, project_id, name);;


----------------------------------------------
-- 사이트별 나타나는 키워드
----------------------------------------------
CREATE TABLE keyword
(
	 site_id		INTEGER			NOT NULL
	,id				SMALLINT			--
	,name			NVARCHAR(64)	NOT NULL	DEFAULT(N'')	--
	,[description]	TEXT          			--
	,PRIMARY KEY (site_id, id)
);;
CREATE UNIQUE NONCLUSTERED INDEX idx_keyword_name	ON keyword (site_id, name);;



----------------------------------------------
-- 버그리포트와 키워드의 관계 테이블. 
----------------------------------------------
CREATE TABLE bug_keyword
(
	 site_id	INTEGER			NOT NULL
	,bug_id		INTEGER			NOT NULL	DEFAULT(0)	--
	,keyword_id	INTEGER			NOT NULL	DEFAULT(0)	--
);;
CREATE UNIQUE NONCLUSTERED INDEX idx_bug_keyword_bug_id_tag_id	ON bug_keyword (site_id, bug_id,keyword_id);;
CREATE NONCLUSTERED INDEX		 idx_bug_keyword_tag_id	ON bug_keyword (keyword_id);;



----------------------------------------------
-- 코멘트 descriptrion등으로 부터 추출한 형식적인 텍스트들을 담음. (ideneity 옵션임)
----------------------------------------------
CREATE TABLE additional_info
(
	 id				INTEGER			NOT NULL	<{0}>
	,site_id 		INTEGER 		NOT NULL
	,bug_id			INTEGER			NOT NULL 	DEFAULT(0)   	--	
	,[type]		 	NVARCHAR(20)	NOT NULL 	DEFAULT(0)   	--
	,thedata		NVARCHAR(max)	NOT NULL	DEFAULT(N'')  	--
	,PRIMARY KEY (id)
);;


----------------------------------------------
-- status 속성테이블
----------------------------------------------
CREATE TABLE [status]
(
	 site_id 		INTEGER 			NOT NULL
	,id				SMALLINT									--ID, 버그질라는 자동생성
	,sortkey		SMALLINT			NOT NULL	DEFAULT(0)   	--
	,value			NVARCHAR(128)   	NOT NULL	DEFAULT(N'')  	--
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_status_sortkey_value	ON [status] (sortkey,value);;
CREATE UNIQUE NONCLUSTERED INDEX idx_status_value	ON [status] (site_id, value);;


----------------------------------------------
-- resolution 속성테이블
----------------------------------------------
CREATE TABLE resolution
(
	 site_id 		INTEGER 			NOT NULL
	,id				SMALLINT									--ID, 버그질라는 자동생성
	,sortkey		SMALLINT			NOT NULL	DEFAULT(0)   	--
	,value			NVARCHAR(128)   	NOT NULL	DEFAULT(N'')  	--
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_resolution_sortkey_value	ON resolution (sortkey,value);;
CREATE UNIQUE NONCLUSTERED INDEX idx_resolution_value	ON resolution (site_id, value);;


----------------------------------------------
-- severity 속성테이블
----------------------------------------------
CREATE TABLE severity
(
	 site_id 		INTEGER 			NOT NULL
	,id				SMALLINT									--ID, 버그질라는 자동생성
	,sortkey		SMALLINT			NOT NULL	DEFAULT(0)   	--
	,value			NVARCHAR(128)   	NOT NULL	DEFAULT(N'')  	--
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_severity_sortkey_value	ON severity (sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX idx_severity_value	ON severity (site_id, value);;


----------------------------------------------
-- [priority] 속성테이블
----------------------------------------------
CREATE TABLE [priority]
(
	 site_id 		INTEGER 			NOT NULL
	,id				SMALLINT									--ID, 버그질라는 자동생성
	,sortkey		SMALLINT			NOT NULL	DEFAULT(0)   	--
	,value			NVARCHAR(128)   	NOT NULL	DEFAULT(N'')  	--
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_priority_sortkey_value	ON [priority] (sortkey,value);;
CREATE UNIQUE NONCLUSTERED INDEX idx_priority_value	ON [priority] (site_id, value);;




----------------------------------------------
-- platform 속성테이블
----------------------------------------------
CREATE TABLE [platform]
(
	 site_id 		INTEGER 			NOT NULL
	,id				SMALLINT									--ID, 버그질라는 자동생성
	,sortkey		SMALLINT			NOT NULL	DEFAULT(0)   	--
	,value			NVARCHAR(128)   	NOT NULL	DEFAULT(N'')  	--
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_platform_sortkey_value	ON [platform] (sortkey,value);;
CREATE UNIQUE NONCLUSTERED INDEX idx_platform_value	ON [platform] (site_id, value);;


----------------------------------------------
-- os 속성테이블
----------------------------------------------
CREATE TABLE os
(
	 site_id 		INTEGER 			NOT NULL
	,id				SMALLINT									--ID, 버그질라는 자동생성
	,sortkey		SMALLINT			NOT NULL	DEFAULT(0)   	--
	,value			NVARCHAR(128)   	NOT NULL	DEFAULT(N'')  	--
	,PRIMARY KEY (site_id, id)
);;
CREATE NONCLUSTERED INDEX idx_os_sortkey_value	ON os (sortkey,value);;
CREATE UNIQUE NONCLUSTERED INDEX idx_os_value	ON os (site_id, value);;




----------------------------------------------
-- [version] 속성테이블
----------------------------------------------
CREATE TABLE [version]
(
	 site_id 		INTEGER 			NOT NULL
	,project_id 	INTEGER 			NOT NULL
	,id				SMALLINT									--ID, 버그질라는 자동생성
	,value			NVARCHAR(128)   	NOT NULL	DEFAULT(N'')  	--
	,PRIMARY KEY (site_id, project_id, id)
);;
CREATE UNIQUE NONCLUSTERED INDEX idx_version_product_id_value	ON [version] (site_id, project_id, value);;


----------------------------------------------
-- milestone 속성테이블
----------------------------------------------
CREATE TABLE milestone
(
	 site_id 		INTEGER 			NOT NULL
	,project_id 	INTEGER 			NOT NULL
	,id				SMALLINT									--ID, 버그질라는 자동생성
	,sortkey		SMALLINT			NOT NULL	DEFAULT(0)   	--
	,value			NVARCHAR(128)   	NOT NULL	DEFAULT(N'')  	--
	,PRIMARY KEY (site_id, project_id, id)
);;
CREATE UNIQUE NONCLUSTERED INDEX idx_milestone_product_id_value	ON milestone (site_id, project_id, value);;



----------------------------------------------
-- 버그리포트의 매핑정보를 업데이트하는 프로시저
----------------------------------------------
CREATE PROCEDURE initializeFieldType
(
	@site_id as integer
)
AS 
BEGIN
	
	UPDATE b SET severity = e.map FROM bug b
	JOIN (SELECT site_id, value, map from enum_map where site_id = @site_id and value <> map and [type] = 'severity') e	on e.site_id = b.site_id and b.severity = e.value WHERE b.site_id = @site_id


	--priority에 관한 매핑 정보 변경
	UPDATE b SET [priority] = e.map FROM bug b
	JOIN (SELECT site_id, value, map FROM enum_map WHERE site_id = @site_id and value <> map and [type] = 'priority'  ) e	on e.site_id = b.site_id and b.[priority] = e.value WHERE b.site_id = @site_id

	--status에 관한 매핑 정보 변경
	UPDATE b SET [status] = e.map FROM bug b
	JOIN (SELECT site_id, value, map FROM enum_map WHERE site_id = @site_id and value <> map and [type] = 'status'  ) e	on e.site_id = b.site_id and b.[status] = e.value WHERE b.site_id = @site_id

	--status에 관한 매핑 정보 변경
	UPDATE b SET resolution = e.map FROM bug b
	JOIN (SELECT site_id, value, map FROM enum_map WHERE site_id = @site_id and value <> map and [type] = 'resolution'  ) e	on e.site_id = b.site_id and b.resolution = e.value WHERE b.site_id = @site_id

END;;




-------------------------------------------------------------------------
-- 버그리포트의 매핑정보를 업데이트하는 프로시저
--    과거의 값을 이용하여 매핑 (UniBas)
-------------------------------------------------------------------------
CREATE PROCEDURE updateFieldType
(
	@site_id as integer
)
AS 
BEGIN

	UPDATE b SET severity = e.map FROM bug b
	JOIN unibas..bug ub on b.site_id = ub.site_id and b.id = ub.id
	JOIN (SELECT site_id, value, map FROM enum_map WHERE site_id = @site_id and [type] = 'severity'  ) e on e.site_id = b.site_id and ub.severity = e.value
	WHERE b.site_id = @site_id

	UPDATE b SET [priority] = e.map FROM bug b
	JOIN unibas..bug ub on b.site_id = ub.site_id and b.id = ub.id
	JOIN (SELECT site_id, value, map FROM enum_map WHERE site_id = @site_id and [type] = 'priority'  ) e on e.site_id = b.site_id and ub.[priority] = e.value
	WHERE b.site_id = @site_id


	UPDATE b SET [status] = e.map FROM bug b
	JOIN unibas..bug ub on b.site_id = ub.site_id and b.id = ub.id
	JOIN (SELECT site_id, value, map FROM enum_map WHERE site_id = @site_id and [type] = 'status'  ) e on e.site_id = b.site_id and ub.[status] = e.value
	WHERE b.site_id = @site_id

	UPDATE b SET resolution = e.map FROM bug b
	JOIN unibas..bug ub on b.site_id = ub.site_id and b.id = ub.id
	JOIN (SELECT site_id, value, map FROM enum_map WHERE site_id = @site_id and [type] = 'resolution'  ) e on e.site_id = b.site_id and ub.resolution = e.value
	WHERE b.site_id = @site_id

END;;


-------------------------------------------------------------------------
-- 버그리포트의 매핑정보테이블을 업데이트 시킴
-------------------------------------------------------------------------
CREATE PROCEDURE initializeFieldMap
(
	 @site_id	as integer
	,@xml		as XML
)
AS 
BEGIN
	--기본 정보 채움.
	DELETE from enum_map
	insert into enum_map	select site_id, id, 'severity', value, value from severity
	insert into enum_map	select site_id, id, 'priority', value, value from [priority]
	insert into enum_map	select site_id, id, 'resolution', value, value from resolution
	insert into enum_map	select site_id, id, 'status', value, value from [status]

	BEGIN TRY
	
		DECLARE @element AS TABLE
		(
			 [type]		NVARCHAR(64)
			,old		NVARCHAR(128)
			,new		NVARCHAR(128)
		)

		--XML로 부터 테이블 생성.
		INSERT @element
		SELECT 
			 b.value(N'@type',		N'NVARCHAR(64)')	as [type]
			,b.value(N'@old',		N'NVARCHAR(128)')	as old
			,b.value(N'@new',		N'NVARCHAR(128)')	as new
		FROM @xml.nodes(N'/Map/E')			as a(b) 

		--업데이트
		UPDATE e
		SET		map = new
		FROM	enum_map e
		join	@element n	on e.[type] = n.[type] and e.value = n.old
	
	END TRY
	BEGIN CATCH
		DECLARE @DataError NVARCHAR(4000)
		SET @DataError = N'Error' + CONVERT(NVARCHAR(10), ERROR_NUMBER()) + N':' + ERROR_MESSAGE()
		PRINT @DataError
		RETURN -1	--RAISERROR (@dataError, 16,1)
	END CATCH
END;;


---------------------------------------------------------------------------
--Exmaple 예시 XML
---------------------------------------------------------------------------
--<Map>
--<E type="severity"		old="trivial"		new="minor"			/>
--<E type="severity"		old="major"			new="normal"		/>
--<E type="priority"		old="P2"			new="P1"			/>
--<E type="priority"		old="P4"			new="P5"			/>
--<E type="resolution"	old="EXPIRED"		new="FIXED"			/>
--<E type="resolution"	old="MOVED"			new="FIXED"			/>
--<E type="resolution"	old="INVALID"		new="INCOMPLETE"	/>
--<E type="resolution"	old="WONTFIX"		new="FIXED"			/>
--<E type="resolution"	old="WORKSFORME"	new="INCOMPLETE"	/>
--<E type="status"		old="ASSIGNED"		new="OPEN"			/>
--<E type="status"		old="CLOSED"		new="CLOSED"		/>
--<E type="status"		old="NEW"			new="OPEN"			/>
--<E type="status"		old="REOPENED"		new="OPEN"			/>
--<E type="status"		old="RESOLVED"		new="CLOSED"		/>
--<E type="status"		old="UNCONFIRMED"	new="OPEN"			/>
--<E type="status"		old="VERIFIED"		new="CLOSED"		/>
--</Map>

