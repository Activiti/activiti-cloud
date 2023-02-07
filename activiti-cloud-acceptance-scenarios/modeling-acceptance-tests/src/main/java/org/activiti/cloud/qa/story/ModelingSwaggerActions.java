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
package org.activiti.cloud.qa.story;

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.acc.modeling.steps.SwaggerModelingSteps;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ModelingSwaggerActions {

    @Steps
    private SwaggerModelingSteps swaggerModelingSteps;

    private String swaggerSpecification;

    @When("the user asks for swagger specification")
    public void getSwaggerSpecification() {
        swaggerSpecification = swaggerModelingSteps.getSwaggerSpecification();
    }

    @Then("the user gets swagger specification following Alfresco MediaType")
    public void isFollowingAlfrescoMediaType() {
        assertThat(swaggerSpecification)
            .contains("ListResponseContent")
            .contains("EntriesResponseContent")
            .contains("EntryResponseContent")
            .doesNotContain("PagedModel")
            .doesNotContain("ResourcesResource")
            .doesNotContain("\"Resource\"");
    }
}
