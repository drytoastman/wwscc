
CREATE USER <seriesname> PASSWORD '<seriesname>';
CREATE SCHEMA <seriesname> AUTHORIZATION <seriesname>;
GRANT scorekeeper to <seriesname>;
GRANT <seriesname> to wwwuser;
SET search_path='<seriesname>','public';


CREATE TABLE serieslog (
    logid   BIGSERIAL PRIMARY KEY,
    tablen  TEXT      NOT NULL,
    usern   TEXT      NOT NULL,
    time    TIMESTAMP NOT NULL,
    addr    INET      NOT NULL,
    query   TEXT      NOT NULL,
    action  CHAR(1)   NOT NULL CHECK (action IN ('I', 'D', 'U')),
    rowdata JSONB     NOT NULL,
    changed JSONB     NOT NULL
);
REVOKE ALL ON serieslog FROM public;
GRANT  ALL ON serieslog TO <seriesname>;
GRANT  ALL ON serieslog_logid_seq TO scorekeeper;
CREATE INDEX ON serieslog(logid);
CREATE INDEX ON serieslog(time);
COMMENT ON TABLE serieslog is 'Change logs that are specific to this local database';
 

CREATE OR REPLACE FUNCTION logseriesmods() RETURNS TRIGGER AS $body$
DECLARE
    audit_row serieslog;
    changes hstore;
BEGIN
    audit_row := ROW(
        NULL, TG_TABLE_NAME::text, session_user::text, CURRENT_TIMESTAMP,
        inet_client_addr(), current_query(), SUBSTRING(TG_OP,1,1), '{}', '{}'
    );
 
    IF (TG_OP = 'UPDATE') THEN
        changes := hstore(NEW) - hstore(OLD);
        IF akeys(changes) = ARRAY['modified'] THEN
            RETURN NULL;
        END IF;

        audit_row.changed := hstore_to_jsonb(changes);
        audit_row.rowdata := to_jsonb(OLD.*);
    ELSIF (TG_OP = 'DELETE') THEN
        audit_row.rowdata := to_jsonb(OLD.*);
    ELSIF (TG_OP = 'INSERT') THEN
        audit_row.rowdata := to_jsonb(NEW.*);
    ELSE
        RETURN NULL;
    END IF;

    audit_row.logid := NEXTVAL('serieslog_logid_seq');
    INSERT INTO serieslog VALUES (audit_row.*);
    RETURN NULL;
END;
$body$
LANGUAGE plpgsql;


CREATE TABLE settings (
    name       VARCHAR     NOT NULL,
    val        VARCHAR     NOT NULL,
    modified   TIMESTAMP   NOT NULL DEFAULT now()
);
REVOKE ALL ON settings FROM public;
GRANT  ALL ON settings TO <seriesname>;
CREATE TRIGGER settingsmod AFTER INSERT OR UPDATE OR DELETE ON settings FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE settings IS 'settings includes any boolean, integer, double preferences for the series, sql keeps us in string format';


CREATE TABLE indexlist (
    indexcode   VARCHAR(16)  PRIMARY KEY,
    descrip     TEXT         NOT NULL, 
    value       FLOAT        NOT NULL,
    modified    TIMESTAMP    NOT NULL DEFAULT now()
);
REVOKE ALL ON indexlist FROM public;
GRANT  ALL ON indexlist TO <seriesname>;
CREATE TRIGGER indexmod AFTER INSERT OR UPDATE OR DELETE ON indexlist FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE indexlist IS 'The list of indexes for this series';


