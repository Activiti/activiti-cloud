package org.activiti.cloud.api.process.model.impl.events;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrationRequestImplTest {

    @Mock
    private IntegrationContext integrationContext;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void should_always_haveAppVersionSet(){
        given(integrationContext.getAppVersion()).willReturn("1");
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        assertThat(integrationRequest.getAppVersion()).isEqualTo("1");
    }

}
