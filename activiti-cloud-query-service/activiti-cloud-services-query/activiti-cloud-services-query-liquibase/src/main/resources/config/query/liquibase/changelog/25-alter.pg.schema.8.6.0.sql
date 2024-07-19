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

CREATE TABLE IF NOT EXISTS process_variable_pivot
(
    process_instance_id varchar(255) not null,
    process_definition_key  varchar(255) not null,
    process_variables       jsonb
);

CREATE INDEX process_instance_id_idx ON process_variable_pivot(process_instance_id);
CREATE INDEX process_variables_idx ON process_variable_pivot USING GIN (process_variables jsonb_path_ops);
