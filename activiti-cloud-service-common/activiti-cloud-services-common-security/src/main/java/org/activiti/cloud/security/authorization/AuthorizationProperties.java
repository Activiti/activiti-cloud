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
package org.activiti.cloud.security.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "authorizations")
public class AuthorizationProperties {

    private List<SecurityConstraint> securityConstraints = new ArrayList();

    public List<SecurityConstraint> getSecurityConstraints() {
        return securityConstraints;
    }

    public void setSecurityConstraints(List<SecurityConstraint> securityConstraints) {
        this.securityConstraints = securityConstraints;
    }

    @ConfigurationProperties
    public static class SecurityConstraint {

        private String[] authRoles = new String[] {};
        private SecurityCollection[] securityCollections = new SecurityCollection[] {};

        public String[] getAuthRoles() {
            return this.authRoles;
        }

        public void setAuthRoles(String[] authRoles) {
            this.authRoles = authRoles;
        }

        public SecurityCollection[] getSecurityCollections() {
            return this.securityCollections;
        }

        public void setSecurityCollections(SecurityCollection[] securityCollections) {
            this.securityCollections = securityCollections;
        }
    }

    @ConfigurationProperties
    public static class SecurityCollection {

        private String[] patterns = new String[] {};
        private String[] omittedMethods = new String[] {};

        public String[] getPatterns() {
            return patterns;
        }

        public void setPatterns(String[] patterns) {
            this.patterns = patterns;
        }

        public String[] getOmittedMethods() {
            return omittedMethods;
        }

        public void setOmittedMethods(String[] omittedMethods) {
            this.omittedMethods = omittedMethods;
        }

        @Override
        public String toString() {
            return (
                "{" +
                "patterns=" +
                Arrays.toString(patterns) +
                ", omittedMethods=" +
                Arrays.toString(omittedMethods) +
                '}'
            );
        }
    }
}
