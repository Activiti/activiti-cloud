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
package org.activiti.cloud.services.audit.jpa.converters;

public class DeleteMe {

    public String coveredUTLocally() {
        return "this is a test method that is covered by unit tests in the same module";
    }

    public String covertedITLocally() {
        return "this is a test method that is covered by integration tests in the same module";
    }

    public String coveredUTExternally() {
        return "this is a test method that is covered by unit tests in other modules";
    }

    public String coveredITExternally() {
        return "this is a test method that is covered by integration tests in other modules";
    }

    public String uncovered() {
        return "this is a test method that is not called in any test";
    }
}
