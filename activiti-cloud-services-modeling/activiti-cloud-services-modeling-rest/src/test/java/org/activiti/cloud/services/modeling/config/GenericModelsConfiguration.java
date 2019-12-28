package org.activiti.cloud.services.modeling.config;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.ALL_VALUE;

import org.activiti.cloud.modeling.api.ContentUpdateListener;
import org.activiti.cloud.modeling.api.JsonModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelExtensionsValidator;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.services.common.file.FileContent;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("generic")
@Configuration
public class GenericModelsConfiguration {

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
        public void validate(byte[] modelFile,
                             ValidationContext validationContext) {
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
        public void validate(byte[] modelFile,
                             ValidationContext validationContext) {
        }

        @Override
        public ModelType getHandledModelType() {
            return genericJsonModelType;
        }
    };

    ModelContentValidator genericNonJsonContentValidator = new ModelContentValidator() {

        @Override
        public void validate(byte[] modelFile,
                             ValidationContext validationContext) {
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
        public void validate(byte[] modelFile,
                             ValidationContext validationContext) {
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
        public void execute(Model model,
                            FileContent fileContent) {
        }
    };

    private ContentUpdateListener genericNonJsonContentUpdateListener = new ContentUpdateListener() {

        @Override
        public ModelType getHandledModelType() {
            return genericNonJsonModelType;
        }

        @Override
        public void execute(Model model,
                            FileContent fileContent) {
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

    @Bean(name = "genericNonJsonContentUpdateListener")
    public ContentUpdateListener genericNonJsonContentUpdateListener() {
        return genericNonJsonContentUpdateListener;
    }
}
