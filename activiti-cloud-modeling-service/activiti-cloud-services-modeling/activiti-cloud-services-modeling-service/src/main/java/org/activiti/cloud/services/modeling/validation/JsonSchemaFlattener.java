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
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import org.springframework.core.io.ClassPathResource;

public class JsonSchemaFlattener {

    // TODO pass to the constructor the bean objectMapper
    private ObjectMapper mapper;

    public JsonSchemaFlattener() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private void handleObject(Object value,
                              Map<String, Object> addDefinitions) {
        if (value instanceof ObjectNode) {
            handleJsonNode((ObjectNode) value,
                              addDefinitions);
        } else if (value instanceof ArrayNode) {
            handleJSONArray((ArrayNode) value,
                             addDefinitions);
        }
    }

    private void handleJsonNode(ObjectNode jsonObject,
                                  Map<String, Object> addDefinitions) {

        if (!jsonObject.isEmpty()) {
            Iterator iterator = jsonObject.fieldNames();

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

    private void handleJSONArray(ArrayNode jsonArray,
                                 Map<String, Object> addDefinitions) {

        for (int i = 0; i < jsonArray.size(); i++) {
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
        Optional<String> stringValue = Optional.empty();
        if (value instanceof TextNode) {
            stringValue = Optional.of(((TextNode) value).asText());
        } else if (value != null) {
            stringValue = Optional.of(value.toString());
        }

        if (stringValue.isPresent() && stringValue.get().startsWith("#")) {

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
                ObjectNode jsonObject = (ObjectNode)addDefinitions.get(sectionName);

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

    private ObjectNode loadResourceFromClassPass(String schemaFileName) throws IOException  {

        try (InputStream schemaInputStream = new ClassPathResource(schemaFileName).getInputStream()) {
            return mapper.readValue(schemaInputStream, ObjectNode.class);
        }
    }

    private Optional<ObjectNode> flattenIntern(ObjectNode jsonSchema,
                                               Map<String, Object> addDefinitions) {
        if (!jsonSchema.isEmpty()) {
            handleJsonNode(jsonSchema,
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

    public ObjectNode flatten(ObjectNode jsonSchema) {

        if (jsonSchema == null) {
            return mapper.createObjectNode();
        }

        Map<String, Object> addDefinitions = new HashMap<>();
        Optional<ObjectNode> reply = flattenIntern(jsonSchema,
                                                   addDefinitions);

        ObjectNode replyObject = reply.isPresent() ? reply.get() : jsonSchema;

        ObjectNode definitions = null;
        if (replyObject.has("definitions")) {
            definitions = (ObjectNode)replyObject.get("definitions");
        } else {
            definitions = mapper.createObjectNode();
            replyObject.set("definitions", definitions);
        }

        if (!addDefinitions.isEmpty()) {
            for (Map.Entry<String, Object> entry : addDefinitions.entrySet()) {
                // TODO check if it's ok to use a String here
                definitions.put(entry.getKey(), String.valueOf((entry.getValue())));
            }
        }

        return replyObject;
    }

}
