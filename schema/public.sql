--------------------------------------------------------------------------------------
--- Create top level results and drivers tables that every series shares

CREATE ROLE baseaccess;
CREATE USER seriesview PASSWORD 'seriesview';
CREATE USER wwwuser PASSWORD '<wwwpassword>';
GRANT  baseaccess TO wwwuser;
REVOKE ALL ON SCHEMA public FROM public;
GRANT  ALL ON SCHEMA public TO baseaccess;
CREATE EXTENSION hstore;

-- Logs are specific to this machine
CREATE TABLE driverslog (
    logid   BIGSERIAL PRIMARY KEY,
    usern   TEXT      NOT NULL,
    time    TIMESTAMP NOT NULL,
    addr    INET      NOT NULL,
    action  CHAR(1)   NOT NULL CHECK (action IN ('I', 'D', 'U')),
    rowdata JSONB     NOT NULL,
    changed JSONB     NOT NULL
);
REVOKE ALL ON driverslog FROM public;
GRANT  ALL ON driverslog TO baseaccess;
GRANT  ALL ON driverslog_logid_seq TO baseaccess;
CREATE INDEX ON driverslog(logid);
CREATE INDEX ON driverslog(time);
COMMENT ON TABLE driverslog IS 'Change logs that are specific to this local database';


CREATE OR REPLACE FUNCTION logdrivermods() RETURNS TRIGGER AS $body$
DECLARE
    audit_row driverslog;
BEGIN
    audit_row = ROW(NULL, session_user::text, CURRENT_TIMESTAMP, inet_client_addr(), SUBSTRING(TG_OP,1,1), '{}', '{}');

    IF (TG_OP = 'UPDATE') THEN
        IF OLD = NEW THEN
            RETURN NULL;
        END IF;
        audit_row.changed := hstore_to_jsonb(hstore(NEW) - hstore(OLD));
        audit_row.rowdata := to_jsonb(OLD.*);
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


CREATE OR REPLACE FUNCTION ignoreunmodified() RETURNS TRIGGER AS $body$
DECLARE
BEGIN
    IF (TG_OP = 'UPDATE') THEN
        IF (OLD = NEW) THEN
            RETURN NULL;
        END IF;
        IF akeys(hstore(NEW) - hstore(OLD)) = ARRAY['modified'] THEN
            RETURN NULL;
        END IF;
    END IF;
    RETURN NEW;
END;
$body$
LANGUAGE plpgsql;
COMMENT ON FUNCTION ignoreunmodified() IS 'does not update rows if only change is the modified field or less';


-- The results table acts as a storage of calculated results for each series, it is also the location where
-- we archive old series into a compacted form where so can delete old unused drivers but maintain the basic data
-- Name values are champ=champ results, e#=eventid results, a#=eventid announcer data

CREATE TABLE results (
    series     TEXT        NOT NULL,
    name       TEXT        NOT NULL,
    data       JSONB       NOT NULL,
    modified   TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (series, name)
);
REVOKE ALL ON results FROM public;
GRANT  ALL ON results TO baseaccess;
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
GRANT  ALL   ON drivers TO baseaccess;
CREATE TRIGGER driversmod AFTER INSERT OR UPDATE OR DELETE ON drivers FOR EACH ROW EXECUTE PROCEDURE logdrivermods();
CREATE TRIGGER driversuni BEFORE UPDATE ON drivers FOR EACH ROW EXECUTE PROCEDURE ignoreunmodified();
COMMENT ON TABLE drivers IS 'The global list of drivers for all series';


