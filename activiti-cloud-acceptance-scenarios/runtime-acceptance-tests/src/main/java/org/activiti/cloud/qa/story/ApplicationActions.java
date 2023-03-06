package org.activiti.cloud.qa.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

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

import java.util.Collection;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.process.model.Deployment;
import org.activiti.api.process.model.events.ApplicationEvent;
import org.activiti.api.process.model.events.ApplicationEvent.ApplicationEvents;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ApplicationQuerySteps;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudApplication;
import org.activiti.cloud.api.process.model.events.CloudApplicationDeployedEvent;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ApplicationActions {

    @Steps
    private AuditSteps auditSteps;

    @Steps
    private ApplicationQuerySteps applicationQuerySteps;

    @When("services are started")
    public void checkServicesStatus() {
        auditSteps.checkServicesHealth();
        applicationQuerySteps.checkServicesHealth();
    }

    @Then("application deployed events are emitted on start")
    public void verifyApplicationDeployedEvents() throws Exception {
        await()
            .untilAsserted(() -> {
                Collection<CloudRuntimeEvent> events = auditSteps.getEventsByEventType(
                    ApplicationEvents.APPLICATION_DEPLOYED.name()
                );
                assertThat(events)
                    .extracting(CloudRuntimeEvent::getEventType, event -> deployment(event).getVersion())
                    .contains(tuple(ApplicationEvent.ApplicationEvents.APPLICATION_DEPLOYED, 1));
            });
    }

    @Then("the user can get applications")
    public void checkIfApplicationsArePresent() {
        Collection<CloudApplication> applications = applicationQuerySteps.getAllApplications().getContent();
        assertThat(applications).extracting(CloudApplication::getName).containsExactly("default-app");
    }

    private Deployment deployment(CloudRuntimeEvent<?, ?> event) {
        return CloudApplicationDeployedEvent.class.cast(event).getEntity();
    }
}
