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
package org.activiti.cloud.services.notifications.graphql.ws.util;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class QueryParameters {

    String query;
    String operationName;
    Map<String, Object> variables = Collections.emptyMap();

    public String getQuery() {
        return query;
    }

    public String getOperationName() {
        return operationName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public static QueryParameters from(String queryMessage) throws JsonParseException, JsonMappingException, IOException {
        Map<String, Object> json = JsonConverter.toMap(queryMessage);
        return from(json);
    }

    public static QueryParameters from(Map<String, Object> json) throws JsonParseException, JsonMappingException, IOException {
        QueryParameters parameters = new QueryParameters();
        parameters.query = (String) json.get("query");
        parameters.operationName = (String) json.get("operationName");
        parameters.variables = getVariables(json.get("variables"));
        return parameters;
    }


    private static Map<String, Object> getVariables(Object variables) throws JsonParseException, JsonMappingException, IOException {
        if (variables instanceof Map) {
            Map<?, ?> inputVars = (Map) variables;
            Map<String, Object> vars = new HashMap<>();
            inputVars.forEach((k, v) -> vars.put(String.valueOf(k), v));
            return vars;
        }
        return JsonConverter.toMap(String.valueOf(variables));
    }

}
