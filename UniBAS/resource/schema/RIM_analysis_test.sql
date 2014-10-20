--drop database Analaysis_bug1
--select * from analysis
--delete from analysis
--DBCC CHECKIDENT('analysis', RESEED, 0)
use Analysis_Dupe
use UniBAS
GO
--------------------------------------------------------------
-- 1. �м��ν��Ͻ� ����
--------------------------------------------------------------
PRINT N'�м��ν��Ͻ� ���� ����'
DECLARE @ANALYSIS_ID	AS integer			= 3
DECLARE @TITLE			AS NVARCHAR(256)	= N'DuplicateTest'
DECLARE @DESCRIPTION	AS NVARCHAR(256)	= N'-----------'
DECLARE @SCHEMA_NAME	AS NVARCHAR(256)	= N'Analysis_Dupe'
DECLARE @SITE_ID		AS integer			= 6
DECLARE @PROJECT_ID		AS NVARCHAR(MAX)	= N'7,15,16'	-- N'1,7,8,9,10,11'
DECLARE @IS_UNIFORMLY	AS BIT				= 1
DECLARE @START_DATE		AS DATETIME			= null
DECLARE @END_DATE		AS DATETIME			= null
DECLARE @CONDITION		AS NVARCHAR(MAX)	= null


CREATE PROCEDURE saveAnalysisInfo
(
)
AS
BEGIN
	IF not exists (SELECT * FROM analysis WHERE title = @TITLE OR [schema_name] = @SCHEMA_NAME)
	BEGIN
		INSERT INTO analysis VALUES	(@TITLE, @DESCRIPTION, @SCHEMA_NAME, @IS_UNIFORMLY, @START_DATE, @END_DATE,@CONDITION)
		SET @ANALYSIS_ID = @@IDENTITY

	END
	ELSE
	BEGIN
		UPDATE A SET 
			 [DESCRIPTION]	=@DESCRIPTION
			,[IS_UNIFORMLY]	=@IS_UNIFORMLY
			,[START_DATE]	=@START_DATE
			,[END_DATE]		=@END_DATE
			,[CONDITION]	=@CONDITION
		FROM analysis A
		WHERE [TITLE] = @TITLE

		PRINT N'WARNNING::�ߺ��� �����Դϴ�.'
	END
END;;

--------------------------------------------------------------
-- 2. �����ͺ��̽� ����
--------------------------------------------------------------
PRINT N'�����ͺ��̽� ����'
IF NOT EXISTS (select * from sys.databases where name=@SCHEMA_NAME)
BEGIN
	CREATE DATABASE Analysis_Dupe
END

--------------------------------------------------------------
-- 3. ��Ű�� ����.
--------------------------------------------------------------
PRINT N'��Ű�� ����'
IF NOT EXISTS (SELECT NAME FROM Analysis_Dupe.SYS.TABLES WHERE NAME = N'BUG')
BEGIN
	PRINT N'ERROR :: ��Ű���� �������ּ���. A'
	RETURN
END
ELSE IF NOT EXISTS (SELECT NAME FROM Analysis_Dupe.SYS.TABLES WHERE NAME = N'TERM')
BEGIN
	PRINT N'ERROR :: ��Ű���� �������ּ���. B'
	RETURN
END  
--mssql_unified.sql �ܺ� ������ ����.  (�м��� �������.)
--mssql_unified_TF.sql �ܺ� ������ ����.  (TF�� �������.)


--------------------------------------------------------------
-- 4. �̵� �Լ� ����.
--------------------------------------------------------------
PRINT N'������ �̵�'
IF NOT EXISTS (SELECT NAME FROM SYS.procedures WHERE NAME = N'moveAnalysis')
BEGIN
	PRINT N'ERROR :: �̵��� ���� ���ν����� �����ϴ�.'
	RETURN
END

--�м��������� �̵�.
exec moveAnalysis @SCHEMA_NAME, @SITE_ID, @PROJECT_ID, @START_DATE, @END_DATE, @CONDITION





--------------------------------------------------------------
-- 5. ������� ������Ʈ
--------------------------------------------------------------

use UniBAS
GO

PRINT N'�м��ν��Ͻ� ���� ����'
--select * from analysis
--select * from analysis_history
--select * from analysis_project
--select * from analysis_query
DECLARE @ANALYSIS_ID	AS integer			= 3
DECLARE @TITLE			AS NVARCHAR(256)	= N'DuplicateTest'
DECLARE @DESCRIPTION	AS NVARCHAR(256)	= N'-----------'
DECLARE @SCHEMA_NAME	AS NVARCHAR(256)	= N'Analysis_Dupe'
DECLARE @SITE_ID		AS integer			= 6
DECLARE @PROJECT_ID		AS NVARCHAR(MAX)	= N'7,15,16'	-- N'1,7,8,9,10,11'
DECLARE @IS_UNIFORMLY	AS BIT				= 1
DECLARE @START_DATE		AS DATETIME			= null
DECLARE @END_DATE		AS DATETIME			= null
DECLARE @CONDITION		AS NVARCHAR(MAX)	= null


--������� ����
EXEC makeAnalysisSummary @analysis_id, @SCHEMA_NAME, @Site_id, @project_id		

--�� �ν��Ͻ��� �������� �����丮�� ����.
INSERT INTO analysis_history VALUES (@ANALYSIS_ID, 1, SYSDATETIME(), 'analysis', 'id',null,'NEW')

select * from analysis_history
select * from analysis_project
select * from analysis


--------------------------------------------------------------
-- 6. �ʵ� ����
--------------------------------------------------------------
--������ �ϴ°�����


select top 100 site_id, id, severity from bug order by id

use UniBAS
select * from bug
select * from analysis
--delete from analysis where id = 4
--DBCC CHECKIDENT('analysis', RESEED,3)


--INSERT INTO analysis VALUES ('AnalysisTest','','Analysis_Bug1',1,'2001-01-01', '2003-12-31','comment.count(*) > 10'),
--('AnalysisTest2','','Analysis_Bug2',1,null,null,null),
--('DuplicateTest','','Analysis_Dupe',1,null,null,null)


select id from analysis_Dupe2..bug
select id from analysis_Dupe2..project
select id from analysis_Dupe2..comment
select id from analysis_Dupe2..bug


--------------------------------------------------------------
-- 7. NLP ó�� (���̽� NLTK �̿�)
--------------------------------------------------------------
--���̽� ���α׷� ����.


use Analysis_dupe2
select * from stopword
select * from term
select * from element_term
select * from tf_mode
--delete from stopword
--delete from term
--delete from element_term
--delete from tf_mode
--DBCC CHECKIDENT('tf_mode', RESEED,0)
--DBCC CHECKIDENT('stopword', RESEED,0)
select COUNT(*) from stopword
select COUNT(*) from term
select COUNT(*) from element_term
select COUNT(*) from tf_mode

select * from element_term where mode_id = 1 and site_id = 6 and bug_id = 19118



select * from [site]

