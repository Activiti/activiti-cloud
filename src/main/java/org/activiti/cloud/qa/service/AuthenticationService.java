/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.qa.Config;
import org.activiti.cloud.qa.client.ConnectorHelper;
import org.activiti.cloud.qa.model.AuthToken;
import org.activiti.cloud.qa.serialization.Serializer;

public class AuthenticationService {

    public static AuthToken authenticate() throws IOException{

        Map<String,String> form = new HashMap<String, String>();
        form.put("client_id", "activiti");
        form.put("grant_type", "password");
        form.put("username","hruser");
        form.put("password","password");

        return Serializer.toAuthToken(ConnectorHelper.postForm(Config.getInstance().getProperties().getProperty("auth.url"), form, null));
    }

}
