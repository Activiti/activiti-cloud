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

import org.activiti.cloud.starter.rb.behavior.CloudActivityBehaviorFactory;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.context.ApplicationContext;

public class SignalBehaviourConfigurer implements ProcessEngineConfigurationConfigurer {

    private ApplicationContext applicationContext;

    public SignalBehaviourConfigurer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
        processEngineConfiguration.setActivityBehaviorFactory(new CloudActivityBehaviorFactory(applicationContext));
    }
}
