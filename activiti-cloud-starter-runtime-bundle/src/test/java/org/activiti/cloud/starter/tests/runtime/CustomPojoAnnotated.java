package org.activiti.cloud.starter.tests.runtime;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,property = "@class")
public class CustomPojoAnnotated extends CustomPojo{
}
