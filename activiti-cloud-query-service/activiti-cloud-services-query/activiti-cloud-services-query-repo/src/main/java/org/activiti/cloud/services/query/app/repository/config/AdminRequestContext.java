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
package org.activiti.cloud.services.query.app.repository.config;

public class AdminRequestContext {

    private static final ThreadLocal<Boolean> ADMIN_REQUEST = ThreadLocal.withInitial(() -> false);

    public static void markAsAdminRequest() {
        ADMIN_REQUEST.set(true);
    }

    public static void markAsUserRequest() {
        ADMIN_REQUEST.set(false);
    }

    public static boolean isAdminRequest() {
        return ADMIN_REQUEST.get();
    }

    public static void clear() {
        ADMIN_REQUEST.remove();
    }
}
