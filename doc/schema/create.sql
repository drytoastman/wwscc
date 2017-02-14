-- initdb.exe -D pgdb -U postgres
-- pg_ctl.exe -D pgdb start

CREATE USER scorekeeper PASSWORD 'scorekeeper';
CREATE USER wwwuser PASSWORD 'wwwuser';
GRANT  scorekeeper TO wwwuser;
CREATE DATABASE scorekeeper WITH OWNER scorekeeper;

