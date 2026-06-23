/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

package org.activiti.cloud.services.query.liquibase;

import org.springframework.stereotype.Service;

@Service
public class DummyClassToTestITCoverage {

    public void directDummyMethod() {
        inDirectDummyMethod();
        // This method is intentionally left blank to verify test coverage
    }

    public void inDirectDummyMethod() {
        // This method is intentionally left blank to verify test coverage
    }

    public void dummyMethodNotCoveredInTests() {}

}
