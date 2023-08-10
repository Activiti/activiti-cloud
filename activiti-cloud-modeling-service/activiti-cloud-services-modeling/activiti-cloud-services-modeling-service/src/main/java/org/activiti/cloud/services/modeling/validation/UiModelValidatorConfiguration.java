package org.activiti.cloud.services.modeling.validation;

import org.activiti.cloud.modeling.api.UIModelType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class UiModelValidatorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UiModelValidator uiModelValidator(UIModelType uiModelType) {
        return new UiModelValidator(null, uiModelType);
    }
}
