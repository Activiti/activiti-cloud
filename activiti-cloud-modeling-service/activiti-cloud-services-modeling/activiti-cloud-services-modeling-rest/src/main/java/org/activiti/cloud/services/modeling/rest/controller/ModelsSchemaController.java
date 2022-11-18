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
package org.activiti.cloud.services.modeling.rest.controller;

import java.util.Optional;
import org.activiti.cloud.services.modeling.rest.api.ModelsSchemaRestApi;
import org.activiti.cloud.services.modeling.service.SchemaService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Schema resources.
 */
@RestController
public class ModelsSchemaController implements ModelsSchemaRestApi {

    @Autowired
    private SchemaService schemaService;

    @Override
    public ResponseEntity<String> getSchema(String modelType) {
        Optional<JSONObject> jsonSchema = schemaService.getJsonSchemaFromType(modelType);
        if (jsonSchema.isPresent()) {
            return new ResponseEntity<>(jsonSchema.get().toString(), HttpStatus.OK);
        } else {
            throw new ResourceNotFoundException();
        }
    }
}
