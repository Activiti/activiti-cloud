package org.activiti.cloud.qa.service;

import feign.Headers;
import feign.RequestLine;

import java.util.Map;

public interface BaseService {

    @RequestLine("GET /actuator/health")
    @Headers("Content-Type: application/json")
    Map<String, Object> health();

    default boolean isServiceUp() {
        Map<String, Object> appInfo = null;
        try {
            appInfo = health();
        } catch (Exception ex) {
            //just retry once
            appInfo = health();
        }

        return appInfo != null && "UP".equals(appInfo.get("status"));
    }
}
