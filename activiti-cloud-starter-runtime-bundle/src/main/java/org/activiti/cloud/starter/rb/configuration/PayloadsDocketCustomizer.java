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

package org.activiti.cloud.starter.rb.configuration;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
import org.activiti.cloud.common.swagger.DocketCustomizer;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger Api Models
 */
public class PayloadsDocketCustomizer implements DocketCustomizer {

    public Docket customize(final Docket docket) {
        return docket
                .directModelSubstitute(StartProcessPayload.class,
                                                            StartProcessPayloadApiModel.class)
                .directModelSubstitute(SignalPayload.class,
                                       SignalPayloadApiModel.class)
                .directModelSubstitute(UpdateProcessPayload.class,
                                       UpdateProcessPayloadApiModel.class)
                .directModelSubstitute(SetProcessVariablesPayload.class,
                                       SetProcessVariablesPayloadApiModel.class)
                .directModelSubstitute(RemoveProcessVariablesPayload.class,
                                       RemoveProcessVariablesPayloadApiModel.class)
                .directModelSubstitute(AssignTaskPayload.class,
                                       AssignTaskPayloadApiModel.class)
                .directModelSubstitute(CompleteTaskPayload.class,
                                       CompleteTaskPayloadApiModel.class)
                .directModelSubstitute(CandidateGroupsPayload.class,
                                       CandidateGroupsPayloadApiModel.class)
                .directModelSubstitute(CandidateUsersPayload.class,
                                       CandidateUsersPayloadApiModel.class)
                .directModelSubstitute(CreateTaskPayload.class,
                                       CreateTaskPayloadApiModel.class)
                .directModelSubstitute(CreateTaskVariablePayload.class,
                                       CreateTaskVariablePayloadApiModel.class)
                .directModelSubstitute(UpdateTaskVariablePayload.class,
                                       UpdateTaskVariablePayloadApiModel.class)
                .directModelSubstitute(UpdateTaskPayload.class,
                                       UpdateTaskPayloadApiModel.class)
                .directModelSubstitute(SaveTaskPayload.class,
                                        SaveTaskPayloadApiModel.class);
    }

    @ApiModel("StartProcessPayload")
    public class StartProcessPayloadApiModel extends StartProcessPayload {

        @ApiModelProperty(allowableValues = "StartProcessPayload")
        public String payloadType;
    }

    @ApiModel("SignalPayload")
    public class SignalPayloadApiModel extends SignalPayload {

        @ApiModelProperty(allowableValues = "SignalPayload")
        public String payloadType;
    }

    @ApiModel("UpdateProcessPayload")
    public class UpdateProcessPayloadApiModel extends UpdateProcessPayload {

        @ApiModelProperty(allowableValues = "UpdateProcessPayload")
        public String payloadType;
    }

    @ApiModel("SetProcessVariablesPayload")
    public class SetProcessVariablesPayloadApiModel extends SetProcessVariablesPayload {

        @ApiModelProperty(allowableValues = "SetProcessVariablesPayload")
        public String payloadType;
    }

    @ApiModel("RemoveProcessVariablesPayload")
    public class RemoveProcessVariablesPayloadApiModel extends RemoveProcessVariablesPayload {

        @ApiModelProperty(allowableValues = "RemoveProcessVariablesPayload")
        public String payloadType;
    }

    @ApiModel("CandidateGroupsPayload")
    public class CandidateGroupsPayloadApiModel extends CandidateGroupsPayload {

        @ApiModelProperty(allowableValues = "CandidateGroupsPayload")
        public String payloadType;
    }

    @ApiModel("CandidateUsersPayload")
    public class CandidateUsersPayloadApiModel extends CandidateUsersPayload {

        @ApiModelProperty(allowableValues = "CandidateUsersPayload")
        public String payloadType;
    }

    @ApiModel("AssignTaskPayload")
    public class AssignTaskPayloadApiModel extends AssignTaskPayload {

        @ApiModelProperty(allowableValues = "AssignTaskPayload")
        public String payloadType;
    }

    @ApiModel("CompleteTaskPayload")
    public class CompleteTaskPayloadApiModel extends CompleteTaskPayload {

        @ApiModelProperty(allowableValues = "CompleteTaskPayload")
        public String payloadType;
    }

    @ApiModel("CreateTaskPayload")
    public class CreateTaskPayloadApiModel extends CreateTaskPayload {

        @ApiModelProperty(allowableValues = "CreateTaskPayload")
        public String payloadType;
    }

    @ApiModel("CreateTaskVariablePayload")
    public class CreateTaskVariablePayloadApiModel extends CreateTaskVariablePayload {

        @ApiModelProperty(allowableValues = "CreateTaskVariablePayload")
        public String payloadType;
    }

    @ApiModel("UpdateTaskVariablePayload")
    public class UpdateTaskVariablePayloadApiModel extends UpdateTaskVariablePayload {

        @ApiModelProperty(allowableValues = "UpdateTaskVariablePayload")
        public String payloadType;
    }

    @ApiModel("UpdateTaskPayload")
    public class UpdateTaskPayloadApiModel extends UpdateTaskPayload {

        @ApiModelProperty(allowableValues = "UpdateTaskPayload")
        public String payloadType;
    }

    @ApiModel("SaveTaskPayload")
    public class SaveTaskPayloadApiModel extends SaveTaskPayload {

        @ApiModelProperty(allowableValues = "SaveTaskPayload")
        public String payloadType;
    }
}
