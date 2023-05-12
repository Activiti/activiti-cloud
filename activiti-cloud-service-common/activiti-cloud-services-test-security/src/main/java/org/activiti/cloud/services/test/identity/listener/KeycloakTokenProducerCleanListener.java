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

package org.activiti.cloud.services.test.identity.listener;

import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class KeycloakTokenProducerCleanListener implements TestExecutionListener, Ordered {

    private Logger logger = LoggerFactory.getLogger(KeycloakTokenProducerCleanListener.class);

    private static final String RESOURCE = "keycloak.resource";
    private static final String TEST_USER = "activiti.identity.test-user";
    private static final String TEST_PASSWORD = "activiti.identity.test-password";

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        try {
            IdentityTokenProducer tokenProducer = testContext
                .getApplicationContext()
                .getBean(IdentityTokenProducer.class);
            Environment env = testContext.getApplicationContext().getEnvironment();
            tokenProducer
                .withTestUser(env.getProperty(TEST_USER, ""))
                .withTestPassword(env.getProperty(TEST_PASSWORD, ""))
                .withResource(env.getProperty(RESOURCE, ""));
        } catch (BeansException e) {
            logger.debug(() -> "No bean of type IdentityTokenProducer: skipping.");
        }
    }
}
