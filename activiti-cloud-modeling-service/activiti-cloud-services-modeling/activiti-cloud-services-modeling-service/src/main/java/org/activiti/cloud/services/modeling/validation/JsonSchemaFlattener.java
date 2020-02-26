/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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
package org.activiti.cloud.services.modeling.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.core.io.ClassPathResource;

public class JsonSchemaFlattener {

    public JsonSchemaFlattener() {

    }

    private void handleObject(Object value,
                              Map<String, Object> addDefinitions) {
        if (value instanceof JSONObject) {
            handleJSONObject((JSONObject) value,
                              addDefinitions);
        } else if (value instanceof JSONArray) {
            handleJSONArray((JSONArray) value,
                             addDefinitions);
        }
    }

    private void handleJSONObject(JSONObject jsonObject,
                                  Map<String, Object> addDefinitions) {

        if (!jsonObject.isEmpty()) {
            Iterator iterator = jsonObject.keys();

            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                Object value = jsonObject.get(key);

                if (isKeyToCheck(key)) {
                    getUpdatedValue(value, addDefinitions).ifPresent(updatedValue -> jsonObject.put(key, updatedValue));
                } else {
                    handleObject(value, addDefinitions);
                }

            }
        }
    }

    private void handleJSONArray(JSONArray jsonArray,
                                 Map<String, Object> addDefinitions) {

        for (int i = 0; i < jsonArray.length(); i++) {
            handleObject(jsonArray.get(i),
                         addDefinitions);
        }
   }

    private boolean isKeyToCheck(String key) {
        return Objects.equals("$ref", key);
    }

    private Optional<String> getClassPathFileName(String value) {
        String regex = "classpath:\\/\\/(.*)";
        Matcher matcher = Pattern.compile(regex)
                                 .matcher(value);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).toString());
        }

        return Optional.empty();
    }

    private Optional<String> getUpdatedValue(Object value,
                                             Map<String, Object> addDefinitions) {

        Optional<String> stringValue = Optional.of(value)
                                        .filter(String.class::isInstance)
                                        .map(String.class::cast)
                                        .filter(s -> !s.startsWith("#"));

        if (stringValue.isPresent()) {

            Optional<String> stringRef = Optional.empty();
            Optional<String> fileName = getClassPathFileName(stringValue.get());

            if (!fileName.isPresent()) {
                String regex = "(.*)\\/#\\/(.*)";
                Matcher matcher = Pattern.compile(regex)
                                         .matcher(stringValue.get());
                if (matcher.find()) {
                    fileName = Optional.of(matcher.group(1).toString());
                    stringRef = Optional.of(matcher.group(2).toString());
                } else {
                    fileName = Optional.of(stringValue.get());
                }
            }

            if (fileName.isPresent()) {

                String sectionName = getSectionNameFromFileName(fileName.get());
                JSONObject jsonObject = (JSONObject)addDefinitions.get(sectionName);

                if (jsonObject == null) {

                    try {
                        jsonObject = loadResourceFromClassPass(fileName.get());
                    } catch (IOException e) {
                        jsonObject = null;;
                    }
                }

                if (jsonObject != null) {
                    addDefinitions.put(sectionName, flattenIntern(jsonObject, addDefinitions).get());

                    return Optional.of("#/definitions/" + sectionName + (stringRef.isPresent() ? stringRef.get() : ""));
                }

            }
        }

        return Optional.empty();
    }

    private JSONObject loadResourceFromClassPass(String schemaFileName) throws IOException  {

        try (InputStream schemaInputStream = new ClassPathResource(schemaFileName).getInputStream()) {
            return  new JSONObject(new JSONTokener(schemaInputStream));
        }
    }

    private Optional<JSONObject> flattenIntern(JSONObject jsonSchema,
                                               Map<String, Object> addDefinitions) {
        if (!jsonSchema.isEmpty()) {
            handleJSONObject(jsonSchema,
                             addDefinitions);
        }
        return Optional.of(jsonSchema);
    }

    public String getSectionNameFromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }

        return fileName
                .replaceAll(".json","")
                .replaceAll("[/-]","_")
                .replaceAll("[^a-zA-Z0-9_]+","");
    }

    public JSONObject flatten(JSONObject jsonSchema) {

        if (jsonSchema == null) {
            return new JSONObject();
        }

        Map<String, Object> addDefinitions = new HashMap<>();
        Optional<JSONObject> reply = flattenIntern(jsonSchema,
                                                   addDefinitions);

        JSONObject replyObject = reply.isPresent() ? reply.get() : jsonSchema;

        if (!addDefinitions.isEmpty()) {

            JSONObject definitions = null;
            if (replyObject.has("definitions")) {
                definitions = (JSONObject)replyObject.get("definitions");
            } else {
                definitions = new JSONObject();
            }
            for (Map.Entry<String, Object> entry : addDefinitions.entrySet()) {
                definitions.put(entry.getKey(), entry.getValue());
            }

            replyObject.put("definitions", definitions);
        }

        return replyObject;
    }

}
