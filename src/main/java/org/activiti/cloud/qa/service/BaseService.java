package org.activiti.cloud.qa.service;

import java.util.Map;

import feign.Headers;
import feign.RequestLine;

public interface BaseService {

    @RequestLine("GET /actuator/health")
    @Headers("Content-Type: application/json")
    Map<String, Object> health();
}
