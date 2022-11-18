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

package org.activiti.cloud.services.modeling.validation.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;

public class FlowElementsExtractorTest {

    private final FlowElementsExtractor flowElementsExtractor = new FlowElementsExtractor();

    @Test
    public void should_extractElements_when_modelHasASimpleProcess() {
        //given
        final Process process = new Process();
        process.addFlowElement(new StartEvent());
        process.addFlowElement(buildUserTask("Task 1"));
        process.addFlowElement(buildUserTask("Task 2"));
        process.addFlowElement(new ServiceTask());
        final BpmnModel model = new BpmnModel();
        model.addProcess(process);

        //when
        final Set<UserTask> userTasks = flowElementsExtractor.extractFlowElements(model, UserTask.class);

        //then
        assertThat(userTasks).extracting(UserTask::getName).containsExactlyInAnyOrder("Task 1", "Task 2");
    }

    @Test
    public void should_extractElements_when_modelHasSubProcess() {
        //given
        final Process process = new Process();
        process.addFlowElement(new StartEvent());
        process.addFlowElement(buildUserTask("Task from main process"));
        final SubProcess subProcess = new SubProcess();
        process.addFlowElement(subProcess);
        subProcess.addFlowElement(buildUserTask("Task from sub-process"));
        process.addFlowElement(new ServiceTask());
        final BpmnModel model = new BpmnModel();
        model.addProcess(process);

        //when
        final Set<UserTask> userTasks = flowElementsExtractor.extractFlowElements(model, UserTask.class);

        //then
        assertThat(userTasks)
            .extracting(UserTask::getName)
            .containsExactlyInAnyOrder("Task from main process", "Task from sub-process");
    }

    @Test
    public void should_extractElements_when_modelHasMoreThanOneProcess() {
        //given
        final Process process1 = new Process();
        process1.addFlowElement(new StartEvent());
        process1.addFlowElement(buildUserTask("Task from process 1"));
        process1.addFlowElement(new ServiceTask());

        final Process process2 = new Process();
        process2.addFlowElement(new StartEvent());
        process2.addFlowElement(buildUserTask("Task from process 2"));
        process2.addFlowElement(new ServiceTask());

        final BpmnModel model = new BpmnModel();
        model.addProcess(process1);
        model.addProcess(process2);

        //when
        final Set<UserTask> userTasks = flowElementsExtractor.extractFlowElements(model, UserTask.class);

        //then
        assertThat(userTasks)
            .extracting(UserTask::getName)
            .containsExactlyInAnyOrder("Task from process 1", "Task from process 2");
    }

    private UserTask buildUserTask(String name) {
        final UserTask userTask = new UserTask();
        userTask.setName(name);
        return userTask;
    }
}
