/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

create table task_process_variable
(
  task_id varchar(255) not null,
  process_variable_id bigint not null
);
alter table if exists task_process_variable
  add constraint fk_task_id foreign key (task_id) references task;
alter table if exists task_process_variable
  add constraint fk_process_variable_id foreign key (process_variable_id) references process_variable;
