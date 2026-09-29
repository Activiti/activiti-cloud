alter table process_instance add column if not exists correlation_id varchar(255);
create index if not exists pi_correlationId_idx on process_instance (correlation_id);
