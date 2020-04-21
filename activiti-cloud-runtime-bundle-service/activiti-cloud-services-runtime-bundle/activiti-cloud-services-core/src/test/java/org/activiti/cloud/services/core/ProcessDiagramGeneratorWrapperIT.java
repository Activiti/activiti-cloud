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

package org.activiti.cloud.services.core;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.services.core.utils.TestProcessEngine;
import org.activiti.cloud.services.core.utils.TestProcessEngineConfiguration;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.image.exception.ActivitiImageException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

/**
 * Integration tests for ProcessDiagramGeneratorWrapper
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestProcessEngineConfiguration.class)
@TestPropertySource("classpath:application-test-process-diagram.properties")
public class ProcessDiagramGeneratorWrapperIT {

    private static final String DEFAULT_DIAGRAM_FONT_NAME = "Arial";

    @SpringBootApplication
    static class Application {

    }

    @SpyBean
    private ProcessDiagramGeneratorWrapper processDiagramGenerator;

    @Autowired
    private TestProcessEngine processEngine;

    /**
     * Test for generating a valid process diagram
     * <p>
     * 1. deploy a process with diagram
     * 2. start the process
     * 3. generate diagram for the corresponding BPMN model
     * 4. Expected: the diagram is not empty
     */
    @Test
    public void testGenerateProcessDiagram() throws Exception {
        //GIVEN
        processEngine.deploy("processes/SimpleProcess");
        ProcessInstance processInstance = processEngine.startProcessInstanceByKey("SimpleProcess");
        BpmnModel bpmnModel = processEngine.getBpmnModel(processInstance.getProcessDefinitionId());
        assertThat(bpmnModel.hasDiagramInterchangeInfo()).isTrue();

        //WHEN
        byte[] diagram = processDiagramGenerator.generateDiagram(bpmnModel);

        //THEN
        assertThat(diagram).isNotEmpty();
    }

    /**
     * Test for generating diagram a process without diagram
     * <p>
     * 1. deploy a process without diagram
     * 2. start the process
     * 3. generate diagram for the corresponding BPMN model
     * 4. Expected: the diagram is empty
     */
    @Test
    public void testGenerateDiagramForProcessWithNoGraphicInfo() throws Exception {
        //GIVEN
        processEngine.deploy("processes/SubProcessTest.fixSystemFailureProcess");
        ProcessInstance processInstance = processEngine.startProcessInstanceByKey("fixSystemFailure");
        BpmnModel bpmnModel = processEngine.getBpmnModel(processInstance.getProcessDefinitionId());
        assertThat(bpmnModel.hasDiagramInterchangeInfo()).isFalse();

        //WHEN
        byte[] diagram = processDiagramGenerator.generateDiagram(bpmnModel);

        //THEN
        assertThat(diagram).isNotEmpty();
    }

    /**
     * Test for generating diagram a process without diagram when there is no image for the default diagram
     * <p>
     * 1. deploy a process without diagram
     * 2. start the process
     * 3. generate diagram for the corresponding BPMN model
     * 4. Expected: ActivitiException is thrown while generating diagram
     */
    @Test
    public void testGenerateDiagramForProcessWithNoGraphicInfoAndNoDefaultImage() throws Exception {
        //GIVEN
        processEngine.deploy("processes/SubProcessTest.fixSystemFailureProcess");
        ProcessInstance processInstance = processEngine.startProcessInstanceByKey("fixSystemFailure");
        BpmnModel bpmnModel = processEngine.getBpmnModel(processInstance.getProcessDefinitionId());
        assertThat(bpmnModel.hasDiagramInterchangeInfo()).isFalse();

        when(processDiagramGenerator.isGenerateDefaultDiagram())
                .thenReturn(true);
        when(processDiagramGenerator.getDefaultDiagramImageFileName())
                .thenReturn("");

        //WHEN
        byte[] diagram = processDiagramGenerator.generateDiagram(bpmnModel);

        //THEN
        assertThat(diagram).isNotEmpty();
    }

    /**
     * Test for generating diagram a process without diagram when there is no image for the default diagram
     * <p>
     * 1. deploy a process without diagram
     * 2. start the process
     * 3. generate diagram for the corresponding BPMN model
     * 4. Expected: ActivitiException is thrown while generating diagram
     */
    @Test
    public void testGenerateDiagramForProcessWithNoGraphicInfoAndInvalidDefaultImage() throws Exception {
        //GIVEN
        processEngine.deploy("processes/SubProcessTest.fixSystemFailureProcess");
        ProcessInstance processInstance = processEngine.startProcessInstanceByKey("fixSystemFailure");
        BpmnModel bpmnModel = processEngine.getBpmnModel(processInstance.getProcessDefinitionId());
        assertThat(bpmnModel.hasDiagramInterchangeInfo()).isFalse();

        when(processDiagramGenerator.isGenerateDefaultDiagram())
                .thenReturn(true);
        when(processDiagramGenerator.getDefaultDiagramImageFileName())
                .thenReturn("invalid-file-name");

        //THEN
        //WHEN
        assertThatExceptionOfType(ActivitiImageException.class)
            .isThrownBy(() -> processDiagramGenerator.generateDiagram(bpmnModel))
            .withMessageContaining("Error occurred while getting default diagram image from file");
    }

    /**
     * Test for generating diagram a process with invalid diagram
     * <p>
     * 1. generate diagram for an invalid BPMN model
     * 2. Expected: ActivitiException is thrown while generating diagram
     */
    @Test
    public void testGenerateDiagramForProcessWithInvalidGraphicInfo() throws Exception {
        //GIVEN
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addGraphicInfo("key",
                                 null);
        assertThat(bpmnModel.hasDiagramInterchangeInfo()).isTrue();

        //THEN
        //WHEN
        assertThatExceptionOfType(ActivitiException.class)
            .isThrownBy(() -> processDiagramGenerator.generateDiagram(bpmnModel))
            .withMessageContaining("Error occurred while getting process diagram");
    }

    /**
     * Test the diagram custom font.
     * <p>
     * 1. Get the diagram activity font name when the custom font is 'Lucida' in properties file
     * 2. Get the diagram label font name when the custom font is 'InvalidFont' in properties file
     * 3. Get the diagram annotation font name when there is no custom font name specified in properties file
     * 4. Expected:
     * - the diagram activity font name is the custom one from properties file
     * - the diagram label font name is the engine default one (Arial)
     * - the diagram annotation font name is the engine default one (Arial)
     */
    @Test
    public void testProcessDiagramFonts() {
        //GIVEN
        when(processDiagramGenerator.getAvailableFonts())
                .thenReturn(new String[]{"Lucida"});

        //WHEN
        String activityFont = processDiagramGenerator.getActivityFontName();
        String labelFont = processDiagramGenerator.getLabelFontName();
        String annotationFont = processDiagramGenerator.getAnnotationFontName();

        //THEN
        assertThat(activityFont).isEqualTo("Lucida");
        assertThat(labelFont).isEqualTo(DEFAULT_DIAGRAM_FONT_NAME);
        assertThat(annotationFont).isEqualTo(DEFAULT_DIAGRAM_FONT_NAME);
    }

    /**
     * Test the diagram custom font when the only available font on the system is the default one ('Arial').
     * <p>
     * Expected: The only used font is the default, no matter what custom font are specified
     */
    @Test
    public void testProcessDiagramFontsWhenWithAvailableFonts() {
        //GIVEN
        when(processDiagramGenerator.getAvailableFonts())
                .thenReturn(new String[]{DEFAULT_DIAGRAM_FONT_NAME});

        //WHEN
        String activityFont = processDiagramGenerator.getActivityFontName();
        String labelFont = processDiagramGenerator.getLabelFontName();
        String annotationFont = processDiagramGenerator.getAnnotationFontName();

        //THEN
        assertThat(activityFont).isEqualTo(DEFAULT_DIAGRAM_FONT_NAME);
        assertThat(labelFont).isEqualTo(DEFAULT_DIAGRAM_FONT_NAME);
        assertThat(annotationFont).isEqualTo(DEFAULT_DIAGRAM_FONT_NAME);
    }
}
