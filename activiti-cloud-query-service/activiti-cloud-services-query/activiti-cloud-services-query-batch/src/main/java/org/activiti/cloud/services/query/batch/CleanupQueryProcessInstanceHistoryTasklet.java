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
package org.activiti.cloud.services.query.batch;

import javax.persistence.EntityManager;

import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Tasklet used to delete data from process instance history tables that are N months old.
 *
 */
public class CleanupQueryProcessInstanceHistoryTasklet implements Tasklet, InitializingBean {

    /**
     * SQL statements removing process instance history
     */
    private static final String  SQL_DELETE_PROCESS_VARIABLE      = "DELETE FROM PROCESS_VARIABLE WHERE CREATE_TIME < ?";
    private static final String  SQL_DELETE_TASK_VARIABLE         = "DELETE FROM TASK_VARIABLE WHERE CREATE_TIME < ?";
    private static final String  SQL_DELETE_INTEGRATION_CONTEXT   = "DELETE FROM INTEGRATION_CONTEXT where CREATE_TIME < ?";
    private static final String  SQL_DELETE_BPMN_ACTIVITY         = "DELETE FROM BPMN_ACTIVITY WHERE CREATE_TIME < ?";
    private static final String  SQL_DELETE_BPMN_SEQUENCE_FLOW    = "DELETE FROM BPMN_SEQUENCE_FLOW WHERE CREATE_TIME < ?";
    private static final String  SQL_DELETE_TASK_CANDIDATE_GROUP  = "DELETE FROM TASK_CANDIDATE_GROUP WHERE CREATE_TIME < ?";
    private static final String  SQL_DELETE_TASK_CANDIDATE_USER   = "DELETE FROM TASK_CANDIDATE_USER WHERE CREATE_TIME < ?";
    private static final String  SQL_DELETE_TASK                  = "DELETE FROM TASK where CREATE_TIME < ?";
    private static final String  SQL_DELETE_PROCESS_INSTANCE      = "DELETE FROM PROCESS_INSTANCE WHERE where CREATE_TIME < ?";

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

    private EntityManager entityManager;

    private static final Logger  LOG                                     = LoggerFactory.getLogger(CleanupQueryProcessInstanceHistoryTasklet.class);

    public CleanupQueryProcessInstanceHistoryTasklet(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        DeletionHelper helper = new DeletionHelper(entityManager);

        int totalCount = helper.deleteCascade(ProcessInstanceEntity.class,
                                             "1");

//        Date date = DateUtils.addMonths(new Date(), -historicRetentionMonth);
//        DateFormat df = new SimpleDateFormat();
//        LOG.info("Removing process instance history before the {}", df.format(date));
//
//        int rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_PROCESS_VARIABLE), date);
//        LOG.info("Deleted rows number from the PROCESS_VARIABLE table: {}", rowCount);
//        totalCount += rowCount;
//
//        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_TASK_VARIABLE), date);
//        LOG.info("Deleted rows number from the TASK_VARIABLE table: {}", rowCount);
//        totalCount += rowCount;
//
//        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_BPMN_ACTIVITY), date);
//        LOG.info("Deleted rows number from the BPMN_ACTIVITY table: {}", rowCount);
//        totalCount += rowCount;
//
//        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_BPMN_SEQUENCE_FLOW), date);
//        LOG.info("Deleted rows number from the BPMN_SEQUENCE_FLOW table: {}", rowCount);
//        totalCount += rowCount;
//
//        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_INTEGRATION_CONTEXT), date);
//        LOG.info("Deleted rows number from the INTEGRATION_CONTEXT table: {}", rowCount);
//        totalCount += rowCount;
//
//        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_TASK_CANDIDATE_GROUP));
//        LOG.info("Deleted rows number from the SQL_DELETE_TASK_CANDIDATE_GROUP table: {}", rowCount);
//        totalCount += rowCount;
//
//        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_TASK_CANDIDATE_USER));
//        LOG.info("Deleted rows number from the SQL_DELETE_TASK_CANDIDATE_USER table: {}", rowCount);
//        totalCount += rowCount;
//
//        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_TASK));
//        LOG.info("Deleted rows number from the TASK table: {}", rowCount);
//        totalCount += rowCount;
//
//        rowCount = jdbcTemplate.update(getQuery(SQL_DELETE_PROCESS_INSTANCE));
//        LOG.info("Deleted rows number from the PROCESS_INSTANCE table: {}", rowCount);
//        totalCount += rowCount;

        contribution.incrementWriteCount(totalCount);

        LOG.info("Completed process instance history cleanup with {} rows", totalCount);

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

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(entityManager, "The entityManager must not be null");
    }

}