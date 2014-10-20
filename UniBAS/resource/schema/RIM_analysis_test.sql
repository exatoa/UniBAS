--drop database Analaysis_bug1
--select * from analysis
--delete from analysis
--DBCC CHECKIDENT('analysis', RESEED, 0)
use Analysis_Dupe
use UniBAS
GO
--------------------------------------------------------------
-- 1. 분석인스턴스 생성
--------------------------------------------------------------
PRINT N'분석인스턴스 정보 생성'
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

		PRINT N'WARNNING::중복된 정보입니다.'
	END
END;;

--------------------------------------------------------------
-- 2. 데이터베이스 생성
--------------------------------------------------------------
PRINT N'데이터베이스 생성'
IF NOT EXISTS (select * from sys.databases where name=@SCHEMA_NAME)
BEGIN
	CREATE DATABASE Analysis_Dupe
END

--------------------------------------------------------------
-- 3. 스키마 생성.
--------------------------------------------------------------
PRINT N'스키마 생성'
IF NOT EXISTS (SELECT NAME FROM Analysis_Dupe.SYS.TABLES WHERE NAME = N'BUG')
BEGIN
	PRINT N'ERROR :: 스키마를 생성해주세요. A'
	RETURN
END
ELSE IF NOT EXISTS (SELECT NAME FROM Analysis_Dupe.SYS.TABLES WHERE NAME = N'TERM')
BEGIN
	PRINT N'ERROR :: 스키마를 생성해주세요. B'
	RETURN
END  
--mssql_unified.sql 외부 파일을 실행.  (분석모델 담겨있음.)
--mssql_unified_TF.sql 외부 파일을 실행.  (TF모델 담겨있음.)


--------------------------------------------------------------
-- 4. 이동 함수 실행.
--------------------------------------------------------------
PRINT N'데이터 이동'
IF NOT EXISTS (SELECT NAME FROM SYS.procedures WHERE NAME = N'moveAnalysis')
BEGIN
	PRINT N'ERROR :: 이동을 위한 프로시저가 없습니다.'
	RETURN
END

--분석데이터의 이동.
exec moveAnalysis @SCHEMA_NAME, @SITE_ID, @PROJECT_ID, @START_DATE, @END_DATE, @CONDITION





--------------------------------------------------------------
-- 5. 통계정보 업데이트
--------------------------------------------------------------

use UniBAS
GO

PRINT N'분석인스턴스 정보 생성'
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


--요약정보 생성
EXEC makeAnalysisSummary @analysis_id, @SCHEMA_NAME, @Site_id, @project_id		

--새 인스턴스가 생겼음을 히스토리에 남김.
INSERT INTO analysis_history VALUES (@ANALYSIS_ID, 1, SYSDATETIME(), 'analysis', 'id',null,'NEW')

select * from analysis_history
select * from analysis_project
select * from analysis


--------------------------------------------------------------
-- 6. 필드 매핑
--------------------------------------------------------------
--다음에 하는것으로


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
-- 7. NLP 처리 (파이썬 NLTK 이용)
--------------------------------------------------------------
--파이썬 프로그램 실행.


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

