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

ALTER TABLE process_variable
ALTER COLUMN "value" TYPE jsonb USING "value"::jsonb;

ALTER TABLE task_variable
ALTER COLUMN "value" TYPE jsonb USING "value"::jsonb;

CREATE INDEX process_variable_value_idx ON process_variable USING GIN ((value -> 'value') jsonb_path_ops);
CREATE INDEX process_definition_key_name_value_idx ON process_variable (process_definition_key,name);

CREATE INDEX task_variable_value_idx ON task_variable USING GIN ((value -> 'value') jsonb_path_ops);
