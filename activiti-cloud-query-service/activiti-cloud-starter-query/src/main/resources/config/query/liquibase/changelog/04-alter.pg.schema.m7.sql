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
	stack_trace_elements 	   	text,

	inbound_variables	 	   	text,
	out_bound_variables 	   	text,
    
    bpmn_activity_id    	   	varchar(255) not null,
    
    primary key (id)
);
