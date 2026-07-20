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
package org.activiti.cloud.services.query.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import org.activiti.api.process.model.ExternalizedContextReference;

@Embeddable
public class ExternalizedContextReferenceEmbeddable implements ExternalizedContextReference {

    @Column(name = "externalized_data_provider_type")
    private String providerType;

    @Column(name = "externalized_data_url")
    private String url;

    public ExternalizedContextReferenceEmbeddable() {}

    public ExternalizedContextReferenceEmbeddable(String providerType, String url) {
        this.providerType = providerType;
        this.url = url;
    }

    public ExternalizedContextReferenceEmbeddable(ExternalizedContextReference other) {
        this.providerType = other.getProviderType();
        this.url = other.getUrl();
    }

    @Override
    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerType, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ExternalizedContextReferenceEmbeddable other = (ExternalizedContextReferenceEmbeddable) obj;
        return Objects.equals(providerType, other.providerType) && Objects.equals(url, other.url);
    }

    @Override
    public String toString() {
        return "ExternalizedContextReferenceEmbeddable [providerType=" + providerType + ", url=" + url + "]";
    }
}
