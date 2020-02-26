/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.notifications.graphql.events;

import java.lang.annotation.Annotation;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SpELTemplateRoutingKeyResolver implements RoutingKeyResolver {

	private ExpressionParser parser = new SpelExpressionParser();

	private ParserContext parserContext = new TemplateParserContext();


	@Override
	public String resolveRoutingKey(Object object) {

		Annotation annotation = AnnotationUtils.findAnnotation(object.getClass(), SpELTemplateRoutingKey.class);

		if(annotation == null)
			throw new RuntimeException("Cannot resolve routing key for class: "+object.getClass());

		String value = AnnotationUtils.getValue(annotation).toString();

		Expression expression = parser.parseExpression(value, parserContext);

		return expression.getValue(object).toString();
    }

}
