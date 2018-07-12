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

package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.runtime.api.model.CloudProcessInstance;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.impl.CloudProcessInstanceImpl;

public class ToCloudProcessInstanceConverter  {

    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public ToCloudProcessInstanceConverter(RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    public CloudProcessInstance from(ProcessInstance processInstance) {
        CloudProcessInstanceImpl cloudProcessInstance = new CloudProcessInstanceImpl(processInstance);
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudProcessInstance);
        return cloudProcessInstance;
    }

}
