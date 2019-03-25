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

package org.activiti.cloud.qa.story;

import java.util.HashMap;
import java.util.Map;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.acc.core.steps.audit.SwaggerAuditSteps;
import org.activiti.cloud.acc.core.steps.query.SwaggerQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.SwaggerRuntimeBundleSteps;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import static org.assertj.core.api.Assertions.assertThat;

public class SwaggerActions {

    @Steps
    private SwaggerRuntimeBundleSteps swaggerRuntimeBundleSteps;

    @Steps
    private SwaggerQuerySteps swaggerQuerySteps;

    @Steps
    private SwaggerAuditSteps swaggerAuditSteps;

    private Map<SwaggerSpecifications, String> swaggerSpecifications = new HashMap<>();

    private enum SwaggerSpecifications {
        RUNTIME_BUNDLE,
        QUERY,
        AUDIT
    }

    @When("the user asks for swagger specification")
    public void getSwaggerSpecification(){
        swaggerSpecifications.put(SwaggerSpecifications.RUNTIME_BUNDLE, swaggerRuntimeBundleSteps.getSwaggerSpecification());
        swaggerSpecifications.put(SwaggerSpecifications.QUERY, swaggerQuerySteps.getSwaggerSpecification());
        swaggerSpecifications.put(SwaggerSpecifications.AUDIT, swaggerAuditSteps.getSwaggerSpecification());
    }

    @Then("the user gets swagger specification following Alfresco MediaType")
    public void isFollowingAlfrescoMediaType(){
        assertThat(swaggerSpecifications.get(SwaggerSpecifications.RUNTIME_BUNDLE))
                .contains("ListResponseContent«CloudProcessDefinition»")
                .contains("EntriesResponseContent«CloudProcessDefinition»")
                .contains("EntryResponseContent«CloudProcessDefinition»")
                .contains("payloadType")
                .doesNotContain("PagedResources«")
                .doesNotContain("Resources«Resource«")
                .doesNotContain("Resource«");

        assertThat(swaggerSpecifications.get(SwaggerSpecifications.QUERY))
                .contains("ListResponseContent«CloudProcessDefinition»")
                .contains("EntriesResponseContent«CloudProcessDefinition»")
                .contains("EntryResponseContent«CloudProcessDefinition»")
                .doesNotContain("PagedResources«")
                .doesNotContain("Resources«Resource«")
                .doesNotContain("Resource«");

        assertThat(swaggerSpecifications.get(SwaggerSpecifications.AUDIT))
                .contains("ListResponseContent«CloudRuntimeEvent»")
                .contains("EntriesResponseContent«CloudRuntimeEvent»")
                .contains("EntryResponseContent«CloudRuntimeEvent»")
                .doesNotContain("PagedResources«")
                .doesNotContain("Resources«Resource«")
                .doesNotContain("Resource«");
    }

}
