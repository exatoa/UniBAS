-------------------------------------------------------------------------------------------
-- 설정
-------------------------------------------------------------------------------------------
--이동을 원하는 타겟 지정
--마이그레이션 함수들이 생성되어있어야함.
use Bugzilla_mozilla
DECLARE @site as NVARCHAR(256) = N'bugzilla_mozilla'

-------------------------------------------------------------------------------------------
-- 실행코드
-------------------------------------------------------------------------------------------
IF NOT EXISTS(select * from sys.objects where type='P' and name like 'Migration%')
BEGIN
	 RaisError ('There is no Migration procedures', 15, 10)
	 return
END

--이동을 원하는 타겟 지정.
DECLARE @site_id as integer
SELECT @site_id = id from UniBAS..[site] where [schema_name] = @site
PRINT @site_id

--마이그레이션 함수들 실행
exec Migration_bug @site_id
exec Migration_comment @site_id
exec Migration_attachment @site_id
exec Migration_attach_data @site_id
exec Migration_History @site_id
exec Migration_relationship @site_id
exec Migration_reference @site_id
exec Migration_monitor @site_id
exec Migration_user @site_id
exec Migration_project @site_id
exec Migration_component @site_id
exec Migration_keyword @site_id
exec Migration_bug_keyword @site_id
exec Migration_additional_info @site_id
exec Migration_status @site_id
exec Migration_resolution @site_id
exec Migration_severity @site_id
exec Migration_priority @site_id
exec Migration_platform @site_id
exec Migration_os @site_id
exec Migration_version @site_id
exec Migration_milestone @site_id

--통계정보 업데이트.
exec UniBAS..makeSummary @site_id

--상태 정보 업데이트.
UPDATE s SET [status] = 'COMPLETED' FROM UniBAS..[SITE] s WHERE id = @site_id
UPDATE s SET [status] = 'COMPLETED' FROM UniBAS..[site_summary] s WHERE site_id = @site_id




------------------------------------------------------------------------------------------
--  마이그레이션 프로시저 모두 삭제;
------------------------------------------------------------------------------------------
Declare @resCnt as integer
Declare @sql as nvarchar(max)
Declare @params as nvarchar(256)= '@resCnt as int OUTPUT'	--OUTPUT에 주의
DECLARE @pname AS NVARCHAR(256)
DECLARE MIG_PROG CURSOR for select name from sys.objects where type='P' and name like N'Migration%'

OPEN MIG_PROG
FETCH NEXT FROM MIG_PROG INTO @pname

--삭제
WHILE (@@FETCH_STATUS = 0)
BEGIN
	SET @sql = 'drop procedure '+@pname
	EXEC sp_executesql @sql, @params, @resCnt OUTPUT
	
	FETCH NEXT FROM MIG_PROG INTO @pname
END

CLOSE MIG_PROG
DEALLOCATE MIG_PROG

