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
package org.activiti.cloud.api;

public class DeleteMe {
    public String coveredByUnit() {
        return "covered by unit test from this module";
    }
    public String coveredByIT() {
        return "covered by integration test from this module";
    }
    
    public String coveredByExternalUnit() {
        return "covered by external unit test from another module";
    }
    public String coveredByExternalIT() {
        return "covered by an integration test from another module";
    }
    public String uncovered() {
        return "this method should not be covered by any test";
    }
}
