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
package org.activiti.cloud.common.messaging.spring.integration;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.messaging.MessageChannel;

public class ExpressionAdviceBuilder {

    private final ExpressionEvaluatingRequestHandlerAdvice advice;

    private ExpressionAdviceBuilder() {
        this.advice = new ExpressionEvaluatingRequestHandlerAdvice();
    }

    /**
     * Start building the advice instance.
     */
    public static ExpressionAdviceBuilder create() {
        return new ExpressionAdviceBuilder();
    }

    /**
     * Set the Spring BeanFactory required to compile SpEL expressions.
     */
    public ExpressionAdviceBuilder beanFactory(BeanFactory beanFactory) {
        this.advice.setBeanFactory(beanFactory);
        return this;
    }

    /**
     * Configure success channel and an optional SpEL string expression.
     */
    public ExpressionAdviceBuilder onSuccess(MessageChannel successChannel, String expressionString) {
        this.advice.setSuccessChannel(successChannel);
        if (expressionString != null) {
            this.advice.setOnSuccessExpressionString(expressionString);
        }
        return this;
    }

    public ExpressionAdviceBuilder onSuccess(MessageChannel successChannel, Expression expression) {
        this.advice.setSuccessChannel(successChannel);
        if (expression != null) {
            this.advice.setOnSuccessExpression(expression);
        }
        return this;
    }

    /**
     * Configure failure channel and an optional SpEL string expression.
     * The thrown exception is accessible via the '#exception' variable.
     */
    public ExpressionAdviceBuilder onFailure(MessageChannel failureChannel, String expressionString) {
        this.advice.setFailureChannel(failureChannel);
        if (expressionString != null) {
            this.advice.setOnFailureExpressionString(expressionString);
        }
        return this;
    }

    public ExpressionAdviceBuilder onFailure(MessageChannel failureChannel, Expression expression) {
        this.advice.setFailureChannel(failureChannel);
        if (expression != null) {
            this.advice.setOnFailureExpression(expression);
        }
        return this;
    }

    public ExpressionAdviceBuilder onFailureExpressionString(String expressionString) {
        this.advice.setOnFailureExpressionString(expressionString);
        return this;
    }

    /**
     * Define whether to trap exceptions (true) or let them bubble up to the caller (false).
     */
    public ExpressionAdviceBuilder trapException(boolean trapException) {
        this.advice.setTrapException(trapException);
        return this;
    }

    /**
     * Define whether to return the evaluation result of the failure expression to the caller.
     */
    public ExpressionAdviceBuilder returnFailureExpressionResult(boolean returnResult) {
        this.advice.setReturnFailureExpressionResult(returnResult);
        return this;
    }

    /**
     * Finalize construction and return the advice.
     */
    public ExpressionEvaluatingRequestHandlerAdvice build() {
        return this.advice;
    }
}
