/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.activiti.cloud.api.process.model.impl.events;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;

public abstract class BaseCloudProcessInstanceEventImpl<T extends Enum<?>> extends CloudRuntimeEventImpl<ProcessInstance,
    T> {

  public BaseCloudProcessInstanceEventImpl() { }

  public BaseCloudProcessInstanceEventImpl(ProcessInstance processInstance) {
    super(processInstance);
    setFlattenInfo(processInstance);
  }

  public BaseCloudProcessInstanceEventImpl(String id,
      Long timestamp,
      ProcessInstance processInstance) {
    super(id,
        timestamp,
        processInstance);
    setFlattenInfo(processInstance);
  }


  private void setFlattenInfo(ProcessInstance processInstance) {
    setProcessDefinitionId(processInstance.getProcessDefinitionId());
    setProcessInstanceId(processInstance.getId());
    setEntityId(processInstance.getId());
    setBusinessKey(processInstance.getBusinessKey());
    setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
    setProcessDefinitionVersion(processInstance.getProcessDefinitionVersion());
    setParentProcessInstanceId(processInstance.getParentId());
    setAppVersion(processInstance.getAppVersion());
  }
}
