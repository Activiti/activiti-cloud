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
package org.activiti.cloud.api.process.model;

import org.activiti.api.process.model.BPMNActivity;
import org.activiti.cloud.api.model.shared.CloudRuntimeEntity;

import java.util.Date;

public interface CloudBPMNActivity extends CloudRuntimeEntity,
        BPMNActivity {

    public static enum BPMNActivityStatus {
        STARTED {
            @Override
            public boolean isFinalState() {
                return false;
            }
        },
        COMPLETED {
            @Override
            public boolean isFinalState() {
                return true;
            }
        },
        CANCELLED {
            @Override
            public boolean isFinalState() {
                return true;
            }
        },
        ERROR {
            @Override
            public boolean isFinalState() {
                return true;
            }
        };

        public abstract boolean isFinalState();
    }

    String getId();

    String getBusinessKey();

    String getProcessDefinitionKey();

    Integer getProcessDefinitionVersion();

    BPMNActivityStatus getStatus();

    Date getStartedDate();

    Date getCompletedDate();

    Date getCancelledDate();

}
