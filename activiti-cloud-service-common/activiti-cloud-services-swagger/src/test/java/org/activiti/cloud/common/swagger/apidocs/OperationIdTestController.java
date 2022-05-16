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
package org.activiti.cloud.common.swagger.apidocs;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("test/operationId")
public class OperationIdTestController {

    @GetMapping
    public String getTest() {
        return "test";
    }

    @GetMapping("entity")
    public EntityModel<String> getEntityModel() {
        return EntityModel.of("example");
    }

    @GetMapping("collection")
    public CollectionModel<EntityModel<String>> getCollectionModel() {
        return CollectionModel.empty();
    }

    @GetMapping("paged")
    public PagedModel<EntityModel<String>> getPagedModel(Pageable pageable) {
        return PagedModel.empty();
    }
}
