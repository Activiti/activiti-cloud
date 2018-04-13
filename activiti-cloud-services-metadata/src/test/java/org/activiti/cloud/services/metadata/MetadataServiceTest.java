package org.activiti.cloud.services.metadata;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class MetadataServiceTest {


    @InjectMocks
    private MetadataService metadataService;

    @Mock
    private MetadataProperties metadataProperties;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        HashMap<String,String> application = new HashMap<>();
        application.put("name","app");
        application.put("version","1");


        HashMap<String,String> service = new HashMap<>();
        service.put("name","rb");
        service.put("version","2");


        when(metadataProperties.getApplication()).thenReturn(application);
        when(metadataProperties.getService()).thenReturn(service);

    }



    @Test
    public void shouldGetMetaData() throws Exception {

        Map<String,String> metaData = metadataService.getMetadata();

        assertThat(metaData.keySet()).hasSize(4);
        assertThat(metaData.keySet()).contains("activiti-cloud-service-name");
        assertThat(metaData.keySet()).contains("activiti-cloud-service-version");
        assertThat(metaData.keySet()).contains("activiti-cloud-application-name");
        assertThat(metaData.keySet()).contains("activiti-cloud-application-version");

        assertThat(metaData.values()).contains("1");
        assertThat(metaData.values()).contains("app");
    }

}
