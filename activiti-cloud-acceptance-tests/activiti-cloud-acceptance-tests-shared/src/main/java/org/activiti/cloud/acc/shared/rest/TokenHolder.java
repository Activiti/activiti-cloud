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
package org.activiti.cloud.acc.shared.rest;

import org.activiti.cloud.acc.shared.model.AuthToken;

public class TokenHolder {

    private static AuthToken authToken = null;
    private static String userName = null;

    public static AuthToken getAuthToken() {
        return authToken;
    }

    public static void setAuthToken(AuthToken authToken) {
        TokenHolder.authToken = authToken;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setUserName(String userName) {
        TokenHolder.userName = userName;
    }
}
