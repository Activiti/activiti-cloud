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

import static org.jbehave.core.steps.AbstractStepResult.failed;
import static org.jbehave.core.steps.AbstractStepResult.successful;

import java.lang.reflect.Method;
import java.util.Map;
import net.thucydides.core.steps.StepEventBus;
import org.activiti.cloud.acc.shared.rest.error.ExpectRestError;
import org.activiti.cloud.acc.shared.rest.error.ExpectRestNotFound;
import org.activiti.cloud.acc.shared.rest.error.ExpectedRestException;
import org.activiti.cloud.acc.shared.serenity.exception.ExpectException;
import org.activiti.cloud.acc.shared.serenity.exception.ExpectedException;
import org.activiti.cloud.acc.shared.serenity.exception.ExpectedExceptionHandler;
import org.activiti.cloud.acc.shared.serenity.exception.ExpectedExceptionNotThrown;
import org.jbehave.core.configuration.Keywords;
import org.jbehave.core.failures.UUIDExceptionWrapper;
import org.jbehave.core.parsers.StepMatcher;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.ParameterControls;
import org.jbehave.core.steps.ParameterConverters;
import org.jbehave.core.steps.Step;
import org.jbehave.core.steps.StepCreator;
import org.jbehave.core.steps.StepMonitor;
import org.jbehave.core.steps.StepResult;
import org.jbehave.core.steps.Timer;
import org.jbehave.core.steps.context.StepsContext;

/**
 * Extended StepCreator that can create ExpectingExceptionParametrisedStep if ExpectingException is detected
 */
public class ExtendedStepCreator extends StepCreator {

    private Keywords keywords;

    public ExtendedStepCreator(
        Class<?> stepsType,
        InjectableStepsFactory stepsFactory,
        StepsContext stepsContext,
        ParameterConverters parameterConverters,
        ParameterControls parameterControls,
        StepMatcher stepMatcher,
        StepMonitor stepMonitor,
        Keywords keywords
    ) {
        super(stepsType, stepsFactory, stepsContext, parameterConverters, parameterControls, stepMatcher, stepMonitor);
        this.keywords = keywords;
    }

    @Override
    public Step createParametrisedStep(
        Method method,
        String stepAsString,
        String stepWithoutStartingWord,
        Map<String, String> namedParameters
    ) {
        ExpectedException expectedException = getExpectedException(method);
        return expectedException != null
            ? new ExpectingExceptionParametrisedStep(
                stepAsString,
                method,
                stepWithoutStartingWord,
                namedParameters,
                getKeywords(),
                expectedException
            )
            : super.createParametrisedStep(method, stepAsString, stepWithoutStartingWord, namedParameters);
    }

    /**
     * Get the expected exception for a given method.
     * @param method the method
     * @return the expected exception
     */
    protected ExpectedException getExpectedException(final Method method) {
        ExpectedException expectedException = null;

        ExpectException expectException = method.getAnnotation(ExpectException.class);
        if (expectException != null) {
            expectedException = new ExpectedException(expectException.value());
        }

        ExpectRestError expectRestError = method.getAnnotation(ExpectRestError.class);
        if (expectRestError != null) {
            expectedException = new ExpectedRestException(expectRestError.statusCode(), expectRestError.value());
        }

        ExpectRestNotFound expectRestNotFound = method.getAnnotation(ExpectRestNotFound.class);
        if (expectRestNotFound != null) {
            expectedException = new ExpectedRestException(expectRestNotFound.statusCode(), expectRestNotFound.value());
        }
        return expectedException;
    }

    public Keywords getKeywords() {
        return keywords;
    }

    /**
     * Extended ParametrisedStep for steps that is expecting to throw an exception
     */
    class ExpectingExceptionParametrisedStep extends ParametrisedStep {

        private String stepAsString;

        private Keywords keywords;

        private ExpectedException expectedException;

        public ExpectingExceptionParametrisedStep(
            String stepAsString,
            Method method,
            String stepWithoutStartingWord,
            Map<String, String> namedParameters,
            Keywords keywords,
            ExpectedException expectedException
        ) {
            super(stepAsString, method, stepWithoutStartingWord, namedParameters);
            this.stepAsString = stepAsString;
            this.keywords = keywords;
            this.expectedException = expectedException;
        }

        @Override
        public StepResult perform(UUIDExceptionWrapper storyFailureIfItHappened) {
            if (expectedException == null) {
                return super.perform(storyFailureIfItHappened);
            }

            Timer timer = new Timer().start();

            boolean isExpectedExceptionThrown = ExpectedExceptionHandler.isThrowingExpectedException(
                expectedException,
                () -> performWithThrowing(storyFailureIfItHappened)
            );

            if (!isExpectedExceptionThrown) {
                ExpectedExceptionNotThrown failureCause = new ExpectedExceptionNotThrown(
                    "The exception was not thrown as expected: " + expectedException,
                    expectedException
                );
                return failed(stepAsString, new UUIDExceptionWrapper(stepAsString, failureCause))
                    .withParameterValues(asString(keywords))
                    .setTimings(timer.stop());
            }

            StepEventBus.getEventBus().getBaseStepListener().exceptionExpected(ExpectedException.class);

            return successful(stepAsString).withParameterValues(asString(keywords)).setTimings(timer.stop());
        }

        /**
         * Perform normally the step but throw whatever failure throwable is caught.
         * @param storyFailureIfItHappened
         * @throws Throwable any throwable
         */
        protected void performWithThrowing(UUIDExceptionWrapper storyFailureIfItHappened) throws Throwable {
            StepResult result = super.perform(storyFailureIfItHappened);
            if (result.getFailure() != null) {
                throw result.getFailure().getCause();
            }
        }
    }
}
