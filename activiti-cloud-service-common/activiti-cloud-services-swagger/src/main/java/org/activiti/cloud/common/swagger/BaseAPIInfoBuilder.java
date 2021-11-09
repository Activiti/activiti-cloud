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

package org.activiti.cloud.common.swagger;

import org.springframework.boot.info.BuildProperties;
import springfox.documentation.builders.ApiInfoBuilder;

public class BaseAPIInfoBuilder {

    private BuildProperties buildProperties;

    public BaseAPIInfoBuilder(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    public ApiInfoBuilder baseApiInfoBuilder(String title) {
        return new ApiInfoBuilder()
            .title(title)
            .version(buildProperties.getVersion())
            .license(String.format("© %s-%s %s. All rights reserved",
                buildProperties.get("inceptionYear"),
                buildProperties.get("year"),
                buildProperties.get("organization.name")))
            .termsOfServiceUrl(buildProperties.get("organization.url"));
    }

}
