# Query Timeout Configuration - Implementation Guide

## Overview

This document describes the query timeout protection mechanisms implemented to prevent long-running database queries from consuming resources during heavy database loads in the Activiti Query Service.

## Problem Statement

During heavy database loads, some queries (particularly in the `/admin/*` endpoints) can exceed 17 minutes, while the application drops the connection after 2 minutes. This causes:
- Orphaned database queries continuing to consume resources
- Increased database load making the situation worse
- Client timeouts without proper cleanup on the database side

## Solution

The solution implements multi-layered timeout protection at three levels:

### Phase 1: Database-Level Query Protection

**Configuration Property:** `spring.datasource.hikari.*`

Database queries that exceed 5 minutes (300 seconds) are automatically killed by the database.

```properties
# In PostgreSQL, add to connection URL:
statement_timeout=300000  # 5 minutes in milliseconds
```

**Benefits:**
- Database automatically terminates long-running queries
- Prevents resource exhaustion on the database server
- Works at the lowest level (independent of application behavior)

### Phase 2: Connection Pool Configuration

**Configuration Properties:**
```properties
# HikariCP Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000      # 30 seconds
spring.datasource.hikari.max-lifetime=1800000          # 30 minutes
spring.datasource.hikari.idle-timeout=600000           # 10 minutes
spring.datasource.hikari.leak-detection-threshold=30000 # 30 seconds
```

**Benefits:**
- Limits concurrent database connections
- Quickly fails when pool is exhausted
- Recycles long-lived connections
- Detects connection leaks

**Configuration Details:**
- `maximum-pool-size`: Prevents connection exhaustion (set based on concurrent queries needed)
- `connection-timeout`: Fast-fail when no connections available (30s)
- `max-lifetime`: Recycles connections to prevent stale connections (30min)
- `idle-timeout`: Releases unused connections (10min)
- `leak-detection-threshold`: Logs connections held > 30s

### Phase 3: Application-Level Query Timeout

**Configuration Properties:**
```properties
# JPA/Hibernate Query Timeout (in milliseconds)
spring.jpa.properties.hibernate.query.timeout=120000  # 2 minutes

# Statement cache
spring.jpa.properties.hibernate.jdbc.statement_cache_size=250

# Fetch size optimization
spring.jpa.properties.org.hibernate.jdbc.fetch_size=100
```

**Implementation:**
- `CustomizedJpaSpecificationExecutorImpl` applies timeout hints to all JPA queries
- Timeout is set on both JPA (`jakarta.persistence.query.timeout`) and Hibernate (`org.hibernate.timeout`) levels
- Converts milliseconds to seconds for Hibernate where needed

**Benefits:**
- Application gracefully handles query timeouts
- Allows proper error handling and logging
- Prevents query threads from hanging indefinitely

### Phase 4: Request-Level Timeout (Future Enhancement)

Spring MVC/WebFlux should be configured with request timeouts to match application behavior:
```properties
# Spring Server timeout
server.servlet.session.timeout=15m
server.max-http-request-header-size=8192
# WebFlux timeout (if used)
spring.webflux.httpclient.max-idle-time=5m
```

## Configuration Files Modified

1. **Starter Modules:**
   - `activiti-cloud-starter-query-consumer/src/main/resources/metadata.properties`
   - `activiti-cloud-starter-query-rest/src/main/resources/metadata.properties`

2. **Query Service Modules:**
   - `activiti-cloud-services-query/activiti-cloud-services-query-rest/src/main/resources/query-rest.properties`
   - `activiti-cloud-services-query/activiti-cloud-services-query-liquibase/src/main/resources/config/query-liquibase.properties`

3. **Test Configuration:**
   - `activiti-cloud-starter-query/src/test/resources/application-test.properties`

## Code Changes

### 1. QueryTimeoutConfiguration.java (NEW)

Central configuration class for managing query timeout properties.

```java
@AutoConfiguration
public class QueryTimeoutConfiguration {
    // Loads timeout properties from Spring configuration
    // Provides QueryTimeoutProperties bean for injection
}
```

### 2. CustomizedJpaSpecificationExecutorImpl.java (ENHANCED)

