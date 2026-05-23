create table v2_schema_marker (
    marker_key varchar(64) primary key,
    created_at timestamp not null default current_timestamp
);

insert into v2_schema_marker (marker_key)
values ('backend-v2-foundation');
