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
package org.activiti.cloud.services.query.app.specification;

/**
 * A sort request could not be applied - e.g. sorting by a process variable without naming its
 * process definition key and type. Deliberately not an {@link IllegalArgumentException}/
 * {@link IllegalStateException} - those get silently translated into a message-bearing
 * {@code InvalidDataAccessApiUsageException} by Spring's JPA exception translation when thrown
 * from inside a {@code Specification}, which only has a handler for task search, not process
 * instance search - so process instance callers would see a 500 instead of a 400. Unlike
 * {@link IllegalFilterException}, the message here is safe and meant to reach the client.
 */
public class InvalidSortException extends RuntimeException {

    public InvalidSortException(String message) {
        super(message);
    }
}
