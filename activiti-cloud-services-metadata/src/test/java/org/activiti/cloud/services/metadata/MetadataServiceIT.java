package org.activiti.cloud.services.metadata;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource("classpath:propstest.properties")
public class MetadataServiceIT {

    @Autowired
    private MetadataService metadataService;


    @Test
    public void shouldGetMetaData() throws Exception {

        Map<String,String> metaData = metadataService.getMetadata();

        assertThat(metaData.keySet()).hasSize(5);
        assertThat(metaData.keySet()).contains("activiti-cloud-service-name");
        assertThat(metaData.keySet()).contains("activiti-cloud-service-version");
        assertThat(metaData.keySet()).contains("activiti-cloud-application-name");
        assertThat(metaData.keySet()).contains("activiti-cloud-application-version");
        assertThat(metaData.keySet()).contains("activiti-cloud-service-short-name");

        assertThat(metaData.values()).contains("1");
        assertThat(metaData.values()).contains("app");
    }
}
