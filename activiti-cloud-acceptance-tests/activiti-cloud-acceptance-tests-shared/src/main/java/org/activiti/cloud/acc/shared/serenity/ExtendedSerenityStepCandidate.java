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

import java.util.Map;
import net.serenitybdd.jbehave.SerenityStepCandidate;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.Keywords;
import org.jbehave.core.parsers.RegexPrefixCapturingPatternParser;
import org.jbehave.core.parsers.StepMatcher;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.Step;
import org.jbehave.core.steps.StepCandidate;
import org.jbehave.core.steps.StepCreator;

/**
 * Extended SerenityStepCandidate
 */
public class ExtendedSerenityStepCandidate extends SerenityStepCandidate {

    private StepCreator stepCreator;

    private Keywords keywords;

    public ExtendedSerenityStepCandidate(
        StepCandidate stepCandidate,
        Configuration configuration,
        InjectableStepsFactory stepsFactory
    ) {
        super(stepCandidate);
        keywords = configuration.keywords();

        StepMatcher stepMatcher = new RegexPrefixCapturingPatternParser()
            .parseStep(getStepType(), stepCandidate.getPatternAsString());
        this.stepCreator =
            new ExtendedStepCreator(
                getStepsType(),
                stepsFactory,
                configuration.stepsContext(),
                configuration.parameterConverters(),
                configuration.parameterControls(),
                stepMatcher,
                configuration.stepMonitor(),
                keywords
            );
    }

    @Override
    public Step createMatchedStep(String stepAsString, Map<String, String> namedParameters) {
        String stepWithoutStartingWord = keywords.stepWithoutStartingWord(stepAsString, getStepType());
        return stepCreator.createParametrisedStep(getMethod(), stepAsString, stepWithoutStartingWord, namedParameters);
    }

    protected StepCreator getStepCreator() {
        return stepCreator;
    }
}
