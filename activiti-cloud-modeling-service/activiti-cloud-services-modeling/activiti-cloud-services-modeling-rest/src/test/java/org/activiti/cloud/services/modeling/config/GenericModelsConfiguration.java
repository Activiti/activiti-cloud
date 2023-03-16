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
package org.activiti.cloud.services.modeling.config;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collection;
import java.util.Collections;
import org.activiti.cloud.modeling.api.ContentUpdateListener;
import org.activiti.cloud.modeling.api.JsonModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelExtensionsValidator;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ModelUpdateListener;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.services.common.file.FileContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("generic")
@Configuration
public class GenericModelsConfiguration {

    private static Logger LOGGER = LoggerFactory.getLogger(GenericModelsConfiguration.class);

    JsonModelType genericJsonModelType = new JsonModelType() {
        @Override
        public String getName() {
            return "GENERIC_JSON_MODEL";
        }

        @Override
        public String getFolderName() {
            return "generic-json-model";
        }
    };

    ModelType genericNonJsonModelType = new ModelType() {
        @Override
        public String getName() {
            return "GENERIC_NON_JSON_MODEL";
        }

        @Override
        public String getFolderName() {
            return "generic-non-json-model";
        }

        @Override
        public String getContentFileExtension() {
            return ALL_VALUE;
        }

        @Override
        public String[] getAllowedContentFileExtension() {
            return new String[] { ".a", ".b" };
        }
    };

    ModelContentValidator genericJsonContentValidator = new ModelContentValidator() {
        @Override
        public Collection<ModelValidationError> validate(byte[] modelFile, ValidationContext validationContext) {
            LOGGER.info("validate generic json content");
            return Collections.emptyList();
        }

        @Override
        public void validate(
            Model model,
            byte[] modelContent,
            ValidationContext validationContext,
            boolean validateUsage
        ) {
            LOGGER.info("usage of json");
        }

        @Override
        public ModelType getHandledModelType() {
            return genericJsonModelType;
        }

        @Override
        public String getHandledContentType() {
            return APPLICATION_JSON_VALUE;
        }
    };

    private ModelExtensionsValidator genericJsonExtensionsValidator = new ModelExtensionsValidator() {
        @Override
        public Collection<ModelValidationError> validate(byte[] modelFile, ValidationContext validationContext) {
            LOGGER.info("validate generic json extensions");
            return Collections.emptyList();
        }

        @Override
        public ModelType getHandledModelType() {
            return genericJsonModelType;
        }
    };

    ModelContentValidator genericNonJsonContentValidator = new ModelContentValidator() {
        @Override
        public Collection<ModelValidationError> validate(byte[] modelFile, ValidationContext validationContext) {
            LOGGER.info("validate generic non json content");
            return Collections.emptyList();
        }

        @Override
        public ModelType getHandledModelType() {
            return genericNonJsonModelType;
        }

        @Override
        public String getHandledContentType() {
            return ALL_VALUE;
        }
    };

    private ModelExtensionsValidator genericNonJsonExtensionsValidator = new ModelExtensionsValidator() {
        @Override
        public Collection<ModelValidationError> validate(byte[] modelFile, ValidationContext validationContext) {
            LOGGER.info("validate generic non json extensions");
            return Collections.emptyList();
        }

        @Override
        public ModelType getHandledModelType() {
            return genericNonJsonModelType;
        }
    };

    private ContentUpdateListener genericJsonContentUpdateListener = new ContentUpdateListener() {
        @Override
        public ModelType getHandledModelType() {
            return genericJsonModelType;
        }

        @Override
        public void execute(Model model, FileContent fileContent) {
            LOGGER.info("generic json content update listener");
        }
    };

    private ModelUpdateListener genericJsonModelUpdateListener = new ModelUpdateListener() {
        @Override
        public ModelType getHandledModelType() {
            return genericJsonModelType;
        }

        @Override
        public void execute(Model modelToBeUpdated, Model newModel) {
            LOGGER.info("generic json model update listener");
        }
    };

    private ContentUpdateListener genericNonJsonContentUpdateListener = new ContentUpdateListener() {
        @Override
        public ModelType getHandledModelType() {
            return genericNonJsonModelType;
        }

        @Override
        public void execute(Model model, FileContent fileContent) {
            LOGGER.info("generic non json content update listener");
        }
    };

    private ModelUpdateListener genericNonJsonModelUpdateListener = new ModelUpdateListener() {
        @Override
        public ModelType getHandledModelType() {
            return genericNonJsonModelType;
        }

        @Override
        public void execute(Model modelToBeUpdated, Model newModel) {
            LOGGER.info("generic non json model update listener");
        }
    };

    @Bean(name = "genericJsonModelType")
    public JsonModelType genericJsonModelType() {
        return genericJsonModelType;
    }

    @Bean(name = "genericNonJsonModelType")
    public ModelType genericNonJsonModelType() {
        return genericNonJsonModelType;
    }

    @Bean(name = "genericJsonExtensionsValidator")
    public ModelExtensionsValidator genericExtensionsValidator() {
        return genericJsonExtensionsValidator;
    }

    @Bean(name = "genericNonJsonExtensionsValidator")
    public ModelExtensionsValidator genericNonJsonExtensionsValidator() {
        return genericNonJsonExtensionsValidator;
    }

    @Bean(name = "genericJsonContentValidator")
    public ModelContentValidator genericJsonContentValidator() {
        return genericJsonContentValidator;
    }

    @Bean(name = "genericNonJsonContentValidator")
    public ModelContentValidator genericNonJsonContentValidator() {
        return genericNonJsonContentValidator;
    }

    @Bean(name = "genericJsonContentUpdateListener")
    public ContentUpdateListener genericJsonContentUpdateListener() {
        return genericJsonContentUpdateListener;
    }

    @Bean(name = "genericJsonModelUpdateListener")
    public ModelUpdateListener genericJsonModelUpdateListener() {
        return genericJsonModelUpdateListener;
    }

    @Bean(name = "genericNonJsonContentUpdateListener")
    public ContentUpdateListener genericNonJsonContentUpdateListener() {
        return genericNonJsonContentUpdateListener;
    }

    @Bean(name = "genericNonJsonModelUpdateListener")
    public ModelUpdateListener genericNonJsonModelUpdateListener() {
        return genericNonJsonModelUpdateListener;
    }
}
