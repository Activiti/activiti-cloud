package org.activiti.cloud.services.modeling.validation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties({ExecutableMimeTypeProperties.class})
@PropertySource("classpath:executable-mime-type-list.properties")
public class FileContentValidatorConfiguration {

    @Bean
    public FileContentValidator fileContentValidator(ExecutableMimeTypeProperties executableMimeTypeProperties) {
        return new FileContentValidator(executableMimeTypeProperties.getMimeTypes());
    }

}
