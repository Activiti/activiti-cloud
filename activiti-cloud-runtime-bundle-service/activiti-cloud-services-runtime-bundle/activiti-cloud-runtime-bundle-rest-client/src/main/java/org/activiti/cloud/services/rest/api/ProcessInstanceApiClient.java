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
package org.activiti.cloud.services.rest.api;

import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.rest.api.configuration.ClientConfiguration;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

@FeignClient(value = "processInstanceApiClient",
    url = "${runtime.url}",
    path = "${runtime.path}",
    configuration = {ClientConfiguration.class})
public interface ProcessInstanceApiClient extends ProcessInstanceController {

    @Override
    @CollectionFormat(feign.CollectionFormat.CSV)
    PagedModel<EntityModel<CloudProcessInstance>> getProcessInstances(Pageable pageable);

    @Override
    @CollectionFormat(feign.CollectionFormat.CSV)
    PagedModel<EntityModel<CloudProcessInstance>> subprocesses(String processInstanceId, Pageable pageable);

}
