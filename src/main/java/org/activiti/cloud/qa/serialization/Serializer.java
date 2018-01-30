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

package org.activiti.cloud.qa.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.qa.model.AuthToken;
import org.activiti.cloud.qa.model.EventsResponse;
import org.activiti.cloud.qa.model.ProcessInstanceResponse;
import org.activiti.cloud.qa.model.TasksResponse;

public class Serializer {

    private static ObjectMapper objectMapper;
    static  {
         objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    public static AuthToken toAuthToken(String token) throws IOException{
        return objectMapper.readValue(token, AuthToken.class);
    }

    public static ProcessInstanceResponse toProcessInstance(String processInstanceResponse) throws IOException{
        return objectMapper.readValue(processInstanceResponse, ProcessInstanceResponse.class);
    }

    public static TasksResponse toTaskResponse(String tasksResponse) throws IOException{
        return objectMapper.readValue(tasksResponse, TasksResponse.class);
    }

    public static EventsResponse toEventsResponse(String eventsResponse) throws IOException{
        return objectMapper.readValue(eventsResponse, EventsResponse.class);
    }

    public static String toJsonString(Object object)throws JsonProcessingException{

        return objectMapper.writeValueAsString(object);
    }
}
