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

import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mtc.connector")
public class MtcProperties {

    private boolean enabled = false;
    private String exchangePrefix = "mtc-";
    private String binderName = "mtc";
    private Set<String> connectorTypes = Set.of();
    private Map<String, String> typeMappings = Map.of();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExchangePrefix() {
        return exchangePrefix;
    }

    public void setExchangePrefix(String exchangePrefix) {
        this.exchangePrefix = exchangePrefix;
    }

    public String getBinderName() {
        return binderName;
    }

    public void setBinderName(String binderName) {
        this.binderName = binderName;
    }

    public Set<String> getConnectorTypes() {
        return connectorTypes;
    }

    public void setConnectorTypes(Set<String> connectorTypes) {
        this.connectorTypes = connectorTypes;
    }

    public Map<String, String> getTypeMappings() {
        return typeMappings;
    }

    public void setTypeMappings(Map<String, String> typeMappings) {
        this.typeMappings = typeMappings;
    }
}
