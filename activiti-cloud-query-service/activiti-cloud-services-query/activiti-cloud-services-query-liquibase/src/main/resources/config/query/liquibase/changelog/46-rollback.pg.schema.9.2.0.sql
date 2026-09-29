drop index if exists pi_correlationId_idx;
alter table process_instance drop column if exists correlation_id;
