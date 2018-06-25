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

package org.activiti.cloud.qa.steps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.ProcessInstance;
import org.activiti.cloud.qa.model.Task;
import org.activiti.cloud.qa.model.commands.CreateTaskCmd;
import org.activiti.cloud.qa.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.qa.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.qa.service.RuntimeBundleDiagramService;
import org.activiti.cloud.qa.service.RuntimeBundleService;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;

import static org.assertj.core.api.Assertions.*;

/**
 * Runtime bundle steps
 */
@EnableRuntimeFeignContext
public class MultipleRuntimeBundleSteps {

    public static final String DEFAULT_PROCESS_INSTANCE_COMMAND_TYPE = "StartProcessInstanceCmd";

    public static final String DEFAULT_PROCESS_INSTANCE_KEY = "ProcessWithVariables";

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private RuntimeBundleService runtimeBundleService;

    @Autowired
    private RuntimeBundleService runtimeBundleAnotherService;

    @Step
    public void checkServicesHealth() {
        assertThat(runtimeBundleService.isServiceUp()).isTrue();
    }

    @Step
    public Map<String, Object> health() {
        return runtimeBundleService.health();
    }

    @Step
    public ProcessInstance startProcess(String process, boolean isPrimaryService) {

        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setCommandType(DEFAULT_PROCESS_INSTANCE_COMMAND_TYPE);
        processInstance.setProcessDefinitionKey(process);

        if (isPrimaryService) {
            return dirtyContextHandler.dirty(runtimeBundleService.startProcess(processInstance));
        } else {
        	return dirtyContextHandler.dirty(runtimeBundleAnotherService.startProcess(processInstance));
        }
    }

    @Step
    public void waitForMessagesToBeConsumed() throws InterruptedException {
        Thread.sleep(220);
    }
}
