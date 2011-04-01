CREATE TABLE EVENT (
    event_id int PRIMARY KEY,
    event_type varchar(50) NOT NULL,
    server_id varchar(50),
    event_user varchar(50),
    event_time timestamp);

CREATE TABLE EVENT_PROP (
    event_prop_id int PRIMARY KEY,
    event_id NOT NULL FOREIGN KEY REFERENCES EVENT(event_id),
    prop_key varchar(32) NOT NULL,
    prop_value varchar(250));

CREATE SEQUENCE seq_event
    MINVALUE 1
    START WITH 1
    INCREMENT BY 1
    CACHE 10;

CREATE SEQUENCE seq_event_prop
    MINVALUE 1
    START WITH 1
    INCREMENT BY 1
    CACHE 10;
