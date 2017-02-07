--------------------------------------------------------------------------------------
-- Initial database creation
-- CREATE USER wwwuser WITH PASSWORD <password>;
-- CREATE USER scorekeeper WITH PASSWORD <password>;
-- GRANT scorekeeper TO wwwuser;
-- CREATE DATABASE scorekeeper WITH OWNER scorekeeper;

-- As superuser when creating a new series
-- CREATE USER <seriesname> PASSWORD <password>;
-- CREATE SCHEMA <seriesname> AUTHORIZATION <seriesname>;
-- GRANT scorekeeper to <seriesname>;
-- GRANT <seriesname> to wwwuser;

--------------------------------------------------------------------------------------
--- Create top level results and drivers tables that every series shares

-- Logs are specific to this machine
CREATE TABLE driverslog (
    logid   BIGSERIAL PRIMARY KEY,
    usern   TEXT      NOT NULL,
    time    TIMESTAMP NOT NULL,
    addr    INET      NOT NULL,
    query   TEXT      NOT NULL,
    action  CHAR(1)   NOT NULL CHECK (action IN ('I', 'D', 'U')),
    rowdata JSONB     NOT NULL,
    changed JSONB     NOT NULL
);
REVOKE ALL ON driverslog FROM public;
CREATE INDEX ON driverslog(logid);
CREATE INDEX ON driverslog(time);
COMMENT ON TABLE driverslog IS 'Change logs that are specific to this local database';
 

CREATE OR REPLACE FUNCTION logdrivermods() RETURNS TRIGGER AS $body$
DECLARE
    audit_row driverslog;
BEGIN
    audit_row = ROW(
        NULL, session_user::text, CURRENT_TIMESTAMP,
        inet_client_addr(), current_query(), SUBSTRING(TG_OP,1,1), '{}', '{}'
    );
 
    IF (TG_OP = 'UPDATE') THEN
        IF OLD = NEW THEN
            RETURN NULL;
        END IF;
        audit_row.rowdata = to_jsonb(OLD.*);
        audit_row.changed = hstore_to_jsonb(hstore(NEW) - hstore(OLD));
    ELSIF (TG_OP = 'DELETE') THEN
        audit_row.rowdata = to_jsonb(OLD.*);
    ELSIF (TG_OP = 'INSERT') THEN
        audit_row.rowdata = to_jsonb(NEW.*);
    ELSE
        RETURN NULL;
    END IF;

    audit_row.logid = NEXTVAL('driverslog_logid_seq');
    INSERT INTO driverslog VALUES (audit_row.*);
    RETURN NULL;
END;
$body$
LANGUAGE plpgsql;
COMMENT ON FUNCTION logdrivermods() IS 'Function to log details of any insert, delete or update on the drivers table';


-- The results table acts as a storage of calculated results for each series, it is also the location where
-- we archive old series into a compacted form where so can delete old unused drivers but maintain the basic data
-- Name values are 'series'=eventlist+settings+series champresults, e#=eventid results, a#=eventid announcer data

CREATE TABLE results (
    series     TEXT        NOT NULL,
    name       TEXT        NOT NULL,
    data       JSONB       NOT NULL,
    modified   TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (series, name)
);
REVOKE ALL ON results FROM public;
-- Everyone can view results but only owner can insert, update, delete their rows
ALTER TABLE results ENABLE ROW LEVEL SECURITY;
CREATE POLICY all_view ON results FOR SELECT USING (true);
CREATE POLICY own_mod1 ON results FOR INSERT WITH CHECK (series = current_user);
CREATE POLICY own_mod2 ON results FOR UPDATE USING (series = current_user);
CREATE POLICY own_mod3 ON results FOR DELETE USING (series = current_user);
COMMENT ON TABLE results IS 'The stored list of JSON data represent event results, champ results and series related display settings';


-- attr includes alias, address, city, state, zip, phone, brag, sponsor, emergency, notes, etc
CREATE TABLE drivers (
    driverid   UUID        PRIMARY KEY, 
    firstname  TEXT        NOT NULL, 
    lastname   TEXT        NOT NULL, 
    email      TEXT        NOT NULL,
	password   TEXT        NOT NULL,
    membership TEXT        NOT NULL,
    attr       JSONB       NOT NULL, 
    modified   TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX ON drivers(lower(firstname));
CREATE INDEX ON drivers(lower(lastname));
REVOKE ALL   ON drivers FROM public;
CREATE TRIGGER driversmod AFTER INSERT OR UPDATE OR DELETE ON drivers FOR EACH ROW EXECUTE PROCEDURE logdrivermods();
COMMENT ON TABLE drivers IS 'The global list of drivers for all series';


