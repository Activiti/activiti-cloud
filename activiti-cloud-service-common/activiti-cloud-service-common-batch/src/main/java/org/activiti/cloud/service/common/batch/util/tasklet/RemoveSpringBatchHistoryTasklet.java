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
package org.activiti.cloud.service.common.batch.util.tasklet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Tasklet used to delete data from Spring Batch Metadata tables that are N months old.
 *
 * <p>
 * The row numbers in the 6 tables of Spring Batch may increase a lot. This tasklet cleans the
 * Spring Batch database by removing old job instances executions and keep the historic of recent
 * job executions (last 6 months by default).<br>
 * Spring Batch tables prefix could be customized by the {@link #setTablePrefix(String)}<br>
 * </p>
 *
 * @see https://jira.springsource.org/browse/BATCH-1747
 *
 */
public class RemoveSpringBatchHistoryTasklet implements Tasklet, InitializingBean {

    /**
     * SQL statements removing step and job executions compared to a given date.
     */
    private static final String  SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT = "DELETE FROM %PREFIX%STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID IN (SELECT STEP_EXECUTION_ID FROM %PREFIX%STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM  %PREFIX%JOB_EXECUTION where CREATE_TIME < ?))";
    private static final String  SQL_DELETE_BATCH_STEP_EXECUTION         = "DELETE FROM %PREFIX%STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM %PREFIX%JOB_EXECUTION where CREATE_TIME < ?)";
    private static final String  SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT  = "DELETE FROM %PREFIX%JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM  %PREFIX%JOB_EXECUTION where CREATE_TIME < ?)";
    private static final String  SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS   = "DELETE FROM %PREFIX%JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM %PREFIX%JOB_EXECUTION where CREATE_TIME < ?)";
    private static final String  SQL_DELETE_BATCH_JOB_EXECUTION          = "DELETE FROM %PREFIX%JOB_EXECUTION where CREATE_TIME < ?";
    private static final String  SQL_DELETE_BATCH_JOB_INSTANCE           = "DELETE FROM %PREFIX%JOB_INSTANCE WHERE JOB_INSTANCE_ID NOT IN (SELECT JOB_INSTANCE_ID FROM %PREFIX%JOB_EXECUTION)";

    /**
     * Default value for the table prefix property.
     */
    private static final String  DEFAULT_TABLE_PREFIX                    = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

    /**
     * Default value for the data retention (in month)
     */
    private static final Integer DEFAULT_RETENTION_MONTH                 = 6;

    private String               tablePrefix                             = DEFAULT_TABLE_PREFIX;

    private Integer              historicRetentionMonth                  = DEFAULT_RETENTION_MONTH;

    private JdbcTemplate         jdbcTemplate;

    private static final Logger  LOG                                     = LoggerFactory.getLogger(RemoveSpringBatchHistoryTasklet.class);

    public RemoveSpringBatchHistoryTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int totalCount = 0;
        Date date = DateUtils.addMonths(new Date(), -historicRetentionMonth);
        DateFormat df = new SimpleDateFormat();
        LOG.info("Remove the Spring Batch history before the {}", df.format(date));

        int rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT), date);
        LOG.info("Deleted rows number from the BATCH_STEP_EXECUTION_CONTEXT table: {}", rowCount);
        totalCount += rowCount;

        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_BATCH_STEP_EXECUTION), date);
        LOG.info("Deleted rows number from the BATCH_STEP_EXECUTION table: {}", rowCount);
        totalCount += rowCount;

        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT), date);
        LOG.info("Deleted rows number from the BATCH_JOB_EXECUTION_CONTEXT table: {}", rowCount);
        totalCount += rowCount;

        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS), date);
        LOG.info("Deleted rows number from the BATCH_JOB_EXECUTION_PARAMS table: {}", rowCount);
        totalCount += rowCount;

        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_BATCH_JOB_EXECUTION), date);
        LOG.info("Deleted rows number from the BATCH_JOB_EXECUTION table: {}", rowCount);
        totalCount += rowCount;

        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_BATCH_JOB_INSTANCE));
        LOG.info("Deleted rows number from the BATCH_JOB_INSTANCE table: {}", rowCount);
        totalCount += rowCount;

        contribution.incrementWriteCount(totalCount);

        return RepeatStatus.FINISHED;
    }

    protected String getQuery(String base) {
        return StringUtils.replace(base, "%PREFIX%", tablePrefix);
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public void setHistoricRetentionMonth(Integer historicRetentionMonth) {
        this.historicRetentionMonth = historicRetentionMonth;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(jdbcTemplate, "The jdbcTemplate must not be null");
    }

}