CREATE TABLE classlist (
    classcode       VARCHAR(16)  PRIMARY KEY,
    descrip         TEXT         NOT NULL, 
    indexcode       VARCHAR(16)  NOT NULL REFERENCES indexlist, 
    caridxrestrict  TEXT         NOT NULL,
    classmultiplier FLOAT        NOT NULL, 
    carindexed      BOOLEAN      NOT NULL, 
    usecarflag      BOOLEAN      NOT NULL, 
    eventtrophy     BOOLEAN      NOT NULL, 
    champtrophy     BOOLEAN      NOT NULL, 
    numorder        INTEGER      NOT NULL, 
    countedruns     INTEGER      NOT NULL,
    modified        TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX ON classlist(classcode);
REVOKE ALL ON classlist FROM public;
GRANT  ALL ON classlist TO <seriesname>;
CREATE TRIGGER classmod AFTER INSERT OR UPDATE OR DELETE ON classlist FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE classlist IS 'The list of classes for this series';


CREATE TABLE events (
    eventid       SERIAL      PRIMARY KEY, 
    name          TEXT        NOT NULL, 
    date          DATE        NOT NULL, 
    regopened     TIMESTAMP   NOT NULL DEFAULT now(), 
    regclosed     TIMESTAMP   NOT NULL, 
    courses       INTEGER     NOT NULL DEFAULT 1, 
    runs          INTEGER     NOT NULL DEFAULT 4, 
    countedruns   INTEGER     NOT NULL DEFAULT 0, 
    perlimit      INTEGER     NOT NULL DEFAULT 0, 
    totlimit      INTEGER     NOT NULL DEFAULT 0, 
    conepen       FLOAT       NOT NULL DEFAULT 2.0, 
    gatepen       FLOAT       NOT NULL DEFAULT 10.0, 
    ispro         BOOLEAN     NOT NULL DEFAULT FALSE, 
    ispractice    BOOLEAN     NOT NULL DEFAULT FALSE, 
    attr          JSONB       NOT NULL,
    modified      TIMESTAMP   NOT NULL DEFAULT now()
);
REVOKE ALL ON events FROM public;
GRANT  ALL ON events TO <seriesname>;
CREATE TRIGGER eventmod AFTER INSERT OR UPDATE OR DELETE ON events FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE events IS 'The list of events for this series, attr includes location, sponsor, host, chair, designer, segments, paypal, snail, cost, notes, doublespecial, etc';


CREATE TABLE cars (
    carid         UUID        PRIMARY KEY,
    driverid      UUID        NOT NULL REFERENCES public.drivers, 
    classcode     VARCHAR(16) NOT NULL REFERENCES classlist, 
    indexcode     VARCHAR(16) NOT NULL REFERENCES indexlist, 
    number        INTEGER     NOT NULL, 
    attr          JSONB       NOT NULL,
    modified      TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX ON cars(driverid);
CREATE INDEX ON cars(classcode);
CREATE INDEX ON cars(indexcode);
REVOKE ALL ON cars FROM public;
GRANT  ALL ON cars TO <seriesname>;
CREATE TRIGGER carmod AFTER INSERT OR UPDATE OR DELETE ON cars FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE cars IS 'The cars in this series.  Attr includes year, make, model, color, tireindexed';


CREATE TABLE runs (
    eventid  INTEGER    NOT NULL REFERENCES events, 
    carid    UUID       NOT NULL REFERENCES cars, 
    course   INTEGER    NOT NULL, 
    run      INTEGER    NOT NULL, 
    cones    INTEGER    NOT NULL DEFAULT 0, 
    gates    INTEGER    NOT NULL DEFAULT 0, 
    raw      FLOAT      NOT NULL, 
    status   VARCHAR(8) NOT NULL DEFAULT 'DNS', 
    attr     JSONB      NOT NULL,
    modified TIMESTAMP  NOT NULL DEFAULT now(),
    PRIMARY KEY (eventid, carid, course, run)
);
CREATE INDEX ON runs(eventid);
REVOKE ALL ON runs FROM public;
GRANT  ALL ON runs TO <seriesname>;
CREATE TRIGGER runmod AFTER INSERT OR UPDATE OR DELETE ON runs FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE runs IS 'The runs in this series. Attr includes reaction, sixty, segments[n] (What about net, ordering values?)';


CREATE TABLE registered (
    eventid  INTEGER    NOT NULL REFERENCES events, 
    carid    UUID       NOT NULL REFERENCES cars, 
    paid     BOOLEAN    NOT NULL DEFAULT FALSE,
    modified TIMESTAMP  NOT NULL DEFAULT now(),
    PRIMARY KEY (eventid, carid)
);
CREATE INDEX ON registered(eventid);
REVOKE ALL ON registered FROM public;
GRANT  ALL ON registered TO <seriesname>;
CREATE TRIGGER regmod AFTER INSERT OR UPDATE OR DELETE ON registered FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE registered IS 'The list of cars registered for events';


CREATE TABLE runorder (
    eventid  INTEGER    NOT NULL REFERENCES events, 
    course   INTEGER    NOT NULL, 
    rungroup INTEGER    NOT NULL, 
    row      INTEGER    NOT NULL, 
    carid    UUID       NOT NULL REFERENCES cars, 
    modified TIMESTAMP  NOT NULL DEFAULT now(),
    PRIMARY KEY (eventid, course, rungroup, row),
    CONSTRAINT oneentrypercourse UNIQUE (eventid, course, carid) DEFERRABLE INITIALLY DEFERRED
);
CREATE INDEX getgroup ON runorder(eventid, course, rungroup);
REVOKE ALL ON runorder FROM public;
GRANT  ALL ON runorder TO <seriesname>;
CREATE TRIGGER ordermod AFTER INSERT OR UPDATE OR DELETE ON runorder FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE runorder IS 'the ordering of cars in each runorder';


CREATE TABLE payments (
    txid     TEXT        PRIMARY KEY,
    date     DATE        NOT NULL, 
    type     TEXT        NOT NULL, 
    status   TEXT        NOT NULL, 
    driverid UUID        NOT NULL REFERENCES public.drivers, 
    eventid  INTEGER     NOT NULL REFERENCES events, 
    amount   FLOAT       NOT NULL,
    modified TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX ON payments(driverid);
CREATE INDEX ON payments(eventid);
REVOKE ALL ON payments FROM public;
GRANT  ALL ON payments TO <seriesname>;
CREATE TRIGGER paymentmod AFTER INSERT OR UPDATE OR DELETE ON payments FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE payments IS 'the payments that have been made online';


CREATE TABLE classorder (
    eventid   INTEGER     NOT NULL REFERENCES events, 
    classcode VARCHAR(16) NOT NULL REFERENCES classlist, 
    rungroup  INTEGER     NOT NULL, 
    gorder    INTEGER     NOT NULL, 
    modified  TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (eventid, classcode, rungroup)
);
CREATE INDEX ON classorder(eventid);
CREATE INDEX ON classorder(classcode);
REVOKE ALL ON classorder FROM public;
GRANT  ALL ON classorder TO <seriesname>;
CREATE TRIGGER classordermod AFTER INSERT OR UPDATE OR DELETE ON classorder FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE classorder IS 'the ordering of classes in a runorder, generally only used in the Pro event for grid ordering';


CREATE TABLE challenges (
    challengeid SERIAL      PRIMARY KEY,
    eventid     INTEGER     NOT NULL REFERENCES events, 
    name        TEXT        NOT NULL, 
    depth       INTEGER     NOT NULL,
    modified    TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX ON challenges(eventid);
REVOKE ALL ON challenges FROM public;
GRANT  ALL ON challenges TO <seriesname>;
CREATE TRIGGER challengemod AFTER INSERT OR UPDATE OR DELETE ON challenges FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE challenges is 'The list of challenges for each ProSolo event';


CREATE TABLE challengerounds (
    challengeid  INTEGER   NOT NULL REFERENCES challenges, 
    round        INTEGER   NOT NULL,
    swappedstart BOOLEAN   NOT NULL DEFAULT FALSE, 
    car1id       UUID      REFERENCES cars(carid), 
    car1dial     FLOAT     NOT NULL, 
    car2id       UUID      REFERENCES cars(carid), 
    car2dial     FLOAT     NOT NULL, 
    modified     TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (challengeid, round)
);
CREATE INDEX ON challengerounds(challengeid);
REVOKE ALL ON challengerounds FROM public;
GRANT  ALL ON challengerounds TO <seriesname>;
CREATE TRIGGER roundmod AFTER INSERT OR UPDATE OR DELETE ON challengerounds FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE challengerounds IS 'the list of rounds (carids, input dialin, etc) for each challenge, carid can be null';


CREATE TABLE challengeruns (
    challengeid INTEGER    NOT NULL REFERENCES challenges,
    round       INTEGER    NOT NULL,
    carid       UUID       NOT NULL REFERENCES cars,
    course      INTEGER    NOT NULL,
    reaction    FLOAT      NOT NULL,
    sixty       FLOAT      NOT NULL,
    raw         FLOAT      NOT NULL,
    cones       INTEGER    NOT NULL DEFAULT 0,
    gates       INTEGER    NOT NULL DEFAULT 0,
    status      VARCHAR(8) NOT NULL DEFAULT 'OK',
    modified    TIMESTAMP  NOT NULL DEFAULT now(),
    PRIMARY KEY (challengeid, round, carid, course),
    FOREIGN KEY (challengeid, round) REFERENCES challengerounds
);
CREATE INDEX ON challengeruns(carid);
REVOKE ALL ON challengeruns FROM public;
GRANT  ALL ON challengeruns TO <seriesname>;
CREATE TRIGGER crunmod AFTER INSERT OR UPDATE OR DELETE ON challengeruns FOR EACH ROW EXECUTE PROCEDURE logseriesmods();
COMMENT ON TABLE challengeruns IS 'the list of runs for a challenge in a ProSolo, different from regular runs table';

