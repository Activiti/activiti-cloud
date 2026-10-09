/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.services.connectors.mtc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MtcIntegrationRequestInterceptorTest {

    @Test
    void shouldMatchPerAppKeyViaTypeMapping() {
        MtcProperties props = new MtcProperties();
        props.setEnabled(true);
        props.setConnectorTypes(Set.of("rest-connector"));
        props.setTypeMappings(Map.of("r-h1h2j", "rest-connector"));

        MtcIntegrationRequestInterceptor interceptor = new MtcIntegrationRequestInterceptor(props);

        assertThat(interceptor.isMtcEnabled("r-h1h2j.GET")).isTrue();
        assertThat(interceptor.resolveMtcDestination("r-h1h2j.GET")).isEqualTo("mtc-rest-connector.GET");
        assertThat(interceptor.resolveConnectorType("r-h1h2j.GET")).isEqualTo("rest-connector.GET");
    }

    @Test
    void shouldNotMatchWhenTypeMappingMissing() {
        MtcProperties props = new MtcProperties();
        props.setEnabled(true);
        props.setConnectorTypes(Set.of("rest-connector"));
        props.setTypeMappings(Map.of());

        MtcIntegrationRequestInterceptor interceptor = new MtcIntegrationRequestInterceptor(props);

        assertThat(interceptor.isMtcEnabled("r-h1h2j.GET")).isFalse();
    }

    @Test
    void shouldFallBackToPrefixMatchWithoutTypeMappings() {
        MtcProperties props = new MtcProperties();
        props.setEnabled(true);
        props.setConnectorTypes(Set.of("rest-connector"));
        props.setTypeMappings(Map.of());

        MtcIntegrationRequestInterceptor interceptor = new MtcIntegrationRequestInterceptor(props);

        assertThat(interceptor.isMtcEnabled("rest-connector.GET")).isTrue();
        assertThat(interceptor.resolveMtcDestination("rest-connector.GET")).isEqualTo("mtc-rest-connector.GET");
        assertThat(interceptor.resolveConnectorType("rest-connector.GET")).isEqualTo("rest-connector.GET");
    }

    @Test
    void shouldReturnFalseWhenDisabled() {
        MtcProperties props = new MtcProperties();
        props.setEnabled(false);
        props.setConnectorTypes(Set.of("rest-connector"));
        props.setTypeMappings(Map.of("r-h1h2j", "rest-connector"));

        MtcIntegrationRequestInterceptor interceptor = new MtcIntegrationRequestInterceptor(props);

        assertThat(interceptor.isMtcEnabled("r-h1h2j.GET")).isFalse();
    }

    @Test
    void shouldHandleConnectorTypeWithoutMethodSuffix() {
        MtcProperties props = new MtcProperties();
        props.setEnabled(true);
        props.setConnectorTypes(Set.of("rest-connector"));
        props.setTypeMappings(Map.of("r-h1h2j", "rest-connector"));

        MtcIntegrationRequestInterceptor interceptor = new MtcIntegrationRequestInterceptor(props);

        assertThat(interceptor.isMtcEnabled("r-h1h2j")).isTrue();
        assertThat(interceptor.resolveMtcDestination("r-h1h2j")).isEqualTo("mtc-rest-connector");
        assertThat(interceptor.resolveConnectorType("r-h1h2j")).isEqualTo("rest-connector");
    }

    @Test
    void shouldNotMatchWhenTemplateNotInConnectorTypes() {
        MtcProperties props = new MtcProperties();
        props.setEnabled(true);
        props.setConnectorTypes(Set.of("email-connector"));
        props.setTypeMappings(Map.of("r-h1h2j", "rest-connector"));

        MtcIntegrationRequestInterceptor interceptor = new MtcIntegrationRequestInterceptor(props);

        assertThat(interceptor.isMtcEnabled("r-h1h2j.GET")).isFalse();
    }
}
