check.schmea = select count(*) from EVENT
insert.event = insert into EVENT (event_type, server_id, event_user, event_time) values (?, ?, ?, ?)
insert.event_prop = insert into EVENT_PROP (event_id, prop_key, prop_value) values (?, ?, ?)
