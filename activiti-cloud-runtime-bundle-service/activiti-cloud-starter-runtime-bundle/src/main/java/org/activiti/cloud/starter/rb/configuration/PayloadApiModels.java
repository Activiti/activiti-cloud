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
package org.activiti.cloud.starter.rb.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import org.activiti.api.process.model.payloads.CreateProcessInstancePayload;
import org.activiti.api.process.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;

/**
 * Swagger Api Models substitution with the payload field enriched Models version
 */
public class PayloadApiModels {

    @Schema(name = "StartProcessPayload")
    public static class StartProcessPayloadApiModel extends StartProcessPayload {

        @Schema(allowableValues = "StartProcessPayload")
        public String payloadType;
    }

    @Schema(name = "SignalPayload")
    public static class SignalPayloadApiModel extends SignalPayload {

        @Schema(allowableValues = "SignalPayload")
        public String payloadType;
    }

    @Schema(name = "UpdateProcessPayload")
    public static class UpdateProcessPayloadApiModel extends UpdateProcessPayload {

        @Schema(allowableValues = "UpdateProcessPayload")
        public String payloadType;
    }

    @Schema(name = "SetProcessVariablesPayload")
    public static class SetProcessVariablesPayloadApiModel extends SetProcessVariablesPayload {

        @Schema(allowableValues = "SetProcessVariablesPayload")
        public String payloadType;
    }

    @Schema(name = "RemoveProcessVariablesPayload")
    public static class RemoveProcessVariablesPayloadApiModel extends RemoveProcessVariablesPayload {

        @Schema(allowableValues = "RemoveProcessVariablesPayload")
        public String payloadType;
    }

    @Schema(name = "CandidateGroupsPayload")
    public static class CandidateGroupsPayloadApiModel extends CandidateGroupsPayload {

        @Schema(allowableValues = "CandidateGroupsPayload")
        public String payloadType;
    }

    @Schema(name = "CandidateUsersPayload")
    public static class CandidateUsersPayloadApiModel extends CandidateUsersPayload {

        @Schema(allowableValues = "CandidateUsersPayload")
        public String payloadType;
    }

    @Schema(name = "AssignTaskPayload")
    public static class AssignTaskPayloadApiModel extends AssignTaskPayload {

        @Schema(allowableValues = "AssignTaskPayload")
        public String payloadType;
    }

    @Schema(name = "CompleteTaskPayload")
    public static class CompleteTaskPayloadApiModel extends CompleteTaskPayload {

        @Schema(allowableValues = "CompleteTaskPayload")
        public String payloadType;
    }

    @Schema(name = "CreateTaskPayload")
    public static class CreateTaskPayloadApiModel extends CreateTaskPayload {

        @Schema(allowableValues = "CreateTaskPayload")
        public String payloadType;
    }

    @Schema(name = "CreateTaskVariablePayload")
    public static class CreateTaskVariablePayloadApiModel extends CreateTaskVariablePayload {

        @Schema(allowableValues = "CreateTaskVariablePayload")
        public String payloadType;
    }

    @Schema(name = "UpdateTaskVariablePayload")
    public static class UpdateTaskVariablePayloadApiModel extends UpdateTaskVariablePayload {

        @Schema(allowableValues = "UpdateTaskVariablePayload")
        public String payloadType;
    }

    @Schema(name = "UpdateTaskPayload")
    public static class UpdateTaskPayloadApiModel extends UpdateTaskPayload {

        @Schema(allowableValues = "UpdateTaskPayload")
        public String payloadType;
    }

    @Schema(name = "SaveTaskPayload")
    public static class SaveTaskPayloadApiModel extends SaveTaskPayload {

        @Schema(allowableValues = "SaveTaskPayload")
        public String payloadType;
    }

    @Schema(name = "CreateProcessInstancePayload")
    public static class CreateProcessInstancePayloadApiModel extends CreateProcessInstancePayload {

        @Schema(allowableValues = "CreateProcessInstancePayload")
        public String payloadType;
    }
}
