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

package org.activiti.cloud.api.process.model.impl;

import java.util.Arrays;
import java.util.List;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationRequest;

public class IntegrationErrorImpl extends CloudRuntimeEntityImpl implements IntegrationError {

    private IntegrationRequest integrationRequest;
    private IntegrationContext integrationContext;
    
    private String errorMessage;
    private List<StackTraceElement> stackTraceElements;
    private String errorClassName;
    
    IntegrationErrorImpl() {
    }

    public IntegrationErrorImpl(IntegrationRequest integrationRequest,
                                Throwable error) {
        this.integrationRequest = integrationRequest;
        this.integrationContext = integrationRequest.getIntegrationContext();
        this.errorMessage = error.getMessage();
        this.errorClassName = error.getClass().getName();
        this.stackTraceElements = Arrays.asList(error.getStackTrace());
    }

    @Override
    public IntegrationContext getIntegrationContext() {
        return integrationContext;
    }

    @Override
    public IntegrationRequest getIntegrationRequest() {
        return integrationRequest;
    }
    
    @Override
    public List<StackTraceElement> getStackTraceElements() {
        return stackTraceElements;
    }
    
    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String getErrorClassName() {
        return errorClassName;
    }
}
