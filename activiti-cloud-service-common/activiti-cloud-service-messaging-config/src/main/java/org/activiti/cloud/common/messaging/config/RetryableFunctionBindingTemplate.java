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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.BackOffPolicyBuilder;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.ExpressionRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;

public class RetryableFunctionBindingTemplate implements Function<Retryable, RetryTemplate>, BeanFactoryAware {
    private static final TemplateParserContext PARSER_CONTEXT = new TemplateParserContext();

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

    private BeanFactory beanFactory;

    @Override
    public RetryTemplate apply(Retryable retryable) {
        String[] listenersBeanNames = retryable.listeners();

        RetryTemplate template = new RetryTemplate();
        if (listenersBeanNames.length > 0) {
            template.setListeners(getListenersBeans(listenersBeanNames));
        }
//        else if (this.globalListeners != null) {
//            template.setListeners(this.globalListeners);
//        }

        template.setRetryPolicy(getRetryPolicy(retryable));
        template.setBackOffPolicy(getBackoffPolicy(retryable.backoff()));

        return template;
    }

    private RetryListener[] getListenersBeans(String[] listenersBeanNames) {
        RetryListener[] listeners = new RetryListener[listenersBeanNames.length];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = this.beanFactory.getBean(listenersBeanNames[i], RetryListener.class);
        }
        return listeners;
    }

    private RetryPolicy getRetryPolicy(Annotation retryable) {
        Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(retryable);
        @SuppressWarnings("unchecked")
        Class<? extends Throwable>[] includes = (Class<? extends Throwable>[]) attrs.get("value");
        String exceptionExpression = (String) attrs.get("exceptionExpression");
        boolean hasExpression = StringUtils.hasText(exceptionExpression);
        if (includes.length == 0) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable>[] value = (Class<? extends Throwable>[]) attrs.get("include");
            includes = value;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Throwable>[] excludes = (Class<? extends Throwable>[]) attrs.get("exclude");
        Integer maxAttempts = (Integer) attrs.get("maxAttempts");
        String maxAttemptsExpression = (String) attrs.get("maxAttemptsExpression");
        if (StringUtils.hasText(maxAttemptsExpression)) {
            if (ExpressionRetryPolicy.isTemplate(maxAttemptsExpression)) {
                maxAttempts = PARSER.parseExpression(resolve(maxAttemptsExpression), PARSER_CONTEXT)
                                    .getValue(this.evaluationContext, Integer.class);
            }
            else {
                maxAttempts = PARSER.parseExpression(resolve(maxAttemptsExpression)).getValue(this.evaluationContext,
                                                                                              Integer.class);
            }
        }
        if (includes.length == 0 && excludes.length == 0) {
            SimpleRetryPolicy simple = hasExpression
                    ? new ExpressionRetryPolicy(resolve(exceptionExpression)).withBeanFactory(this.beanFactory)
                    : new SimpleRetryPolicy();
            simple.setMaxAttempts(maxAttempts);
            return simple;
        }
        Map<Class<? extends Throwable>, Boolean> policyMap = new HashMap<Class<? extends Throwable>, Boolean>();
        for (Class<? extends Throwable> type : includes) {
            policyMap.put(type, true);
        }
        for (Class<? extends Throwable> type : excludes) {
            policyMap.put(type, false);
        }
        boolean retryNotExcluded = includes.length == 0;
        if (hasExpression) {
            return new ExpressionRetryPolicy(maxAttempts, policyMap, true, exceptionExpression, retryNotExcluded)
                    .withBeanFactory(this.beanFactory);
        }
        else {
            return new SimpleRetryPolicy(maxAttempts, policyMap, true, retryNotExcluded);
        }
    }

    private BackOffPolicy getBackoffPolicy(Backoff backoff) {
        Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(backoff);
        long min = backoff.delay() == 0 ? backoff.value() : backoff.delay();
        String delayExpression = (String) attrs.get("delayExpression");
        if (StringUtils.hasText(delayExpression)) {
            if (ExpressionRetryPolicy.isTemplate(delayExpression)) {
                min = PARSER.parseExpression(resolve(delayExpression), PARSER_CONTEXT).getValue(this.evaluationContext,
                                                                                                Long.class);
            }
            else {
                min = PARSER.parseExpression(resolve(delayExpression)).getValue(this.evaluationContext, Long.class);
            }
        }
        long max = backoff.maxDelay();
        String maxDelayExpression = (String) attrs.get("maxDelayExpression");
        if (StringUtils.hasText(maxDelayExpression)) {
            if (ExpressionRetryPolicy.isTemplate(maxDelayExpression)) {
                max = PARSER.parseExpression(resolve(maxDelayExpression), PARSER_CONTEXT)
                            .getValue(this.evaluationContext, Long.class);
            }
            else {
                max = PARSER.parseExpression(resolve(maxDelayExpression)).getValue(this.evaluationContext, Long.class);
            }
        }
        double multiplier = backoff.multiplier();
        String multiplierExpression = (String) attrs.get("multiplierExpression");
        if (StringUtils.hasText(multiplierExpression)) {
            if (ExpressionRetryPolicy.isTemplate(multiplierExpression)) {
                multiplier = PARSER.parseExpression(resolve(multiplierExpression), PARSER_CONTEXT)
                                   .getValue(this.evaluationContext, Double.class);
            }
            else {
                multiplier = PARSER.parseExpression(resolve(multiplierExpression)).getValue(this.evaluationContext,
                                                                                            Double.class);
            }
        }
        boolean isRandom = false;
        if (multiplier > 0) {
            isRandom = backoff.random();
            String randomExpression = (String) attrs.get("randomExpression");
            if (StringUtils.hasText(randomExpression)) {
                if (ExpressionRetryPolicy.isTemplate(randomExpression)) {
                    isRandom = PARSER.parseExpression(resolve(randomExpression), PARSER_CONTEXT)
                                     .getValue(this.evaluationContext, Boolean.class);
                }
                else {
                    isRandom = PARSER.parseExpression(resolve(randomExpression)).getValue(this.evaluationContext,
                                                                                          Boolean.class);
                }
            }
        }
        return min == 0 ? new NoBackOffPolicy() : BackOffPolicyBuilder.newBuilder()
                                                                      .delay(min)
                                                                      .maxDelay(max)
                                                                      .multiplier(multiplier)
                                                                      .random(isRandom)
                                                                      .build();
    }

    /**
     * Resolve the specified value if possible.
     *
     * @see ConfigurableBeanFactory#resolveEmbeddedValue
     */
    private String resolve(String value) {
        if (this.beanFactory != null && this.beanFactory instanceof ConfigurableBeanFactory) {
            return ((ConfigurableBeanFactory) this.beanFactory).resolveEmbeddedValue(value);
        }
        return value;
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
