--use master
--create database bugzilla_Test
--use bugzilla_Test
--모든 테이블 기본옵션
--ENGINE=MyISAM DEFAULT(CHARSET=utf8', 'pgsql' => 'WITHOUT OIDS')));;
--create database mantis
--use mantis

--###################################################################################################
--# Bug Manager 테이블
--###################################################################################################
CREATE TABLE bugs_manager
(
	 bug_id			int				NOT NULL
	,progress		NVARCHAR(20)		NOT NULL		--상태유형 : PENDING
													--			 BUG_REQUEST, BUG_DOWNLOADED, BUG_PARSED, BUG_SAVED
													--			 HIST_REQUEST, HIST_DOWNLOADED, HIST_PARSED, HIST_SAVED
													--			 VOTE_REQUEST, VOTE_DOWNLOADED, VOTE_PARSED, VOTE_SAVED
													--			 DONE
	,bug_url		NVARCHAR(512)		
	,bug_path		NVARCHAR(512)
	,creation_ts	datetime
	,udpate_ts		datetime
	,PRIMARY KEY (bug_id)
);;


----------------------------------------------
--새로운 버그아이디를 등록한다.
----------------------------------------------
CREATE PROCEDURE addUnit
(
	 @bug_id		as integer			--bugID
)
AS
BEGIN
	--버그리포트 정보 추가( 중복에러 무시)
	BEGIN TRY
		INSERT INTO bugs_manager VALUES (@bug_id, N'PENDING',null, null, SYSDATETIME(), SYSDATETIME())			--bug_manager에 삽입
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN @bug_id 
		ELSE RETURN -1
	END CATCH
	

	return @bug_id	
END;;



----------------------------------------------
--버그정보를 업데이트 한다.
----------------------------------------------
CREATE PROCEDURE updateUnit
(
	 @bug_id		as integer
	,@progress		as NVARCHAR(30)
	,@url			as NVARCHAR(512)	 = null
	,@path			as NVARCHAR(512)	 = null
)
AS
BEGIN
	IF NOT EXISTS (SELECT bug_id from bugs_manager where bug_id = @bug_id)
		RETURN -1

	UPDATE B 
	SET	 progress = @progress
		,bug_url = CASE WHEN @url is null THEN bug_url ELSE @url END
		,bug_path = CASE WHEN @path is null THEN bug_path ELSE @path END
		,udpate_ts = SYSDATETIME()
	FROM bugs_manager B
	where bug_id = @bug_id

	RETURN @bug_id
END;;



----------------------------------------------------
-- 버그리포트 메인 테이블
----------------------------------------------------
CREATE TABLE bug
(
	 id 				INTEGER			NOT NULL						--UNSIGNED
	,project_id 		INTEGER	  		NOT NULL	DEFAULT(0)			--UNSIGNED
	,category_id		INTEGER		 	NOT NULL	DEFAULT(1)
	
	,reporter_id 		INTEGER			NOT NULL	DEFAULT(0)			--UNSIGNED
	,handler_id 		INTEGER	  		NOT NULL	DEFAULT(0)			--UNSIGNED
	,duplicate_id 		INTEGER	  		NOT NULL	DEFAULT(0)			--UNSIGNED
	
	,[priority]			INTEGER 		NOT NULL	--DEFAULT(30)
	,severity 			INTEGER 		NOT NULL	--DEFAULT(50)
	,[status]			INTEGER 		NOT NULL	--DEFAULT(10)
	,resolution 		INTEGER 		NOT NULL	--DEFAULT(10)
	,reproducibility 	INTEGER 		NOT NULL	--DEFAULT(10)	

	,os 				NVARCHAR(128) 	NOT NULL	DEFAULT(N'')
	,os_build 			NVARCHAR(128) 	NOT NULL	DEFAULT(N'')			--OS_Version
	,[platform] 		NVARCHAR(128) 	NOT NULL	DEFAULT(N'')
	,build 				NVARCHAR(64) 	NOT NULL	DEFAULT(N'')			--Product Build : 혼동이 많은 이슈 http://mantisbt.domainunion.de/forums/viewtopic.php?f=2&t=4348
	,[version] 			NVARCHAR(64) 	NOT NULL	DEFAULT(N'')
	,fixed_in_version 	NVARCHAR(64) 	NOT NULL	DEFAULT(N'')
	,target_version		NVARCHAR(64) 	NOT NULL	DEFAULT(N'')

	,summary 			NVARCHAR(512) 	NOT NULL	DEFAULT(N'')
	,date_submitted 	DATETIME 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	,last_updated 		DATETIME 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	,due_date       	DATETIME		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER

	,view_state 		INTEGER 		NOT NULL	DEFAULT(10)			-- public, private
	,projection 		INTEGER 		NOT NULL	DEFAULT(10)			-- 수정 예상 비용 (redesign, major rework, minor fix, tweak ....ref : http://open.affelio.jp/modules/mantis/doc/documentation.html)
	--,bug_text_id 		INTEGER	  		NOT NULL	DEFAULT(0)			--UNSIGNED
	,profile_id 		INTEGER	  		NOT NULL	DEFAULT(0)			--UNSIGNED	os+os_Build+platform
	,eta 				INTEGER 					DEFAULT(10)			-- Estimated/Expected Time of Arrival (ref : http://www.mantisbt.org/forums/viewtopic.php?f=2&t=10274, http://www.mantisbt.org/bugs/view.php?id=14527)
	,sponsorship_total 	INTEGER	 					DEFAULT(0)
	,sticky				BIT	  						DEFAULT(0)			-- is this bug a difficult problem?
	,tags				NVARCHAR(MAX)
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX idx_bug_sponsorship_total		ON bug(sponsorship_total);;
CREATE NONCLUSTERED INDEX idx_bug_fixed_in_version		ON bug(fixed_in_version);;
CREATE NONCLUSTERED INDEX idx_bug_status				ON bug([status]);;
CREATE NONCLUSTERED INDEX idx_project					ON bug(project_id);;



----------------------------------------------------
-- 버그리포트 중요 텍스트 테이블 (Description)
----------------------------------------------------
CREATE TABLE bug_text
(
	 bug_id 				Integer		NOT NULL
	,description 			TEXT	 	NOT NULL
	,steps_to_reproduce 	TEXT	 	NOT NULL
	,additional_information	TEXT	 	NOT NULL
	,PRIMARY KEY (bug_id)
);;


----------------------------------------------------
-- 버그리포트 Comment 정보 (Notes)
----------------------------------------------------
CREATE TABLE bugnote
(
	 id 				Integer			NOT NULL	IDENTITY(1,1)		--UNSIGNED
	,bug_id 			Integer	 		NOT NULL	DEFAULT(0)			--UNSIGNED
	,reporter_id 		Integer	 		NOT NULL	DEFAULT(0)			--UNSIGNED
--	,bugnote_text_id 	Integer	 		NOT NULL	DEFAULT(0)			--UNSIGNED
	,view_state 		SMALLINT 		NOT NULL	DEFAULT(10)
	,date_submitted 	datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER	DEFAULT('0000-00-00 00:00:00.000')
	,last_modified 		datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER	DEFAULT('0000-00-00 00:00:00.000')
	,note_type 			Integer						DEFAULT(0)
	,note_attr 			NVARCHAR(250)				DEFAULT(N'')
	,time_tracking		INTEGER			NOT NULL	DEFAULT(0)			--UNSIGNED
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX idx_bug		ON bugnote(bug_id);;
CREATE NONCLUSTERED INDEX idx_last_mod		ON bugnote(last_modified);;


----------------------------------------------------
-- 버그리포트 Comment의 실제 텍스트 정보 (Notes)
----------------------------------------------------
CREATE TABLE bugnote_text
(
	 bugnote_id			Integer	 NOT NULL
	,[text]				TEXT	 NOT NULL
	,PRIMARY KEY (bugnote_id)
);;




----------------------------------------------------
-- 버그리포트 첨부파일 정보 (Attached_Files)
----------------------------------------------------
CREATE TABLE bug_file
(
	 id					Integer			NOT NULL						--UNSIGNED
	,bug_id 			Integer			NOT NULL	DEFAULT(0)			--UNSIGNED
	,[user_id]			INTEGER 		NOT NULL	DEFAULT(0)
	,title 				NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,[description] 		NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,diskfile 			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,[filename]			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,folder 			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,filesize 			Integer			NOT NULL	DEFAULT(0)
	,file_type 			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,date_added 		datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	,content 			varbinary(max)	
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX idx_bug_file_bug_id		ON bug_file(bug_id);;
CREATE NONCLUSTERED INDEX idx_diskfile		ON bug_file(diskfile);;




----------------------------------------------------
-- 버그리포트 변경정보 (Issue History)
----------------------------------------------------
CREATE TABLE bug_history
(
	 id 				Integer			NOT NULL	IDENTITY(1,1)		--UNSIGNED
	,[user_id] 			Integer			NOT NULL	DEFAULT(0)			--UNSIGNED
	,bug_id 			Integer	  		NOT NULL	DEFAULT(0)			--UNSIGNED
	,date_modified 		datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	,field_name 		NVARCHAR(64) 	NOT NULL	DEFAULT(N'')
	,old_value 			NVARCHAR(255) 	NOT NULL	DEFAULT(N'')
	,new_value 			NVARCHAR(255) 	NOT NULL	DEFAULT(N'')
	,[type] 			SMALLINT		NOT NULL	DEFAULT(0)
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX idx_bug_history_bug_id	ON bug_history(bug_id);;
CREATE NONCLUSTERED INDEX idx_history_user_id		ON bug_history([user_id]);;



----------------------------------------------------
-- 버그리포트와 태그의 관계 테이블
----------------------------------------------------
CREATE TABLE bug_tag
(
	 bug_id				Integer	 		NOT NULL	DEFAULT(0)			--UNSIGNED
	,tag_id				Integer			NOT NULL	DEFAULT(0)			--UNSIGNED
	,[user_id]			Integer			NOT NULL	DEFAULT(0)			--UNSIGNED
	,date_attached		datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	,PRIMARY KEY (bug_id, tag_id)
);;
CREATE NONCLUSTERED INDEX	idx_bug_tag_tag_id	ON	bug_tag(tag_id);;


----------------------------------------------------
-- 버그리포트에 관심있는 사용자 (CC)
----------------------------------------------------
CREATE TABLE bug_monitor
(
	 [user_id] 			Integer			NOT NULL	DEFAULT(0)			--UNSIGNED
	,bug_id 			Integer			NOT NULL	DEFAULT(0)			--UNSIGNED
	,PRIMARY KEY ([user_id], bug_id)
);;




----------------------------------------------------
-- 버그리포트와 관련있는 버그리포트 (Relationship)
----------------------------------------------------
CREATE TABLE bug_relationship
(
	 id 				Integer	  		NOT NULL	IDENTITY(1,1)			--UNSIGNED
	,source_bug_id		Integer	 		NOT NULL	DEFAULT(0)				--UNSIGNED
	,destination_bug_id	Integer			NOT NULL	DEFAULT(0)				--UNSIGNED
	,relationship_type 	SMALLINT		NOT NULL	DEFAULT(0)
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX idx_bug_relationship_source		ON bug_relationship(source_bug_id);;
CREATE NONCLUSTERED INDEX idx_bug_relationship_destination		ON bug_relationship(destination_bug_id);;


----------------------------------------------------
-- 버그리포트 속성 테이블 (relationship)
----------------------------------------------------
CREATE TABLE relationship
(
	 id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)      NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX relationship_sortkey_idx ON relationship(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX relationship_value_idx ON relationship(value);;
INSERT INTO relationship VALUES  (0,0, N'None'), (1,1, N'Duplicate Of'), (2,2, N'Has Duplicate'), (3,3, N'Child Of'), (4,4, N'Parent Of'), (5,5, N'Related To')
			


----------------------------------------------------
-- 공통정보 태그 정보 
----------------------------------------------------
CREATE TABLE tag
(
	 id					Integer			NOT NULL						--UNSIGNED
	,[user_id]			Integer			NOT NULL	DEFAULT(0)			--UNSIGNED
	,name				NVARCHAR(100)	NOT NULL	DEFAULT(N'')
	,[description]		TEXT			NOT NULL
	,date_created		datetime 					DEFAULT(1)			--DATETIME TO INTEGER
	,date_updated		datetime 					DEFAULT(1)			--DATETIME TO INTEGER
	,PRIMARY KEY (id, name)
);;
CREATE NONCLUSTERED INDEX	idx_tag_name						ON	tag(name);;







----------------------------------------------
-- 사용자 정보
----------------------------------------------
CREATE TABLE [user]
(
	 id 					Integer			NOT NULL						--UNSIGNED
	,username 				NVARCHAR(32) 	NOT NULL	DEFAULT(N'')
	,realname 				NVARCHAR(64) 	NOT NULL	DEFAULT(N'')
	,email 					NVARCHAR(64) 				DEFAULT(N'')
	,[role]					NVARCHAR(64) 				DEFAULT(N'')
	,create_ts				datetime
	,delta_ts				datetime
	--,[password] 			NVARCHAR(32) 	NOT NULL	DEFAULT(N'')
	--,date_created 		datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	--,last_visit 			datetime 		NOT NULL	DEFAULT(1)				--DATETIME TO INTEGER
	--,[enabled]				BIT				NOT NULL	DEFAULT(1)			--UNSIGNED
	--,protected 				BIT 			NOT NULL	DEFAULT(0)
	--,access_level 			SMALLINT 		NOT NULL	DEFAULT(10)
	--,login_count 			Integer	 		NOT NULL	DEFAULT(0)
	--,lost_password_request_count 	SMALLINT 		NOT NULL	DEFAULT(0)
	--,failed_login_count 	SMALLINT 		NOT NULL	DEFAULT(0)
	--,cookie_string 			NVARCHAR(64) 	NOT NULL	DEFAULT(N'')
	,PRIMARY KEY (id)
);;
CREATE UNIQUE NONCLUSTERED INDEX	idx_user_username		ON [user](username);;
INSERT INTO [user] VALUES	(1, N'administrator', N'admin', N'root@localhost', null, null,null);;
--CREATE UNIQUE NONCLUSTERED INDEX	idx_user_cookie_string	ON [user](cookie_string);;
--CREATE NONCLUSTERED INDEX			idx_enable				ON [user]([enabled]);;
--CREATE NONCLUSTERED INDEX			idx_access				ON [user](access_level);;

--IF	NOT EXISTS (SELECT * FROM [USER])
--BEGIN
--	DECLARE @timestamp AS INTEGER
--	SELECT @timestamp = CAST(CURRENT_TIMESTAMP AS TIMESTAMP)
--	INSERT INTO [user](username, realname, email, [password], date_created, last_visit, [enabled], protected, access_level, login_count, lost_password_request_count, failed_login_count, cookie_string) 
--	VALUES	('administrator', N'', 'root@localhost', '63a9f0ea7bb98050796b649e85481845', @timestamp , @timestamp, 1, 0, 90, 3, 0, 0, N'')--md5( mt_rand( 0, mt_getrandmax() ) + mt_rand( 0, mt_getrandmax() ) ) + md5(time()) )
--END;;





----------------------------------------------
-- 프로젝트 정보 (프로젝트)
----------------------------------------------
CREATE TABLE project_file
(
	 id 				Integer	  		NOT NULL	IDENTITY(1,1)			--UNSIGNED
	,project_id 		Integer			NOT NULL	DEFAULT(0)				--UNSIGNED
	,title 				NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,description 		NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,diskfile 			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,filename 			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,folder 			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,filesize 			Integer			NOT NULL	DEFAULT(0)
	,file_type 			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,date_added 		datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	,content 			Binary		 	NOT NULL
	,[user_id]			INTEGER 	NOT NULL	DEFAULT(0)
	,PRIMARY KEY (id)
);;


----------------------------------------------
-- 프로젝트 정보 (프로젝트)
----------------------------------------------
CREATE TABLE project
(
	 id 				Integer			NOT NULL						--UNSIGNED
	,name 				NVARCHAR(128) 	NOT NULL	DEFAULT(N'')
	,status 			SMALLINT 		NOT NULL	DEFAULT(10)
	,enabled 			BIT	 			NOT NULL	DEFAULT(1)			--UNSIGNED
	,view_state 		SMALLINT 		NOT NULL	DEFAULT(10)
	,access_min 		SMALLINT 		NOT NULL	DEFAULT(10)
	,file_path 			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
	,description 		TEXT	 		NOT NULL
	,category_id		INTEGER			NOT NULL	DEFAULT(1)			--UNSIGNED
	,inherit_global		INTEGER			NOT NULL	DEFAULT(0)			--UNSIGNED
	,PRIMARY KEY (id)
);;
--CREATE NONCLUSTERED INDEX			idx_project_id		ON project(id)
CREATE UNIQUE NONCLUSTERED INDEX	idx_project_name	ON project(name);;
CREATE NONCLUSTERED INDEX			idx_project_view	ON project(view_state);;




----------------------------------------------
-- 프로젝트 정보 (프로젝트 상관관계)
----------------------------------------------
CREATE TABLE project_hierarchy
(
	 child_id	Integer	NOT NULL	--UNSIGNED
	,parent_id	Integer	NOT NULL	--UNSIGNED
	,inherit_parent Integer 	NOT NULL	DEFAULT(0)		--UNSIGNED
);;
CREATE NONCLUSTERED INDEX	idx_project_hierarchy_child_id		ON	project_hierarchy(child_id);;
CREATE NONCLUSTERED INDEX	idx_project_hierarchy_parent_id		ON	project_hierarchy(parent_id);;



----------------------------------------------
-- 프로젝트 정보 (프로젝트 버전정보, version, fixed_in_version, target_version)
----------------------------------------------
CREATE TABLE project_version
(
	 id 				Integer	 		NOT NULL	
	,project_id 		Integer	  		NOT NULL	DEFAULT(0)				--UNSIGNED
	,[version] 			NVARCHAR(64) 	NOT NULL	DEFAULT(N'')
	,date_order 		datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	,[description] 		TEXT	 		NOT NULL
	,released 			BIT	 			NOT NULL	DEFAULT(1)			--UNSIGNED
	,obsolete			BIT				NOT NULL	DEFAULT(0)
	,PRIMARY KEY (id)
);;
CREATE UNIQUE NONCLUSTERED INDEX idx_project_version		ON project_version(project_id, [version]);;



----------------------------------------------
-- 프로젝트 정보 (카테고리)
----------------------------------------------
CREATE TABLE category
(
	 id					Integer			NOT NULL						--UNSIGNED
	,project_id			Integer											--UNSIGNED Belong to
	,user_id			Integer						DEFAULT(1)			--UNSIGNED Default UserID
	,name				NVARCHAR(128)	NOT NULL	DEFAULT(N'')
	,[status]			Integer			NOT NULL	DEFAULT(0)			--UNSIGNED
	,PRIMARY KEY (id)
);;
CREATE UNIQUE NONCLUSTERED INDEX	idx_category_project_name		ON category(project_id, name);;
INSERT INTO category VALUES(1, null, 1, N'General', 0);; --Administrator 기본값 할당.




----------------------------------------------------
-- 버그리포트 속성 테이블 (severity)
----------------------------------------------------
CREATE TABLE severity
(
	 id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)      NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX severity_sortkey_idx ON severity(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX severity_value_idx ON severity(value);;


----------------------------------------------------
-- 버그리포트 속성 테이블 (priority)
----------------------------------------------------
CREATE TABLE [priority]
(
	id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)     NOT NULL      --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX priority_sortkey_idx ON [priority](sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX priority_value_idx ON [priority](value);;



----------------------------------------------------
-- 버그리포트 속성 테이블 ([status])
----------------------------------------------------
CREATE TABLE [status]
(
	id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX status_sortkey_idx ON [status](sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX status_value_idx ON [status](value);;
 

----------------------------------------------------
-- 버그리포트 속성 테이블 (resolution)
----------------------------------------------------
CREATE TABLE resolution
(
	id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX resolution_sortkey_idx ON resolution(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX resolution_value_idx ON resolution(value);;


----------------------------------------------------
-- 버그리포트 속성 테이블 (reproducibility)
----------------------------------------------------
CREATE TABLE reproducibility
(
	id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX reproducibility_sortkey_idx ON reproducibility(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX reproducibility_value_idx ON reproducibility(value);;


----------------------------------------------------
-- 버그리포트 속성 테이블 (os)
----------------------------------------------------
CREATE TABLE os
(
	id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(128)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX os_sortkey_idx ON os(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX os_value_idx ON os(value);;



----------------------------------------------------
-- 버그리포트 속성 테이블 (os_build, os_version)
----------------------------------------------------
CREATE TABLE os_build
(
	id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(128)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX os_build_sortkey_idx ON os_build(sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX os_build_value_idx ON os_build(value);;


----------------------------------------------------
-- 버그리포트 속성 테이블 ([platform])
----------------------------------------------------
CREATE TABLE [platform]
(
	id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(128)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX platform_sortkey_idx ON [platform](sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX platform_value_idx ON [platform](value);;



----------------------------------------------------
-- 버그리포트 속성 테이블 (projection)
----------------------------------------------------
CREATE TABLE [projection]
(
	id						INTEGER									  --a unique ID.
	,sortkey				INTEGER		DEFAULT (0)     NOT NULL     --A number used to determine the order in which values are shown.
	,value					NVARCHAR(64)     DEFAULT (N'')     NOT NULL     --A possible value of the field
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX projection_sortkey_idx ON [projection](sortkey, value);;
CREATE UNIQUE NONCLUSTERED INDEX projection_value_idx ON [projection](value);;




----------------------------------------------
-- 부가정보 (리비전 정보...음??)
----------------------------------------------
CREATE TABLE bug_revision
(
	 id					Integer			NOT NULL	IDENTITY(1,1)	--UNSIGNED
	,bug_id				Integer			NOT NULL					--UNSIGNED
	,bugnote_id			Integer			NOT NULL	DEFAULT(0)		--UNSIGNED
	,[user_id]			Integer			NOT NULL					--UNSIGNED
	,[timestamp]		datetime 		NOT NULL	DEFAULT(1)		--DATETIME TO INTEGER
	,[type]				Integer			NOT NULL					--UNSIGNED
	,value				TEXT			NOT NULL
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX idx_bug_rev_id_time		ON bug_revision(bug_id, [timestamp]);;
CREATE NONCLUSTERED INDEX idx_bug_rev_type			ON bug_revision([type]);;



----------------------------------------------
-- 부가정보 (리비전 정보...음??)
----------------------------------------------
CREATE TABLE sponsorship
(
     id 				Integer	 		NOT NULL	IDENTITY(1,1)
	,bug_id 			Integer	 		NOT NULL	DEFAULT(0)
	,[user_id] 			Integer	 		NOT NULL	DEFAULT(0)
	,amount 			Integer	 		NOT NULL	DEFAULT(0)
	,logo 				NVARCHAR(128) 	NOT NULL	DEFAULT(N'')
	,url 				NVARCHAR(128) 	NOT NULL	DEFAULT(N'')
	,paid 				BIT	 			NOT NULL	DEFAULT(0)
	,date_submitted 	datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	,last_updated 		datetime 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
	,PRIMARY KEY (id)
);;
CREATE NONCLUSTERED INDEX idx_sponsorship_bug_id		ON sponsorship(bug_id);;
CREATE NONCLUSTERED INDEX idx_sponsorship_user_id		ON sponsorship([user_id]);;



----------------------------------------------
-- 부가정보 (안쓰는 정보들)
----------------------------------------------
--CREATE TABLE config
--(
--	 config_id		NVARCHAR(64) 	NOT NULL
--	,project_id		Integer							DEFAULT(0)
--	,[user_id]		Integer							DEFAULT(0)
--	,access_reqd	Integer							DEFAULT(0)
--	,[type]			Integer							DEFAULT(90)
--	,value			TEXT	 		NOT NULL
--	,PRIMARY KEY (config_id, project_id, [user_id])
--);;

--CREATE TABLE custom_field
--(
--	 id 				Integer	 		NOT NULL	IDENTITY(1,1)
--	,name 				NVARCHAR(64) 	NOT NULL	DEFAULT(N'')
--	,type 				SMALLINT 		NOT NULL	DEFAULT(0)
--	,possible_values 	TEXT		 	NOT NULL	DEFAULT(N'')
--	,default_value 		NVARCHAR(255) 	NOT NULL	DEFAULT(N'')
--	,valid_regexp 		NVARCHAR(255) 	NOT NULL	DEFAULT(N'')
--	,access_level_r 	SMALLINT 		NOT NULL	DEFAULT(0)
--	,access_level_rw 	SMALLINT 		NOT NULL	DEFAULT(0)
--	,length_min 		Integer	 		NOT NULL	DEFAULT(0)
--	,length_max 		Integer	 		NOT NULL	DEFAULT(0)
--	,require_report 	BIT	 			NOT NULL	DEFAULT(0)
--	,require_update 	BIT	 			NOT NULL	DEFAULT(0)
--	,display_report 	BIT	 			NOT NULL	DEFAULT(0)
--	,display_update 	BIT	 			NOT NULL	DEFAULT(1)			--UNSIGNED
--	,require_resolved 	BIT	 			NOT NULL	DEFAULT(0)
--	,display_resolved 	BIT	 			NOT NULL	DEFAULT(0)
--	,display_closed 	BIT	 			NOT NULL	DEFAULT(0)
--	,require_closed 	BIT	 			NOT NULL	DEFAULT(0)
--	,filter_by 			BIT	 			NOT NULL	DEFAULT(1)			--UNSIGNED
--	,PRIMARY KEY (id)
--);;
--CREATE NONCLUSTERED INDEX idx_custom_field_name		ON custom_field(name);;


--CREATE TABLE custom_field_project
--(
--	 field_id 			Integer	 	NOT NULL	DEFAULT(0)
--	,project_id 		Integer		NOT NULL	DEFAULT(0)			--UNSIGNED
--	,sequence			SMALLINT 	NOT NULL	DEFAULT(0)
--	,PRIMARY KEY (field_id, project_id)
--);;


--CREATE TABLE custom_field_string
--(
--	 field_id 			Integer	 		NOT NULL	DEFAULT(0)
--	,bug_id 			Integer	 		NOT NULL	DEFAULT(0)
--	,value 				NVARCHAR(255) 	NOT NULL	DEFAULT(N'')
--	,PRIMARY KEY (field_id, bug_id)
--);;
--CREATE NONCLUSTERED INDEX idx_custom_field_bug		ON custom_field_string(bug_id);;


--CREATE TABLE filters
--(
--	 id 				Integer	  		NOT NULL	IDENTITY(1,1)				--UNSIGNED
--	,user_id 			Integer	 		NOT NULL	DEFAULT(0)
--	,project_id 		Integer	 		NOT NULL	DEFAULT(0)
--	,is_public 			BIT							DEFAULT(NULL)
--	,name 				NVARCHAR(64)		NOT NULL	DEFAULT(N'')
--	,filter_string 	TEXT	 			NOT NULL
--	,PRIMARY KEY (id)
--);;


--CREATE TABLE news
--(
--	 id 				Integer			NOT NULL	IDENTITY(1,1)			--UNSIGNED
--	,project_id 		Integer	 		NOT NULL	DEFAULT(0)				--UNSIGNED
--	,poster_id 			Integer			NOT NULL	DEFAULT(0)				--UNSIGNED
--	,date_posted 		Integer 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
--	,last_modified 		Integer 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
--	,view_state 		SMALLINT 		NOT NULL	DEFAULT(10)
--	,announcement 		BIT	 			NOT NULL	DEFAULT(0)
--	,headline 			NVARCHAR(64)		NOT NULL	DEFAULT(N'')
--	,body 				TEXT	 		NOT NULL
--	,PRIMARY KEY (id)
--);;


--CREATE TABLE tokens
--(
--	 id					Integer			NOT NULL	IDENTITY(1,1)
--	,[owner]			Integer			NOT NULL
--	,[type]				Integer			NOT NULL
--	,[timestamp]		Integer 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
--	,expiry				Integer 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
--	,value				TEXT	 		NOT NULL
--	,PRIMARY KEY (id)
--);;
--CREATE NONCLUSTERED INDEX	idx_typeowner		ON	tokens(type, owner);;


--CREATE TABLE user_pref
--(
--	 id 					Integer			NOT NULL	IDENTITY(1,1)			--UNSIGNED
--	,[user_id] 				Integer	  		NOT NULL	DEFAULT(0)				--UNSIGNED
--	,project_id 			Integer	  		NOT NULL	DEFAULT(0)				--UNSIGNED
--	,default_profile 		Integer	  		NOT NULL	DEFAULT(0)				--UNSIGNED
--	,default_project 		Integer			NOT NULL	DEFAULT(0)				--UNSIGNED
--	,refresh_delay 			Integer	 		NOT NULL	DEFAULT(0)
--	,redirect_delay 		Integer			NOT NULL	DEFAULT(0)
--	,bugnote_order 			NVARCHAR(4) 		NOT NULL	DEFAULT('ASC')
--	,email_on_new 			BIT	 			NOT NULL	DEFAULT(0)
--	,email_on_assigned 		BIT	 			NOT NULL	DEFAULT(0)
--	,email_on_feedback 		BIT	 			NOT NULL	DEFAULT(0)
--	,email_on_resolved		BIT	 			NOT NULL	DEFAULT(0)
--	,email_on_closed 		BIT	 			NOT NULL	DEFAULT(0)
--	,email_on_reopened 		BIT	 			NOT NULL	DEFAULT(0)
--	,email_on_bugnote 		BIT	 			NOT NULL	DEFAULT(0)
--	,email_on_status 		BIT	 			NOT NULL	DEFAULT(0)
--	,email_on_priority 		BIT	 			NOT NULL	DEFAULT(0)
--	,email_on_priority_min_severity 	SMALLINT 		NOT NULL	DEFAULT(10)
--	,email_on_status_min_severity 		SMALLINT 		NOT NULL	DEFAULT(10)
--	,email_on_bugnote_min_severity 		SMALLINT 		NOT NULL	DEFAULT(10)
--	,email_on_reopened_min_severity 	SMALLINT 		NOT NULL	DEFAULT(10)
--	,email_on_closed_min_severity 		SMALLINT 		NOT NULL	DEFAULT(10)
--	,email_on_resolved_min_severity 	SMALLINT 		NOT NULL	DEFAULT(10)
--	,email_on_feedback_min_severity		SMALLINT 		NOT NULL	DEFAULT(10)
--	,email_on_assigned_min_severity 	SMALLINT 		NOT NULL	DEFAULT(10)
--	,email_on_new_min_severity 			SMALLINT 		NOT NULL	DEFAULT(10)
--	,email_bugnote_limit 				SMALLINT 		NOT NULL	DEFAULT(0)
--	,[language]							NVARCHAR(32)		NOT NULL	DEFAULT('english')
--	,timezone NVARCHAR(32) 	NOT NULL	DEFAULT(N'')
--	,PRIMARY KEY (id)
--);;


--CREATE TABLE user_print_pref
--(
--	 [user_id] 				Integer	  		NOT NULL	DEFAULT(0)			--UNSIGNED
--	,print_pref 			NVARCHAR(64) 	NOT NULL	DEFAULT(N'')
--	,PRIMARY KEY ([user_id])
--);;


--CREATE TABLE user_profile
--(
--	 id 					Integer			NOT NULL	IDENTITY(1,1)			--UNSIGNED
--	,[user_id] 				Integer			NOT NULL	DEFAULT(0)				--UNSIGNED
--	,[platform] 			NVARCHAR(32) 	NOT NULL	DEFAULT(N'')
--	,os 					NVARCHAR(32) 	NOT NULL	DEFAULT(N'')
--	,os_build 				NVARCHAR(32) 	NOT NULL	DEFAULT(N'')
--	,description 			TEXT	 		NOT NULL
--	,PRIMARY KEY (id)
--);;


--CREATE TABLE email
--(
--	  email_id 			Integer			NOT NULL	IDENTITY(1,1)		--UNSIGNED
--	,email		 		NVARCHAR(64) 	NOT NULL	DEFAULT(N'')
--	,subject			NVARCHAR(250) 	NOT NULL	DEFAULT(N'')
--	,submitted 			Integer 		NOT NULL	DEFAULT(1)			--DATETIME TO INTEGER
--	,metadata 			TEXT	 		NOT NULL
--	,body 				TEXT	 		NOT NULL
--	,PRIMARY KEY (email_id)
--);;
--CREATE NONCLUSTERED INDEX idx_email_id		ON email(email_id);;


--CREATE TABLE plugin
--(
--	 basename			NVARCHAR(40)		NOT NULL
--	,enabled			BIT				NOT NULL	DEFAULT(0)
--	,protected			BIT				NOT NULL	DEFAULT(0)
--	,priority			Integer			NOT NULL	DEFAULT(3)		--UNSIGNED
--	,PRIMARY KEY (basename)
--);;
--INSERT INTO plugin(basename, [enabled]) VALUES ('MantisCoreFormatting', 1);;


 


 ----------------------------------------------------------------
----------------------------------------------------------------
-- Function 정의
----------------------------------------------------------------
----------------------------------------------------------------

----------------------------------------------------------------
-- int값을 담고있는 데이터를 테이블로 변환
----------------------------------------------------------------
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
		SET @delimiter = N','
	
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

----------------------------------------------------------------
-- char값을 담고있는 데이터를 테이블로 변환
----------------------------------------------------------------
CREATE FUNCTION getCharArray
(
	 @delimiter		as NVARCHAR(2) = null
	,@strItems		as NVARCHAR(MAX)
)
RETURNS @Items TABLE 
(
    Item	NVARCHAR(128) primary key NOT NULL
)
AS
BEGIN
	DECLARE @sp_len		as INTEGER
	DECLARE @len		as INTEGER
	DECLARE @s			as INTEGER
	DECLARE @e			as INTEGER
	DECLARE @str		as NVARCHAR(128)
	DECLARE @item		as NVARCHAR(128)

	if @strItems IS NULL
		RETURN
	--작업을 위한 초기설정
	IF @delimiter is null					--기본 DELIMITER 지정
		SET @delimiter = N','
	
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
			IF NOT EXISTS (SELECT Item FROM @Items Where Item = @str)
				INSERT INTO @Items VALUES (@str)
		END

		SET @s = @e+@sp_len
	END

	RETURN 
END;;





----------------------------------------------------------------
----------------------------------------------------------------
-- Procedure 정의
----------------------------------------------------------------
----------------------------------------------------------------


----------------------------------------------
-- 사용자 등록
----------------------------------------------
CREATE PROCEDURE registerUser
(	 
	 @id		as integer			= null
	,@name		as NVARCHAR(128)
	,@realname	as NVARCHAR(128)		= N''
	,@email		as NVARCHAR(128)		= null
	,@role		as NVARCHAR(64)		= null
)
as
BEGIN
	Declare @old_id  as integer
	SELECT @old_id = id FROM [user] where username = @name
	--기존에 존재하는 회원인지 확인 //존재하면 해당 ID반환.
	IF (@old_id is null)
	BEGIN
		--등록
		IF (@id is null )
		BEGIN
			SELECT @id = max(id)+1 from [user]
			IF (@id is null or @id < 100000)
				SET @id = 100000
		END

		INSERT INTO [user] VALUES (@id, @name, @realname, @email, @role, NULL, NULL)
	END
	ELSE
	BEGIN
		IF (@old_id >= 100000 and @id is not null)
		BEGIN
			UPDATE T SET id = @id FROM [user] T WHERE id = @old_id
			
			--관련 테이블의 아이디 값 변경.			
			UPDATE T SET reporter_id = @id FROM bug T WHERE reporter_id = @old_id
			UPDATE T SET handler_id = @id FROM bug T WHERE handler_id = @old_id
			UPDATE T SET reporter_id = @id FROM bugnote T WHERE reporter_id = @old_id
			UPDATE T SET [user_id] = @id FROM bug_file T WHERE [user_id] = @old_id
			UPDATE T SET [user_id] = @id FROM bug_history T WHERE [user_id] = @old_id

			UPDATE T SET [user_id] = @id FROM bug_tag T WHERE [user_id] = @old_id
			UPDATE T SET [user_id] = @id FROM bug_monitor T WHERE [user_id] = @old_id
			UPDATE T SET [user_id] = @id FROM tag T WHERE [user_id] = @old_id

			UPDATE T SET [user_id] = @id FROM project_file T WHERE [user_id] = @old_id
			UPDATE T SET [user_id] = @id FROM category T WHERE [user_id] = @old_id
			UPDATE T SET [user_id] = @id FROM bug_revision T WHERE [user_id] = @old_id
			UPDATE T SET [user_id] = @id FROM sponsorship T WHERE [user_id] = @old_id
		END
		IF @role is not null
		BEGIN
			UPDATE T SET [role] = @role FROM [user] T WHERE id = @id
		END
	END

	--등록결과 반환
	SELECT @id = id FROM [user] where username=@name
	return @id
END;;



-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveProject
(
	 @id		as integer		= null
	,@name		as NVARCHAR(64)
	,@parent_id	as integer			= null
	,@desc		as NVARCHAR(1024)	= N''
)
as
BEGIN
	IF @name is null	RETURN -1
	IF @name = N''		RETURN 0

	DECLARE @tIDX as integer
	SET @tIDX = CHARINDEX('»',@name,1)
	IF (@tIDX >0) SET @name = ltrim(rtrim(substring(@name,@tIDX+1,len(@name))))


	--등록되어있다면 해당아이디 반환
	DECLARE @old_id as integer
	SELECT @old_id = id  FROM project WHERE name = @name
	IF (@old_id is not null) RETURN @old_id

	IF @id is null
	BEGIN
		--없으면 아이디 생성.
		SELECT @id = max(id)+1 FROM project 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
		

	--아이디 등록.
	INSERT INTO project VALUES(@id, @name, 10,1,10,10,N'', @desc, 1,0)
	IF @parent_id is not null
	BEGIN
		INSERT INTO project_hierarchy VALUES (@id, @parent_id, 0)
	END

	RETURN @id
END;;

-----------------------------------------------------------
-- 카테고리 정보 추가 (id가 없는경우는 100000이상의값을 할당하여 추가)
-----------------------------------------------------------
CREATE PROCEDURE saveCategory
(	
	 @id			as integer = null
	,@name			as NVARCHAR(64)
	,@project_id	as integer
	,@user_id		as integer = null	
)
AS
BEGIN
	IF @name is null		RETURN -1
	IF @project_id is NULL  RETURN -1
	IF @user_id is null		SET @user_id = 1 --Administrator
	IF @name = N''			RETURN 0


	--category 정보에 project정보가 포함된 경우 project명과 category명을 분리
	Declare @pname as NVARCHAR(64)
	Declare @cname as NVARCHAR(64)
	Declare @sIdx as integer
	Declare @eIdx as integer
	SET @sIdx = charindex('[',@name,1)+1
	SET @eIdx = charindex(']',@name,1)
	IF (@sIdx >0 and @eIdx >@sIdx)
	BEGIN
		SET @pname = ltrim(rtrim(substring(@name,@sIdx,@eIdx-@sIdx)))
		SET @cname = ltrim(rtrim(substring(@name,@eIdx+1,len(@name))))
	END
	ELSE 
	BEGIN 
		SET @pname = null
		SET @cname = ltrim(rtrim(@name))
	END

	--pname 이 All Projects인 경우는 project_id를 null로 나머지는 project에 있으면 id를 가져오고 없으면 등록.
	IF (@pname = 'All Projects')	SET @project_id = null
	ELSE IF (@pname is not null)
	BEGIN
		DECLARE @tID as integer
		EXEC @tID = saveProject @name = @pname
		IF (@tID is null) PRINT @tID
		ELSE IF (@tID != @project_id) SET @project_id = @tID		
	END


	--등록되어있다면 해당아이디 반환
	DECLARE @old_id as INTEGER
	IF @project_id is null
		SELECT @old_id = id  FROM category WHERE name = @cname and project_id is null
	ELSE
		SELECT @old_id = id  FROM category WHERE name = @cname and project_id = @project_id
	IF (@old_id is not null) RETURN @old_id
	

	--입력된 값이 없으면 100000이상의 값을 할당.
	IF @id is null
	BEGIN
		SELECT @id = max(id)+1 FROM category
		IF @id is null
			SET @id = 100000 --사용자 정의값은 100000 이상의 값을 가지도록 함. (현재 맨티스의 경우에는 id가있는 값이 먼저 들어오므로 그냥 카운팅됨)
	END
	
	INSERT INTO category VALUES(@id, @project_id, @user_id, @name, 0)

	RETURN @id
END;;

-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------
CREATE PROCEDURE saveTag
(
	 @id  	as INTEGER       =null
	,@name 	as NVARCHAR(64)
)
AS
BEGIN
	IF @name is null	RETURN -1
	IF @name = N''		RETURN 0

		--기존에 있는 값인지 검사.
	DECLARE @old_id as integer
	SELECT @old_id = id FROM tag WHERE name=@name
	IF (@old_id is not null)	RETURN @id

	--id가 없는 경우 id값을 할당.
	IF @id is null
	BEGIN
		SELECT @id = max(id)+1 FROM tag
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END

	--정보 삽입.
	INSERT INTO tag VALUES(@id, 0, @name, N'', null,null)
	RETURN @id
END;;


----------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------
CREATE PROCEDURE saveHistory
(
	 @bug_id as integer
	,@user_id as integer
	,@when as NVARCHAR(64)
	,@field_name as NVARCHAR(64)
	,@old_value as text			=N''
	,@new_value as text			=N''
)
as
BEGIN
	IF @user_id is null		RETURN -1
	IF @bug_id is null	RETURN -1
	IF @when is null	RETURN -1
	IF @field_name is null	RETURN -1


	INSERT INTO bug_history VALUES(@user_id, @bug_id, CONVERT(datetime, @when, 120), @field_name, @old_value, @new_value,0)
	
	RETURN 1
END;;



-----------------------------------------------------------
--버그리포트에 관심있는 사람을 추가
-----------------------------------------------------------
CREATE PROCEDURE saveCC
(
	 @bug_id as integer
	,@username as NVARCHAR(64)
)
AS
BEGIN
	IF @bug_id is null RETURN -1
	IF @username is null RETURN -1
	
	--해당 이름에 대한 아이디를 가져옴.
	DECLARE @user_id as integer
	EXEC @user_id = registerUser @name = @username

	--관계정보 삽입.
	BEGIN TRY
		INSERT INTO bug_monitor VALUES(@bug_id, @user_id)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH
	
	return 1
END;;


-----------------------------------------------------------
--버그리포트들 간의 관계를 추가
-----------------------------------------------------------
CREATE PROCEDURE saveRelationship
(
	 @src_bug_id as integer
	,@dest_bug_id as integer
	,@type as integer
)
AS
BEGIN
	IF @src_bug_id is null RETURN -1
	IF @dest_bug_id is null RETURN -1
	IF @type is null RETURN -1

	BEGIN TRY
		INSERT INTO bug_relationship VALUES (@src_bug_id, @dest_bug_id, @type)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH

	return 1
END;;

-----------------------------------------------------------
-- 버그 노트들을 추가. (코멘트)
-----------------------------------------------------------
CREATE PROCEDURE saveBugnote
(
	 @bug_id		int
	,@who			integer
	,@view_state	tinyint = 10
	,@creation_ts	NVARCHAR(64)
	,@update_ts		NVARCHAR(64)
	,@type			smallint = 0
	,@attr 			NVARCHAR(250)  = null
	,@thetext		text
)
AS
BEGIN
	IF (@bug_id is null)		RETURN -1
	IF (@who is null)			RETURN -1
	IF (@creation_ts is null)	RETURN -1
	IF (@thetext is null)		RETURN -1
	IF (@update_ts is null)		SET @update_ts = @creation_ts	


	DECLARE @id  as integer

	INSERT INTO bugnote VALUES ( @bug_id
								,@who
								,@view_state		
								,convert(datetime, @creation_ts	,120)
								,convert(datetime, @update_ts	,120)
								,@type
								,@attr
								,0)

	SET @id = @@IDENTITY

	INSERT INTO bugnote_text VALUES ( @id, @thetext)

	RETURN @id
END;;


-----------------------------------------------------------
-- 첨부파일 저장.
-----------------------------------------------------------
CREATE PROCEDURE saveAttachment
(
	  @file_id			int				
	, @bug_id			int			
	, @user_id			integer			= 0	
	, @title			NVARCHAR(250)	=N''
	, @description		text			=N''
	, @filename			NVARCHAR(100)	
	, @filesize			integer
	, @file_type		NVARCHAR(250)	= 'plain/text'
	, @creation_ts		NVARCHAR(64)
	, @thedata			NVARCHAR(max)	=	null
)
AS
BEGIN

	BEGIN TRY
			INSERT INTO bug_file VALUES ( @file_id		
										 ,@bug_id	
										 ,@user_id
										 ,@title		
										 ,@description
										 ,N''
										 ,@filename
										 ,N''
										 ,@filesize
										 ,@file_type
										 ,convert(datetime, @creation_ts, 120)
										 ,CASE WHEN @thedata is null THEN null
										       ELSE convert(varbinary(max),@thedata) END
										)
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)	RETURN 0 
		ELSE RETURN -1
	END CATCH
			
	RETURN 1
END;;


		--IF (@thedata is null)
		--	INSERT INTO bug_file VALUES ( @file_id		
		--								 ,@bug_id	
		--								 ,@user_id
		--								 ,@title		
		--								 ,@description
		--								 ,N''
		--								 ,@filename
		--								 ,N''
		--								 ,@filesize
		--								 ,@file_type
		--								 ,convert(datetime, @creation_ts, 120)
		--								 ,null
		--								)
		--ELSE

-----------------------------------------------------------
-- Severity에 관련된 정보를 저장.
-----------------------------------------------------------
CREATE PROCEDURE saveSeverity
(
	 @id 		as INTEGER      = null
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from severity where value = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM severity 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO severity VALUES (@id, @id, @value)

	RETURN @id
END;;


-----------------------------------------------------------
-- Priority 에 관련된 정보를 저장.
-----------------------------------------------------------
CREATE PROCEDURE savePriority
(
	 @id 		as INTEGER      = null
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from [priority] where value = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM [priority] 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO [priority] VALUES (@id, @id, @value)

	RETURN @id
END;;

-----------------------------------------------------------
-- Status 에 관련된 정보를 저장.
-----------------------------------------------------------
CREATE PROCEDURE saveStatus
(
	 @id 		as INTEGER      = null
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from [status] where value = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM [status] 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO [status] VALUES (@id, @id, @value)

	RETURN @id
END;;


-----------------------------------------------------------
-- Resolution 에 관련된 정보를 저장.
-----------------------------------------------------------
CREATE PROCEDURE saveResolution
(
	 @id 		as INTEGER      = null
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from [resolution] where value = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM [resolution] 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO [resolution] VALUES (@id, @id, @value)

	RETURN @id
END;;



-----------------------------------------------------------
-- Reproducibility 에 관련된 정보를 저장.
-----------------------------------------------------------
CREATE PROCEDURE saveReproducibility
(
	 @id 		as INTEGER      = null
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from [reproducibility] where value = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM [reproducibility] 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO [reproducibility] VALUES (@id, @id, @value)

	RETURN @id
END;;



-----------------------------------------------------------
-- OS에 관련된 정보를 저장.
-----------------------------------------------------------
CREATE PROCEDURE saveOS
(
	 @id 		as INTEGER      = null
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from os where value = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM os 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO os VALUES (@id, @id, @value)

	RETURN @id
END;;


-----------------------------------------------------------
-- Osbuild 에 관련된 정보를 저장.
-----------------------------------------------------------
CREATE PROCEDURE saveOsbuild
(
	 @id 		as INTEGER      = null
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from [os_build] where value = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM [os_build] 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO [os_build] VALUES (@id, @id, @value)

	RETURN @id
END;;



-----------------------------------------------------------
-- Platform 에 관련된 정보를 저장.
-----------------------------------------------------------
CREATE PROCEDURE savePlatform
(
	 @id 		as INTEGER      = null
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from [platform] where value = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM [platform] 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO [platform] VALUES (@id, @id, @value)

	RETURN @id
END;;



-----------------------------------------------------------
-- Projection 에 관련된 정보를 저장.
-----------------------------------------------------------
CREATE PROCEDURE saveProjection
(
	 @id 		as INTEGER      = null
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from [projection] where value = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM [projection] 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO [projection] VALUES (@id, @id, @value)

	RETURN @id
END;;


-----------------------------------------------------------
-- Version 
-----------------------------------------------------------
CREATE PROCEDURE saveVersion
(
	 @id 		as INTEGER      = null
	,@project_id as INTEGER
	,@value 	as NVARCHAR(64)
)
AS
BEGIN
	IF (@value is null)	RETURN -1
	IF (@project_id is null) RETURN -1
	IF (@value = N'')	RETURN 0

	DECLARE	@old_id as integer
	SELECT @old_id = id from [project_version] where project_id = @project_id and [version] = @value
	IF (@old_id is not NULL)  RETURN @old_id

	--id가 없는 경우 임시로 아이디 할당.
	IF (@id is null)
	BEGIN
		SELECT @id = max(id)+1 FROM [project_version] 
		IF @id is null
			SET @id = 100000 --사용자 정의값은 10000 이상의 값을 가지도록 함.
	END
	
	INSERT INTO [project_version] VALUES (@id, @project_id, @value,1,N'',1,0)

	RETURN @id
END;;


----------------------------------------------------------------
-- 키워드와 버그리포트의 관계를 생성
----------------------------------------------------------------
CREATE PROCEDURE saveKeywordMap
(
	 @bug_id as integer
	,@keywords as NVARCHAR(max)	--원래는 text타입이지만 변경. (text는 len함수를 사용못함)
)
AS
BEGIN
	
	INSERT INTO bug_tag	
	select 
		 @bug_id
		,t.id
		,0
		,1
	from tag t
	join getCharArray(',',@keywords) k on t.name = k.Item
END;;


-----------------------------------------------------------
-----------------------------------------------------------
-----------------------------------------------------------


CREATE PROCEDURE saveBugReport
(
	 @id 				as INTEGER
	,@project 			as NVARCHAR(128)
	,@category			as NVARCHAR(128)
	,@reporter_id 		as INTEGER
	,@handler_id 		as INTEGER		 = 0
	,@duplicate_id 		as INTEGER       = 0
	,@priority			as NVARCHAR(64)
	,@severity 			as NVARCHAR(64)
	,@status			as NVARCHAR(64)
	,@resolution 		as NVARCHAR(64)
	,@reproducibility 	as NVARCHAR(64)				
	,@os 				as NVARCHAR(64) 		= N''
	,@os_build 			as NVARCHAR(64) 		= N''
	,@platform	 		as NVARCHAR(64) 		= N''
	,@build 			as NVARCHAR(32) 		= N''
	,@version 			as NVARCHAR(64) 		= N''
	,@fixed_in_version 	as NVARCHAR(64) 		= N''
	,@target_version	as NVARCHAR(64)  	= N''
	,@summary 			as NVARCHAR(128)
	,@date_submitted 	as NVARCHAR(128)
	,@last_updated 		as NVARCHAR(128)
	,@due_date       	as NVARCHAR(128)		= N''
	,@projection 		as SMALLINT 		= 10
	,@profile_id 		as INTEGER 			= 0
	,@view_state 		as INTEGER 			= 10
	,@eta 				as SMALLINT 		= 10
	,@sponsorship_total 	as INTEGER 		= 0
	,@sticky				as BIT 			= 0
	,@tags				as NVARCHAR(max)	 	= N''
	,@description		as NVARCHAR(max)	 	= N''
	,@step_procedure	as NVARCHAR(max)	 	= N''
	,@additional_info	as NVARCHAR(max)	 	= N''
)
AS
BEGIN
	IF @reporter_id is null RETURN -1
	IF @handler_id is null RETURN -1
	IF @project is null RETURN -1
	IF @category is null RETURN -1


	--속성값들에 대한 id획득.
	DECLARE @project_id 			as INTEGER
	DECLARE @category_id 	as INTEGER

	DECLARE @priority_id 			as INTEGER
	DECLARE @severity_id 			as INTEGER
	DECLARE @status_id 				as INTEGER
	DECLARE @resolution_id 			as INTEGER
	DECLARE @reproducibility_id 	as INTEGER

	EXEC @project_id = saveProject @name = @project
	EXEC @category_id = saveCategory @name = @category, @project_id = @project_id

	EXEC @priority_id = savePriority @value = @priority
	EXEC @severity_id = saveSeverity @value = @severity
	EXEC @status_id = saveStatus   @value = @status
	EXEC @resolution_id = saveResolution @value = @resolution
	EXEC @reproducibility_id = saveReproducibility @value = @reproducibility

	

	--속성값 결과 체크.
	IF @priority_id is null RETURN -1
	IF @severity_id is null RETURN -1
	IF @status_id is null RETURN -1
	IF @resolution_id is null RETURN -1
	IF @reproducibility_id is null RETURN -1
	
	--버그리포트 등록
	BEGIN TRY
		INSERT INTO bug VALUES( @id 				
							   ,@project_id
							   ,@category_id		

							   ,@reporter_id
							   ,@handler_id
							   ,@duplicate_id
							   
							   ,@priority_id
							   ,@severity_id
							   ,@status_id	
							   ,@resolution_id
							   ,@reproducibility_id

							   ,@os 				
							   ,@os_build 			
							   ,@platform	 		
							   ,@build 
							   ,@version 			
							   ,@fixed_in_version 	
							   ,@target_version	

							   ,@summary 			
							   ,CONVERT(datetime, @date_submitted, 120)
							   ,CONVERT(datetime, @last_updated , 120)
							   ,CONVERT(datetime, @due_date, 120) 	
							   
							   ,@view_state 	
							   ,@projection
							   ,@profile_id
							   ,@eta
							   ,@sponsorship_total
							   ,@sticky
							   ,@tags
							   )
	END TRY
	BEGIN CATCH
		IF (ERROR_NUMBER() = 2601)
		BEGIN
			UPDATE b 
			SET
				  project_id 		= @project 			
				 ,category_id		= @category			
				 ,reporter_id 		= @reporter_id 		
				 ,handler_id 		= @handler_id 		
				 ,duplicate_id 		= @duplicate_id 		
				 ,[priority]		= @priority			
				 ,severity 			= @severity 			
				 ,[status]			= @status			
				 ,resolution 		= @resolution 		
				 ,reproducibility 	= @reproducibility 	
				 ,os 				= @os 				
				 ,os_build 			= @os_build 			
				 ,[platform]		= @platform	 		
				 ,build 			= @build
				 ,[version] 		= @version 			
				 ,fixed_in_version	= @fixed_in_version 	
				 ,target_version	= @target_version	
				 ,summary 			= @summary 			
				 ,date_submitted	= CONVERT(datetime, @date_submitted, 120)
				 ,last_updated		= CONVERT(datetime, @last_updated , 120)
				 ,due_date			= CONVERT(datetime, @due_date, 120) 	
				 ,projection 		= @projection 		
				 ,profile_id 		= @profile_id 		
				 ,view_state 		= @view_state 		
				 ,eta 				= @eta 				
				 ,sponsorship_total	= @sponsorship_total 
				 ,sticky			= @sticky
				 ,tags				= @tags
			FROM bug b
			WHERE id = @id
		END
		ELSE RETURN -1
	END CATCH

	--bug_text추가.
	INSERT INTO bug_text VALUES (@id, @description, @step_procedure, @additional_info)



	--등록후 부가작업---------------------------------------------------------
	exec saveKeywordMap @id, @tags
	exec saveVersion null, @project_id, @version
	exec saveVersion null, @project_id, @fixed_in_version
	exec saveVersion null, @project_id, @target_version

	exec saveOS @value= @os
	exec saveOsbuild @value= @os_build
	exec savePlatform @value= @platform
END;;


---------------------------------------------
-- 재시작할 아이디 목록 구하기
---------------------------------------------
CREATE PROCEDURE getRevivalIDList
(
	@ret NVARCHAR(max) out
)
AS
BEGIN

	Declare ID_CURSOR CURSOR FOR SELECT bug_id FROM bugs_manager WHERE progress in ('PENDING', 'FAIL')
	Declare @id as integer

	--기본값 설정
	SET @ret = N''

	
	--커서 시작
	OPEN ID_CURSOR
	FETCH NEXT FROM ID_CURSOR	INTO @id

	WHILE @@FETCH_STATUS = 0
	BEGIN
		
		SET @ret = @ret + convert(NVARCHAR(64),@id)

		FETCH NEXT FROM ID_CURSOR	INTO @id
		
		IF (@@FETCH_STATUS = 0) 
			SET @ret = @ret + ','
	END
	CLOSE ID_CURSOR
	DEALLOCATE ID_CURSOR
	RETURN 1
END;;