Enhanced to apply query timeout hints to all queries:

```java
private void applyQueryTimeout(TypedQuery<T> query) {
    if (queryTimeout > 0) {
        query.setHint("jakarta.persistence.query.timeout", queryTimeout);
        query.setHint("org.hibernate.timeout", queryTimeout / 1000);
    }
}
```

### 3. QueryRepositoryAutoConfiguration.java (ENHANCED)

Updated to register the QueryTimeoutConfiguration bean.

### 4. ProcessInstanceAdminService.java (ENHANCED)

Updated to accept optional QueryTimeoutProperties for future timeout-aware query building.

## Timeout Values Recommended

### Development/Testing
- Query timeout: 120 seconds (2 minutes)
- Connection timeout: 30 seconds
- Pool size: 5-10 connections

### Production
- Query timeout: 300 seconds (5 minutes) - allows sufficient time for complex queries
- Connection timeout: 30-60 seconds
- Pool size: 20-50 connections (depends on load)
- Database statement_timeout: 600 seconds (10 minutes) - prevents stuck queries

## Monitoring and Alerts

### Log Messages to Monitor

Look for these messages in application logs:

```
WARN: Query execution exceeded configured timeout of XXXms
WARN: HikariPool connection timeout - no connections available
ERROR: Database connection leaked - connection held for XXms
```

### Metrics to Track

1. **Query Execution Time**
   - Count of queries exceeding threshold
   - Average query execution time
   - P95/P99 query execution times

2. **Connection Pool Metrics**
   - Active connections
   - Idle connections
   - Connection acquisition wait time
   - Connection timeout occurrences

3. **Database Load**
   - Long-running query count
   - Query cancellation rate
   - Lock contention

## Troubleshooting

### Issue: "Query timeout" errors increasing in logs

**Cause:** Queries are legitimately taking > 2 minutes

**Solutions:**
1. Increase `spring.jpa.properties.hibernate.query.timeout` value
2. Optimize slow queries (add indexes, review SQL execution plans)
3. Implement pagination for large result sets
4. Consider splitting large queries into smaller batches

### Issue: "Connection timeout - no connections available"

**Cause:** Pool is exhausted by slow queries

**Solutions:**
1. Reduce query timeout value to free connections faster
2. Increase `maximum-pool-size`
3. Review query logs to find bottlenecks

### Issue: No "Connection leaked" warnings but memory increasing

**Cause:** Connection leak threshold too high

**Solutions:**
1. Reduce `leak-detection-threshold` to 10-20 seconds
2. Check for missing `try/finally` or try-with-resources in code

## Performance Considerations

1. **Query Timeout Trade-off:**
   - Too low: Legitimate queries fail
   - Too high: Slow queries hog resources
   - Recommended: Set to 90th percentile of query time + safety margin

2. **Connection Pool Size Trade-off:**
   - Too small: Frequent connection timeouts
   - Too large: Memory overhead
   - Recommended: 2-3x expected concurrent queries

3. **Statement Cache Size:**
   - Caches prepared statements for performance
   - Set to `250` for most workloads

## Future Enhancements

1. **Dynamic Timeout Configuration:**
   - Make timeouts configurable per endpoint
   - Admin endpoints: longer timeout (5 minutes)
   - User endpoints: shorter timeout (2 minutes)

2. **Query Metrics Collection:**
   - Instrument queries to collect execution time metrics
   - Alert on slow query thresholds

3. **Graceful Degradation:**
   - Implement query cancellation with proper cleanup
   - Return partial results when query times out
   - Provide fallback endpoints with simpler queries

4. **Database Connection String:**
   - For PostgreSQL, add to JDBC URL:
     ```
     jdbc:postgresql://host:5432/db?statement_timeout=300000
     ```
   - For other databases, configure equivalent mechanisms

## References

- Hibernate Query Timeout: https://hibernate.org/orm/documentation/
- HikariCP Configuration: https://github.com/brettwooldridge/HikariCP/wiki/Configuration
- Spring Boot Data Source Configuration: https://spring.io/projects/spring-boot
- PostgreSQL statement_timeout: https://www.postgresql.org/docs/current/runtime-config-client.html

