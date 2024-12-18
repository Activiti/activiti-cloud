package org.activiti.cloud.services.audit.api.search;

import java.util.Date;

public record SearchParams(String search, Date eventTimeFrom, Date eventTimeTo) {}
