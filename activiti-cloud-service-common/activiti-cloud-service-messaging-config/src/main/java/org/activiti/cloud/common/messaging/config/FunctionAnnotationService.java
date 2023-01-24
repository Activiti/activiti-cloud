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

package org.activiti.cloud.common.messaging.config;

import java.lang.annotation.Annotation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

public class FunctionAnnotationService {

    private static final Logger log = LoggerFactory.getLogger(FunctionBindingConfiguration.class);

    private DefaultListableBeanFactory beanFactory;

    public FunctionAnnotationService(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Nullable
    public <T extends Annotation> T findAnnotationOnBean(String beanName, Class<T> annotationType) {
        try {
            return beanFactory.findAnnotationOnBean(beanName, annotationType);
        } catch (NoSuchBeanDefinitionException e) {
            log.warn("Bean with name {} not found.", beanName);
            return null;
        }
    }
}