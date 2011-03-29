CREATE TABLE EVENT (
    event_id integer NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    event_type varchar(50) NOT NULL,
    server_id varchar(50),
    event_user varchar(50),
    event_time timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE EVENT_PROP (
    event_prop_id integer NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    event_id integer NOT NULL,
    prop_key varchar(32) NOT NULL,
    prop_value varchar(500),
    PRIMARY KEY (event_prop_id),
    FOREIGN KEY (event_id) REFERENCES EVENT(event_id));
