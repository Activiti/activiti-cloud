package org.activiti.cloud.services.query.model.conf;

import java.util.Map;
import org.activiti.cloud.services.query.model.dialect.CustomPostgreSQLDialect;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfiguration implements HibernatePropertiesCustomizer {

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.dialect", CustomPostgreSQLDialect.class.getName());
    }
}
