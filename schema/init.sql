--------------------------------------------------------------------------------------
-- Create the initial scorekeeper and wwwuser roles and the scorekeeper database

CREATE USER scorekeeper PASSWORD 'scorekeeper';
CREATE USER wwwuser PASSWORD 'wwwuser';
GRANT  scorekeeper TO wwwuser;
CREATE DATABASE scorekeeper WITH OWNER scorekeeper;
