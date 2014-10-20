

-------------------------------------------------------------
-- Ticket의 메타정보들을 가짐
-------------------------------------------------------------
CREATE TABLE ticket
(
	 id				integer				NOT NULL	identity(1,1)		--e.g. 1 for ticket #1
	,[type]			NVARCHAR(128)		NOT NULL						--Can be joined with table enum field name where field type is ticket_type (update, enhancement..etc)
	
	,[time]			datetime			NOT NULL						--Creation time
	,changetime		datetime			NOT NULL						--Time of last modification or comment

	,component		NVARCHAR(128)		NOT NULL						--Can be joined with table component field name
	
	,severity		NVARCHAR(128)		NOT NULL						--Can be joined with table enum field name where field type is severity
	,[priority]		NVARCHAR(128)		NOT NULL						--Can be joined with table enum field name where field type is priority
	
	,[owner]		NVARCHAR(128)		NOT NULL						--User name or email address of ticket owner
	,reporter		NVARCHAR(128)		NOT NULL						--User name or email address of ticket reporter
	
	,cc				NVARCHAR(128)		NOT NULL						--Comma separated list of email addresses to be CC'd notifications
	
	,[version]		NVARCHAR(128)		NOT NULL						--Can be joined with table version field name
	,milestone		NVARCHAR(128)		NOT NULL						--Can be joined with table milestone field name
	
	,[status]		NVARCHAR(128)		NOT NULL						--Ticket status e.g. new or fixed
	,resolution		NVARCHAR(128)		NOT NULL						--Can be joined with table enum field name where field type is resolution
	
	,summary		NVARCHAR(128)		NOT NULL						--Short title-like summary
	,[description]	NVARCHAR(max)		NOT NULL						--Long description text
	,keywords		NVARCHAR(max)		NOT NULL						--Comma separated list of arbitrary keywords
	,PRIMARY KEY(id)
);;
CREATE NONCLUSTERED INDEX idx_ticket_time ON ticket([time]);;
CREATE NONCLUSTERED INDEX idx_ticket_status ON ticket([status]);;


-------------------------------------------------------------
-- Ticket의 타입값들을 저장하고 있는 테이블. (ticket_type, severity, priority, resolution
-------------------------------------------------------------
CREATE TABLE enum
(
	 [type]			NVARCHAR(64)		NOT NULL			--ticket_type, severity, priority,resolution중에 하나를 가짐
	,name			NVARCHAR(128)	NOT NULL			--Name of the enum value
	,value			integer			NOT NULL			-- Ordering value sortkey
	,PRIMARY KEY([type], name)
);;


-------------------------------------------------------------
-- Ticket의 Componenet 이름을 저장하고 있음 (아이디 없어도 될까?)
-------------------------------------------------------------
CREATE TABLE component
(
	 name			NVARCHAR(128)	NOT NULL	--Ticket component name
	,[owner]		integer			NOT NULL	--User name of component owner
	,[description]	NVARCHAR(max)	NOT NULL	--Component description
	,PRIMARY KEY(name)
);;


-------------------------------------------------------------
-- Ticket의 커스텀 필드를 정의
-------------------------------------------------------------
CREATE TABLE ticket_custom
(
	 ticket			integer			NOT NULL	--Can be joined with table ticket field id
	,name			NVARCHAR(128)	NOT NULL	--Custom ticket field name
	,value			integer			NOT NULL	--Current field value
	,PRIMARY KEY(ticket, name)
);;


-------------------------------------------------------------
-- Ticket의 변경상태를 저장.(history)
-------------------------------------------------------------
CREATE TABLE ticket_change
(
	 id				integer				NOT NULL	identity(1,1)	--key
	,ticket			int					NOT NULL					--Can be joined with table ticket field id
	,[time]			datetime			NOT NULL					--Time of this change or comment (group to get all related entries)
	,author			integer				NOT NULL					--Author of this change or comment
	,field			TEXT				NOT NULL					--Name of the changed field		'이 값이 comment인경우  oldvalue는 comment 번호, newvalue에 description이 저장됨.
	,oldvalue		TEXT				NOT NULL					--Previous value of that field    
	,newvalue		TEXT				NOT NULL					--New value of that field
	,PRIMARY KEY(id)
);;
CREATE NONCLUSTERED INDEX idx_ticket_change_ticket ON ticket_change(ticket);;
CREATE NONCLUSTERED INDEX idx_ticket_change_time ON ticket_change([time]);;




-------------------------------------------------------------
-- Ticket의 첨부파일 목록
-------------------------------------------------------------
CREATE TABLE attachment
(
	 id				integer			NOT NULL	identity(1,1)	--key
	,[type]			NVARCHAR(64)		NOT NULL					--Realm (e.g. wiki for wiki page attachments, ticket for ticket attachments)
	,related_id		integer			NOT NULL					--Resource id (ticket id or wiki page name)
	,[filename]		NVARCHAR(512)	NOT NULL					--Attachment file name
	,size			int				NOT NULL					--Attachment file size in bytes
	,[time]			datetime		NOT NULL					--Time attachment was uploaded
	,[description]	NVARCHAR(max)	NOT NULL					--
	,author			integer			NOT NULL					--User name of uploader
	,ipnr			NVARCHAR(128)	NOT NULL					--IP address of the uploader
	,PRIMARY KEY(id)
);;


-------------------------------------------------------------
-- Ticket의 뭘까?
-------------------------------------------------------------
CREATE TABLE report
(
	 id				integer			NOT NULL identity(1,1)
	,author			integer			NOT NULL	--
	,title			NVARCHAR(512)	NOT NULL	--
	,query			NVARCHAR(512)	NOT NULL	--
	,description	NVARCHAR(max)	NOT NULL	--
	,PRIMARY KEY (ID)
);;