check.schmea = select count(*) from EVENT;
insert.event = insert into EVENT (event_id, event_type, server_id, event_user, event_time) values (seq_event.nextval, ?, ?, ?, ?);
insert.event_prop = insert into EVENT_PROP (event_prop_id, event_id, prop_key, prop_value) values (seq_event_prop.nextval, ?, ?, ?);