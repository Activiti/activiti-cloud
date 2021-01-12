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

package org.activiti.cloud.service.common.batch.util.core.tasklet;

import static org.springframework.batch.repeat.RepeatStatus.FINISHED;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PropertyResolverConsumerTasklet implements Tasklet, StepExecutionListener {

    private final Environment environment;
    private final Consumer<PropertyResolver> propertyResolverConsumer;
    private StepExecution stepExecution;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        propertyResolverConsumer.accept(
            new PropertySourcesPropertyResolver(
                propertySources(stepExecution.getJobExecution()
                                             .getJobConfigurationName(),
                                stepExecution.getJobParameters()
                                             .toProperties(),
                                environment)));
        return FINISHED;
    }

    private PropertySources propertySources(String propertyName, Properties properties, Environment env) {
        MutablePropertySources propertySources = new MutablePropertySources();
        if (properties != null)
            propertySources.addFirst(new PropertiesPropertySource(Optional.ofNullable(propertyName).orElse("jobConfig"),
                                                                  properties));
        ((AbstractEnvironment) env).getPropertySources().forEach(propertySources::addLast);
        return propertySources;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }
}
