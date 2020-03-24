create table integration_context
(
    id                  		varchar(255) not null,
    app_name            		varchar(255),
    app_version         		varchar(255),
    service_full_name   		varchar(255),
    service_name        		varchar(255),
    service_type        		varchar(255),
    service_version     		varchar(255),

    process_definition_id      	varchar(255),
    process_definition_key     	varchar(255),
    process_definition_version 	integer,
    process_instance_id        	varchar(255),
    execution_id	        	varchar(255),
    parent_process_instance_id  varchar(255),
    business_key		      	varchar(255),

    client_id                  	varchar(255),
    client_name                	varchar(255),
    client_type                	varchar(255),
    
    connector_type            	varchar(255),
    status                      varchar(255),

    request_date               	timestamp,
    result_date                	timestamp,
    error_date                 	timestamp,

    error_message			   	varchar(255),
    error_class_name    	   	varchar(255),
	stack_trace_elements 	   	CLOB,

	inbound_variables	 	   	CLOB,
	out_bound_variables 	   	CLOB,
	
    primary key (id)
);

alter table bpmn_activity
    add column execution_id varchar(255);
    
alter table bpmn_activity
    delete constraint bpmn_activity_processInstance_elementId_idx;
    
alter table bpmn_activity
    add constraint bpmn_activity_processInstance_elementId_idx unique (process_instance_id, element_id, executionId);
    
