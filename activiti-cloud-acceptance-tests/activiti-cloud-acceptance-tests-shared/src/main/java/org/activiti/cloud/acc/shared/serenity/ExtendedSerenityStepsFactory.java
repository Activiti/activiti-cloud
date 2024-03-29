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
import net.serenitybdd.jbehave.SerenityStepFactory;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.failures.FailingUponPendingStep;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.core.steps.Steps;

/**
 * Extended SerenityStepFactory
 */
public class ExtendedSerenityStepsFactory extends SerenityStepFactory {

    private Configuration configuration;

    public ExtendedSerenityStepsFactory(Configuration configuration, String rootPackage, ClassLoader classLoader) {
        super(configuration, rootPackage, classLoader);
        this.configuration = configuration.usePendingStepStrategy(new FailingUponPendingStep());
    }

    @Override
    public List<CandidateSteps> createCandidateSteps() {
        super.createCandidateSteps();
        return stepsTypes()
            .stream()
            .map(type -> new Steps(configuration, type, this))
            .map(steps -> new ExtendedSerenityCandidateSteps(steps, configuration, this))
            .collect(Collectors.toList());
    }
}
