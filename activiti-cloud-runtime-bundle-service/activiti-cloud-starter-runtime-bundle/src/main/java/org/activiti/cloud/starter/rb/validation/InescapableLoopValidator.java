/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.starter.rb.validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.validation.ValidationError;
import org.activiti.validation.validator.ProcessLevelValidator;

/**
 * Detects a <b>loop with no way out</b>: a flow node that can circle back to
 * itself and can never reach an end event, so a process instance is stuck forever.
 *
 * <p>It handles these basic cases:
 * <ul>
 *   <li>A points to B and B points back to A (A &rarr; B &rarr; A),</li>
 *   <li>a single node points to itself (A &rarr; A), and</li>
 *   <li>a gateway whose every branch loops back and none reaches an end.</li>
 * </ul>
 *
 * <p>For each node we ask two simple questions by following the arrows:
 * <ol>
 *   <li><b>Is it on a loop?</b> - can we get from the node back to itself?</li>
 *   <li><b>Can it reach an end?</b> - is there any path to an end event?</li>
 * </ol>
 * A node is reported only when it is on a loop <em>and</em> cannot reach an end.
 * A normal rework loop that also has an exit therefore stays valid.
 *
 * <p>This validator is Activiti Cloud specific and is only registered when the
 * feature flag {@code activiti.cloud.validation.inescapable-loop.enabled} is set.
 */
public class InescapableLoopValidator extends ProcessLevelValidator {

    public static final String FLOW_INESCAPABLE_LOOP = "FLOW_INESCAPABLE_LOOP";

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        // Build a simple graph: node id -> the ids it points to (via sequence flows).
        Map<String, List<String>> successors = buildSuccessors(process);

        Set<String> reported = new HashSet<>();
        for (String node : successors.keySet()) {
            if (isOnLoop(node, successors) && !canReachEnd(node, process, successors) && reported.add(node)) {
                addError(errors, FLOW_INESCAPABLE_LOOP, process, (FlowNode) process.getFlowElement(node));
            }
        }
    }

    /**
     * Reads every sequence flow and records, for each node, the nodes it points to.
     * Uses {@link SequenceFlow#getSourceRef()} and {@link SequenceFlow#getTargetRef()}.
     */
    private Map<String, List<String>> buildSuccessors(Process process) {
        Map<String, List<String>> successors = new LinkedHashMap<>();
        for (SequenceFlow flow : process.findFlowElementsOfType(SequenceFlow.class, true)) {
            successors.computeIfAbsent(flow.getSourceRef(), key -> new ArrayList<>()).add(flow.getTargetRef());
        }
        return successors;
    }

    /**
     * Follows the arrows starting from {@code start}. Returns true if we can arrive
     * back at {@code start} - meaning it sits on a loop.
     */
    private boolean isOnLoop(String start, Map<String, List<String>> successors) {
        Set<String> visited = new HashSet<>();
        Deque<String> toVisit = new ArrayDeque<>(successors.getOrDefault(start, List.of()));
        while (!toVisit.isEmpty()) {
            String node = toVisit.poll();
            if (node.equals(start)) {
                return true; // came back to where we started -> loop
            }
            if (visited.add(node)) {
                toVisit.addAll(successors.getOrDefault(node, List.of()));
            }
        }
        return false;
    }

    /**
     * Follows the arrows starting from {@code start}. Returns true if any path
     * reaches an end event - i.e. there is a way out.
     */
    private boolean canReachEnd(String start, Process process, Map<String, List<String>> successors) {
        Set<String> visited = new HashSet<>();
        Deque<String> toVisit = new ArrayDeque<>();
        toVisit.add(start);
        while (!toVisit.isEmpty()) {
            String node = toVisit.poll();
            if (process.getFlowElement(node) instanceof EndEvent) {
                return true;
            }
            if (visited.add(node)) {
                toVisit.addAll(successors.getOrDefault(node, List.of()));
            }
        }
        return false;
    }
}
