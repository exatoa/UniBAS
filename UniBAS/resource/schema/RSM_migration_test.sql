-------------------------------------------------------------------------------------------
-- ����
-------------------------------------------------------------------------------------------
--�̵��� ���ϴ� Ÿ�� ����
--���̱׷��̼� �Լ����� �����Ǿ��־����.
use Bugzilla_mozilla
DECLARE @site as NVARCHAR(256) = N'bugzilla_mozilla'

-------------------------------------------------------------------------------------------
-- �����ڵ�
-------------------------------------------------------------------------------------------
IF NOT EXISTS(select * from sys.objects where type='P' and name like 'Migration%')
BEGIN
	 RaisError ('There is no Migration procedures', 15, 10)
	 return
END

--�̵��� ���ϴ� Ÿ�� ����.
DECLARE @site_id as integer
SELECT @site_id = id from UniBAS..[site] where [schema_name] = @site
PRINT @site_id

--���̱׷��̼� �Լ��� ����
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

--������� ������Ʈ.
exec UniBAS..makeSummary @site_id

--���� ���� ������Ʈ.
UPDATE s SET [status] = 'COMPLETED' FROM UniBAS..[SITE] s WHERE id = @site_id
UPDATE s SET [status] = 'COMPLETED' FROM UniBAS..[site_summary] s WHERE site_id = @site_id




------------------------------------------------------------------------------------------
--  ���̱׷��̼� ���ν��� ��� ����;
------------------------------------------------------------------------------------------
Declare @resCnt as integer
Declare @sql as nvarchar(max)
Declare @params as nvarchar(256)= '@resCnt as int OUTPUT'	--OUTPUT�� ����
DECLARE @pname AS NVARCHAR(256)
DECLARE MIG_PROG CURSOR for select name from sys.objects where type='P' and name like N'Migration%'

OPEN MIG_PROG
FETCH NEXT FROM MIG_PROG INTO @pname

--����
WHILE (@@FETCH_STATUS = 0)
BEGIN
	SET @sql = 'drop procedure '+@pname
	EXEC sp_executesql @sql, @params, @resCnt OUTPUT
	
	FETCH NEXT FROM MIG_PROG INTO @pname
END

CLOSE MIG_PROG
DEALLOCATE MIG_PROG

