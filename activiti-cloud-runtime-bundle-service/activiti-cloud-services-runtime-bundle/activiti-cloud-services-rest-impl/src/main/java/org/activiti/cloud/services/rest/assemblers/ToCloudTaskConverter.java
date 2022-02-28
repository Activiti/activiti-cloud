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
package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.impl.CloudTaskImpl;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;

public class ToCloudTaskConverter {

    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    public ToCloudTaskConverter(
        RuntimeBundleInfoAppender runtimeBundleInfoAppender
    ) {
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    public CloudTask from(Task task) {
        CloudTaskImpl cloudTask = new CloudTaskImpl(task);
        runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(cloudTask);
        return cloudTask;
    }
}
