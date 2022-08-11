package org.activiti.cloud.services.modeling.validation.magicnumber;

import org.activiti.cloud.services.modeling.validation.magicnumber.FileMagicNumber.FileMagicNumberList;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties(FileMagicNumberList.class)
@PropertySource("classpath:executable-magic-numbers.properties")
public class FileContentValidatorConfiguration {

    @Bean
    public FileMagicNumberValidator fileContentValidator(FileMagicNumberList fileMagicNumber) {
        return new FileMagicNumberValidator(fileMagicNumber.getMagicNumber());
    }

}
