--------------------------------------------------
--  타겟 BS확인 쿼리
--------------------------------------------------
use Bug_infoDB

select id, iname, sname, base_url, start_ts, end_ts,[status],create_ts, delta_ts
--delete
from target_infos
where id <=1171
--where id in (131)
--where id >=2178
--where sname in ( 'Mantis_Mantis','Mantis_Scribus','Mantis_Sylohe','Mantis_Phplist')
--where id>=1160 and id <=1170
--where id in (1154,1146,1147,1148,1149,1150,1151,1152,1153)


--identity 값 재설정
--DBCC CHECKIDENT ('target_info', RESEED, 55);



--------------------------------------------------
--  데이터 베이스 삭제
--------------------------------------------------

--drop database Bug_Mantis
--drop database Mantis_Mantis
--drop database Bug_Scribus
--drop database Mantis_Scribus
--drop database Bug_Phplist
--drop database Mantis_Phplist
--drop database Bug_pikatech
--drop database Mantis_pikatech

--drop database Bug_Sylohe
--drop database Mantis_Sylohe
--drop database Bug_Thomas
--drop database Mantis_Thomas
--drop database Bug_kdenlive
--drop database Mantis_kdenlive

--drop database Bug_Mantis1
--drop database Mantis_Mantis1
--drop database Bug_Mantis2
--drop database Mantis_Mantis2
--drop database Bug_Mantis3
--drop database Mantis_Mantis3
--drop database Bug_Scribus1
--drop database Mantis_Scribus1
--drop database Bug_Phplist1
--drop database Mantis_Phplist1
--drop database Bug_Mozilla
--drop database Bugzilla_Mozilla
--drop database Bug_MozillaZ


--drop database Bug_Mantis2
--drop database Mantis_Mantis2
--drop database Bug_Test1
--drop database Bug_Test2
--drop database Bug_Test3
--drop database Bug_Test4
--drop database Bug_Test5
--drop database Bug_Test6
--drop database Bug_Test7
--drop database Bug_Test8
--drop database Bug_Test9
--drop database Bugzilla_Test1
--drop database Bugzilla_Test2
--drop database Bugzilla_Test3
--drop database Bugzilla_Test4
--drop database Bugzilla_Test5
--drop database Bugzilla_Test6
--drop database Bugzilla_Test7
--drop database Bugzilla_Test8
--drop database Bugzilla_Test9



--------------------------------------------------
--  데이터 베이스 임시로 값 추가.
--------------------------------------------------
--insert into target_info values ('Bug_Moailla9', '', 'Bugzilla', 'https://bugzilla.mozilla.org/', 'forglee@naver.com', 'Sel535447', 'E:\_Temp\BTS\log_Mozilla9.txt', '2014-04-08 10:23:49', '2014-04-09 08:29:00', '2010-01-01','2010-07-01', 'Done')



