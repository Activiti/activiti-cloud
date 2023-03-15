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
package org.activiti.cloud.acc.shared.serenity;

import java.util.List;
import java.util.stream.Collectors;
import org.jbehave.core.annotations.ScenarioType;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.steps.BeforeOrAfterStep;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.StepCandidate;

/**
 * Extended SerenityCandidateSteps
 */
public class ExtendedSerenityCandidateSteps implements CandidateSteps {

    private final CandidateSteps candidateSteps;

    private final Configuration configuration;

    private final InjectableStepsFactory stepsFactory;

    public ExtendedSerenityCandidateSteps(
        CandidateSteps candidateSteps,
        Configuration configuration,
        InjectableStepsFactory stepsFactory
    ) {
        this.candidateSteps = candidateSteps;
        this.configuration = configuration;
        this.stepsFactory = stepsFactory;
    }

    @Override
    public List<StepCandidate> listCandidates() {
        return candidateSteps
            .listCandidates()
            .parallelStream()
            .map(step -> new ExtendedSerenityStepCandidate(step, configuration, stepsFactory))
            .collect(Collectors.toList());
    }

    public List<BeforeOrAfterStep> listBeforeOrAfterStories() {
        return candidateSteps.listBeforeOrAfterStories();
    }

    public List<BeforeOrAfterStep> listBeforeOrAfterStory(boolean givenStory) {
        return candidateSteps.listBeforeOrAfterStory(givenStory);
    }

    public List<BeforeOrAfterStep> listBeforeOrAfterScenario(ScenarioType type) {
        return candidateSteps.listBeforeOrAfterScenario(type);
    }

    public Configuration configuration() {
        return candidateSteps.configuration();
    }
}
