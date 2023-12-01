/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.metadata;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataService {

    private MetadataProperties metadataProperties;

    private final String servicePrefix = "activiti-cloud-service-";
    private final String applicationPrefix = "activiti-cloud-application-";

    @Autowired
    public MetadataService(MetadataProperties metadataProperties) {
        this.metadataProperties = metadataProperties;
    }

    public String getKeyAsMetaData(String key, String keyPrefix) {
        return keyPrefix + key.replace(".", "-");
    }

    public Map<String, String> getMetadata() {
        Map<String, String> metadata = new HashMap<>();

        Iterator<String> applicationIterator = metadataProperties.getApplication().keySet().iterator();
        while (applicationIterator.hasNext()) {
            String key = applicationIterator.next();
            if (metadataProperties.getApplication().get(key) != null) {
                metadata.put(getKeyAsMetaData(key, applicationPrefix), metadataProperties.getApplication().get(key));
            }
        }

        Iterator<String> serviceIterator = metadataProperties.getService().keySet().iterator();
        while (serviceIterator.hasNext()) {
            String key = serviceIterator.next();
            if (metadataProperties.getService().get(key) != null) {
                metadata.put(getKeyAsMetaData(key, servicePrefix), metadataProperties.getService().get(key));
            }
        }
        return metadata;
    }
}
