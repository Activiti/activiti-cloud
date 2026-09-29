alter table process_instance add (correlation_id varchar(255));
create index pi_correlationId_idx on process_instance (correlation_id);
