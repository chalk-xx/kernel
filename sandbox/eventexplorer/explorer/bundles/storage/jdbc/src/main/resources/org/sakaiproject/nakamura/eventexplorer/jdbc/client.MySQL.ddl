CREATE TABLE EVENT (
    `event_id` int NOT NULL AUTO_INCREMENT,
    `event_type` varchar(50) NOT NULL,
    `server_id` varchar(50),
    `event_user` varchar(50),
    `event_time` timestamp NOT NULL,
    PRIMARY KEY (eventId));

CREATE TABLE EVENT_PROP (
    `event_prop_id` int NOT NULL AUTO_INCREMENT,
    `event_id` int NOT NULL,
    `prop_key` varchar(32) NOT NULL,
    `prop_value` varchar(500),
    PRIMARY KEY (event_prop_id),
    FOREIGN KEY(event_id) REFERENCES EVENT);
