alter table process_definition
    add column category varchar(255);

create sequence variable_sequence start with 1 increment by 50;

select setval('variable_sequence', (select max(id) from process_variable));
