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

  ALTER TABLE project
   ADD COLUMN disp_name varchar(255);

  UPDATE project p
   SET disp_name = p.name;

  ALTER TABLE project
   ALTER COLUMN disp_name SET NOT NULL;

  ALTER TABLE project
   RENAME COLUMN name TO tech_name;

   ALTER TABLE project
      ALTER COLUMN tech_name SET NOT NULL;



