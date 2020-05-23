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
package org.activiti.cloud.starter.tests.runtime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.message.RuntimeBundleInfoMessageHeaders;
import org.activiti.cloud.services.job.executor.JobMessageFailedEvent;
import org.activiti.cloud.services.job.executor.JobMessageHandler;
import org.activiti.cloud.services.job.executor.JobMessageHandlerFactory;
import org.activiti.cloud.services.job.executor.JobMessageHeaders;
import org.activiti.cloud.services.job.executor.JobMessageProducer;
import org.activiti.cloud.services.job.executor.JobMessageSentEvent;
import org.activiti.cloud.services.job.executor.MessageBasedJobManager;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.entity.JobEntityImpl;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.ProcessEngineConfigurationConfigurer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles(JobExecutorIT.JOB_EXECUTOR_IT)
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.activiti.asyncExecutorActivate=true",
    "spring.activiti.cloud.rb.job-executor.message-job-consumer.max-attempts=4" // customized
})
@DirtiesContext
@ContextConfiguration(classes = {RuntimeITConfiguration.class,
    JobExecutorIT.JobExecutorITProcessEngineConfigurer.class},
    initializers = {RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class JobExecutorIT {

    private static final Logger logger = LoggerFactory.getLogger(JobExecutorIT.class);
    public static final String JOB_EXECUTOR_IT = "JobExecutorIT";

    private static final String FAILED_TIMER_JOB_RETRY = "failedTimerJobRetry";
    private static final String FAILED_JOB_RETRY = "failedJobRetry";
    private static final String TEST_BOUNDARY_TIMER_EVENT = "testBoundaryTimerEvent";
    private static final String START_TIMER_EVENT_EXAMPLE = "startTimerEventExample";
    private static final String TEST_INTERMEDIATE_TIMER_EVENT = "testCatchingTimerEvent";
    private static final String ASYNC_TASK = "asyncTask";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ConsumerProperties messageJobConsumerProperties;

    @Autowired
    private MessageBasedJobManager messageBasedJobManager;

    @Autowired
    private RuntimeBundleProperties runtimeBundleProperties;

    @SpyBean
    private JobMessageProducer jobMessageProducer;

    private ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    private MessageHandler jobMessageHandler;

    @Autowired
    private RuntimeBundleProperties properties;

    @Captor
    private ArgumentCaptor<Message<String>> messageArgumentCaptor;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean(name = "spyAsyncExecutorJobs")
    private SubscribableChannel spyJobMessageChannel;

    @TestConfiguration
    @Profile(JOB_EXECUTOR_IT)
    static class JobExecutorITProcessEngineConfigurer implements
        ProcessEngineConfigurationConfigurer {

        @Override
        public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
            processEngineConfiguration.setAsyncExecutorDefaultTimerJobAcquireWaitTime(500);
            processEngineConfiguration.setAsyncExecutorDefaultAsyncJobAcquireWaitTime(500);
        }

        @Bean
        public JobMessageHandlerFactory jobMessageHandlerFactory() {
            return new JobMessageHandlerFactory() {

                @Override
                public MessageHandler create(ProcessEngineConfigurationImpl configuration) {
                    return spy(new JobMessageHandler(configuration));
                }
            };
        }
    }

    @BeforeEach
    public void setUp() {
        reset(jobMessageHandler);

        processEngineConfiguration = ProcessEngines.getProcessEngine("default")
            .getProcessEngineConfiguration();
    }

    @AfterEach
    public void tearDown() {
        processEngineConfiguration.getClock().reset();
    }

    @Test
    public void shouldConfigureConsumerProperties() {
        assertThat(messageJobConsumerProperties.getMaxAttempts())
            .as("should configure consumer properties")
            .isEqualTo(4);
    }

    @Test
    public void shouldRegisterJobMessageHandlerBean() {
        assertThat(jobMessageHandler).as("should register JobMessageHandler bean")
            .isInstanceOf(JobMessageHandler.class);
    }

    @Test
    public void shouldRegisterMessageBasedJobManagerBean() {
        assertThat(messageBasedJobManager).as("should register MessageBasedJobManager bean")
            .isInstanceOf(MessageBasedJobManager.class);

        assertThat(messageBasedJobManager.getDestination())
            .as("should configure rb scoped destination")
            .startsWith(runtimeBundleProperties.getServiceName());
    }

    @Test
    public void testAsyncJobs() throws InterruptedException {
        int jobCount = 100;
        CountDownLatch jobsCompleted = new CountDownLatch(jobCount);

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(jobsCompleted),
            ActivitiEventType.JOB_EXECUTION_SUCCESS);

        String processDefinitionId = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(ASYNC_TASK)
            .singleResult()
            .getId();
        //when
        for (int i = 0; i < jobCount; i++) {
            runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinitionId)
                .start();
        }

        // then
        await("the async executions should complete and no more jobs should exist")
            .untilAsserted(() -> {
                assertThat(runtimeService.createExecutionQuery()
                    .processDefinitionKey(ASYNC_TASK).count()).isEqualTo(0);

                assertThat(managementService.createJobQuery()
                    .processDefinitionId(processDefinitionId)
                    .count()).isEqualTo(0);
            });

        assertThat(jobsCompleted.await(1, TimeUnit.MINUTES)).as("should complete all jobs")
            .isTrue();
        // message is sent
        verify(jobMessageProducer, times(jobCount))
            .sendMessage(eq(messageBasedJobManager.getDestination()),
                any(Job.class));
        // message handler is invoked
        verify(jobMessageHandler, times(jobCount)).handleMessage(any(Message.class));
    }

    @Test
    public void testCatchingTimerEvent() throws Exception {
        CountDownLatch jobsCompleted = new CountDownLatch(1);
        CountDownLatch timerScheduled = new CountDownLatch(1);
        CountDownLatch timerFired = new CountDownLatch(1);
        CountDownLatch eventPublished = new CountDownLatch(1);

        applicationContext.addApplicationListener(
            new CountDownLatchApplicationEventListener<JobMessageSentEvent>(eventPublished));

        // Set the clock fixed
        Date startTime = new Date();

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(timerScheduled),
            ActivitiEventType.TIMER_SCHEDULED);

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(timerFired),
            ActivitiEventType.TIMER_FIRED);

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(jobsCompleted),
            ActivitiEventType.JOB_EXECUTION_SUCCESS);

        // when
        ProcessInstance pi = runtimeService
            .startProcessInstanceByKey(TEST_INTERMEDIATE_TIMER_EVENT);

        // then
        assertThat(pi).isNotNull();

        await("the timer job should be created")
            .untilAsserted(() -> {
                assertThat(managementService.createTimerJobQuery()
                    .processInstanceId(pi.getId())
                    .count()).isEqualTo(1);
            });

        // After setting the clock to time '5 minutes and 5 seconds', the timer should fire
        processEngineConfiguration.getClock()
            .setCurrentTime(new Date(startTime.getTime() + ((5 * 60 * 1000) + 5000)));

        // timer event has been scheduled
        assertThat(timerScheduled.await(1, TimeUnit.MINUTES)).as("should schedule timer")
            .isTrue();

        // then
        await("the process instance should complete and no more jobs should exist")
            .untilAsserted(() -> {
                assertThat(runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(pi.getProcessDefinitionKey())
                    .count()).isEqualTo(0);

                assertThat(managementService.createTimerJobQuery()
                    .processInstanceId(pi.getId())
                    .count()).isEqualTo(0);
            });

        // timer event has been fired
        assertThat(timerFired.await(1, TimeUnit.MINUTES)).as("should fire timer")
            .isTrue();

        // job event has been completed
        assertThat(jobsCompleted.await(1, TimeUnit.MINUTES)).as("should complete job")
            .isTrue();
        // job event has been published
        assertThat(eventPublished.await(1, TimeUnit.SECONDS)).as("should publish application event")
            .isTrue();
        // message is sent
        verify(jobMessageProducer).sendMessage(eq(messageBasedJobManager.getDestination()),
            any(Job.class));
        // message handler is invoked
        verify(jobMessageHandler).handleMessage(any(Message.class));
    }

    @Test
    public void testAsyncJobsFailRetry() throws InterruptedException {
        //given
        RetryFailingDelegate.shallThrow = true;
        int retryCount = 5;
        CountDownLatch jobRetries = new CountDownLatch(retryCount);

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(jobRetries),
            ActivitiEventType.JOB_EXECUTION_FAILURE);

        String processDefinitionId = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(FAILED_JOB_RETRY)
            .singleResult()
            .getId();
        //when
        runtimeService.createProcessInstanceBuilder()
            .processDefinitionId(processDefinitionId)
            .start();
        // then
        assertThat(jobRetries.await(1, TimeUnit.MINUTES))
            .as("should retry failed jobs 5 times every 1 sec")
            .isTrue();

        await("the async executions should exists with job exception")
            .untilAsserted(() -> {
                assertThat(runtimeService.createExecutionQuery()
                    .processDefinitionId(processDefinitionId)
                    .activityId("failingJobTask")
                    .count()).isEqualTo(1);

                assertThat(managementService.createDeadLetterJobQuery()
                    .processDefinitionId(processDefinitionId)
                    .withException()
                    .count()).isEqualTo(1);
            });

        // message is sent
        verify(jobMessageProducer, times(retryCount))
            .sendMessage(eq(messageBasedJobManager.getDestination()),
                any(Job.class));
        // message handler is invoked
        verify(jobMessageHandler, times(retryCount)).handleMessage(any(Message.class));
    }

    @Test
    public void testTimerJobsFailRetry() throws InterruptedException {
        //given
        RetryFailingDelegate.shallThrow = true;
        int retryCount = 3;
        CountDownLatch jobRetries = new CountDownLatch(retryCount);

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(jobRetries),
            ActivitiEventType.JOB_EXECUTION_FAILURE);

        String processDefinitionId = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(FAILED_TIMER_JOB_RETRY)
            .singleResult()
            .getId();
        //when
        runtimeService.createProcessInstanceBuilder()
            .processDefinitionId(processDefinitionId)
            .start();
        // then
        assertThat(jobRetries.await(1, TimeUnit.MINUTES))
            .as("should retry failed jobs 2 times every 1 sec")
            .isTrue();

        await("the async executions should exists with job exception")
            .untilAsserted(() -> {
                assertThat(runtimeService.createExecutionQuery()
                    .processDefinitionId(processDefinitionId)
                    .activityId("timerCatchEvent")
                    .count()).isEqualTo(1);

                assertThat(managementService.createDeadLetterJobQuery()
                    .processDefinitionId(processDefinitionId)
                    .withException()
                    .count()).isEqualTo(1);
            });

        // timer job message is sent with 2 retries
        verify(jobMessageProducer, times(retryCount))
            .sendMessage(eq(messageBasedJobManager.getDestination()),
                any(Job.class));
        // message handler is invoked
        verify(jobMessageHandler, times(retryCount)).handleMessage(any(Message.class));
    }

    @Test
    public void testStartTimeEvent() throws InterruptedException {
        // given
        CountDownLatch jobCompleted = new CountDownLatch(1);
        CountDownLatch timerFired = new CountDownLatch(1);
        CountDownLatch eventPublished = new CountDownLatch(1);

        applicationContext.addApplicationListener(new CountDownLatchApplicationEventListener<JobMessageSentEvent>(eventPublished));

        // Set the clock fixed
        Date startTime = new Date();

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(timerFired), ActivitiEventType.TIMER_FIRED);

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(jobCompleted), ActivitiEventType.JOB_EXECUTION_SUCCESS);

        //when
        String processDefinitionId = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(START_TIMER_EVENT_EXAMPLE)
            .singleResult()
            .getId();
        // when
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey(START_TIMER_EVENT_EXAMPLE)
            .singleResult();
        // then
        assertThat(pi).isNull();

        await("the timer job should be created")
            .untilAsserted(() -> {
                assertThat(managementService.createTimerJobQuery()
                    .processDefinitionId(processDefinitionId)
                    .count()).isEqualTo(1);
            });

        // After setting the clock to time '1 hour and 5 seconds', the timer should fire
        processEngineConfiguration.getClock()
            .setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
        // then
        await("the process should start and no more timer jobs should exist")
            .untilAsserted(() -> {
                assertThat(runtimeService.createProcessInstanceQuery()
                    .processDefinitionId(processDefinitionId)
                    .count()).isEqualTo(1);

                assertThat(managementService.createTimerJobQuery()
                    .processDefinitionId(processDefinitionId)
                    .count()).isEqualTo(0);
            });

        // timer event has been fired
        assertThat(timerFired.await(1, TimeUnit.MINUTES)).as("should fire timer")
            .isTrue();

        // job event has been completed
        assertThat(jobCompleted.await(1, TimeUnit.MINUTES)).as("should complete job")
            .isTrue();

        // job event has been published
        assertThat(eventPublished.await(1, TimeUnit.SECONDS)).as("should publish application event")
            .isTrue();

        // message is sent
        verify(jobMessageProducer).sendMessage(eq(messageBasedJobManager.getDestination()),
            any(Job.class));
        // message handler is invoked
        verify(jobMessageHandler).handleMessage(any(Message.class));
    }

    @Test
    public void testBoundaryTimerEvent() throws Exception {
        CountDownLatch jobsCompleted = new CountDownLatch(1);
        CountDownLatch timerScheduled = new CountDownLatch(1);
        CountDownLatch timerFired = new CountDownLatch(1);

        // Set the clock fixed
        Date startTime = new Date();

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(timerScheduled), ActivitiEventType.TIMER_SCHEDULED);

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(timerFired), ActivitiEventType.TIMER_FIRED);

        runtimeService.addEventListener(new CountDownLatchActvitiEventListener(jobsCompleted), ActivitiEventType.JOB_EXECUTION_SUCCESS);

        // when
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(TEST_BOUNDARY_TIMER_EVENT);

        // then
        assertThat(pi).isNotNull();

        await("the timer job should be created")
            .untilAsserted(() -> {
                assertThat(managementService.createTimerJobQuery()
                    .processInstanceId(pi.getId())
                    .count()).isEqualTo(1);
            });

        // After setting the clock to time '5 minutes and 5 seconds', the timer should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((5 * 60 * 1000) + 5000)));

        // timer event has been scheduled
        assertThat(timerScheduled.await(1, TimeUnit.MINUTES)).as("should schedule timer")
            .isTrue();

        // then
        await("the process instance should complete and no more timer jobs should exist")
            .untilAsserted(() -> {
                assertThat(runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(pi.getProcessDefinitionKey())
                    .count()).isEqualTo(0);

                assertThat(managementService.createTimerJobQuery()
                    .processInstanceId(pi.getId())
                    .count()).isEqualTo(0);
            });

        // timer event has been fired
        assertThat(timerFired.await(1, TimeUnit.MINUTES)).as("should fire timer")
            .isTrue();

        // job event has been completed
        assertThat(jobsCompleted.await(1, TimeUnit.MINUTES)).as("should complete job")
            .isTrue();
        // message is sent
        verify(jobMessageProducer).sendMessage(eq(messageBasedJobManager.getDestination()),
            any(Job.class));
        // message handler is invoked
        verify(jobMessageHandler).handleMessage(any(Message.class));
    }

    @Test
    public void shouldPublishJobMessageFailedEvent() throws InterruptedException {
        // given
        CountDownLatch eventPublished = new CountDownLatch(1);
        String destination = "spyAsyncExecutorJobs";

        applicationContext.addApplicationListener(new CountDownLatchApplicationEventListener<JobMessageFailedEvent>(eventPublished));

        doReturn(false).when(spyJobMessageChannel)
            .send(any(Message.class));

        // when
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobMessageProducer.sendMessage(destination, new TestJobEntity("jobId"));
            }

        });

        // then
        assertThat(eventPublished.await(1, TimeUnit.SECONDS)).as("should publish JobMessageFailedEvent")
            .isTrue();
    }

    @Test
    public void shouldFailIfNoActiveTransactionSynchronization() {
        // when
        Throwable throwable = catchThrowable(
            () -> jobMessageProducer.sendMessage(anyString(),
                any(Job.class))
        );

        //then
        assertThat(throwable)
            .as("Should fail if no active transaction syncronization")
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("requires active transaction synchronization");
    }

    @Test
    public void shouldPublishJobMessageSentEvent() throws InterruptedException {
        // given
        CountDownLatch eventPublished = new CountDownLatch(1);
        String destination = "spyAsyncExecutorJobs";

        applicationContext.addApplicationListener(new CountDownLatchApplicationEventListener<JobMessageSentEvent>(eventPublished));

        doReturn(true).when(spyJobMessageChannel)
            .send(any(Message.class));

        // when
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobMessageProducer.sendMessage(destination, new TestJobEntity("jobId"));
            }

        });

        // then
        assertThat(eventPublished.await(1, TimeUnit.SECONDS)).as("should publish JobMessageSentEvent")
            .isTrue();
    }

    @Test
    public void shouldBuildJobMessage() throws InterruptedException {
        // given
        String destination = "spyAsyncExecutorJobs";
        String jobId = "jobId";

        TestJobEntity job = new TestJobEntity(jobId).withDueDate(new Date())
            .withExecutionId("executionId")
            .withJobHandlerType("jobHandlerType")
            .withJobHandlerConfiguration("jobHandlerConfiguration")
            .withJobType("jobType")
            .withProcessDefinitionId("processDefinitionId")
            .withProcessInstanceId("processInstanceId")
            .withRetries(3)
            .withTenantId("tenantId")
            .withExceptionMessage("exceptionMessage");
        doReturn(true).when(spyJobMessageChannel)
            .send(any(Message.class));

        // when
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobMessageProducer.sendMessage(destination, job);
            }

        });

        // then
        verify(spyJobMessageChannel).send(messageArgumentCaptor.capture());

        Message<String> message = messageArgumentCaptor.getValue();

        assertThat(message.getPayload()).as("should build job id as payload")
            .isEqualTo(jobId);

        assertThat(message.getHeaders()).as("should build common headers")
            .containsEntry("routingKey", destination)
            .containsEntry("messagePayloadType", String.class.getName())
        ;

        assertThat(message.getHeaders()).as("should build runtime bundle properties as headers")
            .containsEntry(RuntimeBundleInfoMessageHeaders.APP_NAME, properties.getAppName())
            .containsEntry(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, properties.getServiceName())
            .containsEntry(RuntimeBundleInfoMessageHeaders.SERVICE_TYPE, properties.getServiceType())
            .containsEntry(RuntimeBundleInfoMessageHeaders.SERVICE_VERSION, properties.getServiceVersion())
        ;

        assertThat(message.getHeaders()).as("should build job attributes as headers")
            .containsEntry(JobMessageHeaders.JOB_ID, job.getId())
            .containsEntry(JobMessageHeaders.JOB_TYPE, job.getJobType())
            .containsEntry(JobMessageHeaders.JOB_HANDLER_TYPE, job.getJobHandlerType())
            .containsEntry(JobMessageHeaders.JOB_EXCEPTION_MESSAGE, job.getExceptionMessage())
            .containsEntry(JobMessageHeaders.JOB_PROCESS_DEFINITION_ID, job.getProcessDefinitionId())
            .containsEntry(JobMessageHeaders.JOB_EXECUTION_ID, job.getExecutionId())
            .containsEntry(JobMessageHeaders.JOB_DUE_DATE, job.getDuedate())
            .containsEntry(JobMessageHeaders.JOB_HANDLER_CONFIGURATION, job.getJobHandlerConfiguration())
            .containsEntry(JobMessageHeaders.JOB_RETRIES, job.getRetries())
        ;

    }


    abstract class AbstractActvitiEventListener implements ActivitiEventListener {

        @Override
        public boolean isFailOnException() {
            return false;
        }
    }

    class CountDownLatchActvitiEventListener extends AbstractActvitiEventListener {

        private final CountDownLatch countDownLatch;

        public CountDownLatchActvitiEventListener(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void onEvent(ActivitiEvent arg0) {
            logger.info("Received Activiti Event: {}", arg0);

            countDownLatch.countDown();
        }
    }

    class CountDownLatchApplicationEventListener<E extends ApplicationEvent> implements ApplicationListener<E> {

        private final CountDownLatch countDownLatch;

        public CountDownLatchApplicationEventListener(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void onApplicationEvent(E event) {
            logger.info("Received Activiti Event: {}", event);

            countDownLatch.countDown();
        }
    }

    static class TestJobEntity extends JobEntityImpl {

        private static final long serialVersionUID = 1L;

        public TestJobEntity(String jobId) {
            super();
            setId(jobId);
        }

        public TestJobEntity withExecutionId(String executionId) {
            setExecutionId(executionId);

            return this;
        }

        public TestJobEntity withDueDate(Date dueDate) {
            setDuedate(dueDate);

            return this;
        }

        public TestJobEntity withJobType(String jobType) {
            setJobType(jobType);

            return this;
        }

        public TestJobEntity withJobHandlerType(String jobHandlerType) {
            setJobHandlerType(jobHandlerType);

            return this;
        }

        public TestJobEntity withJobHandlerConfiguration(String jobHandlerConfiguration) {
            setJobHandlerConfiguration(jobHandlerConfiguration);
            return this;
        }

        public TestJobEntity withProcessDefinitionId(String processDefinitionId) {
            setProcessDefinitionId(processDefinitionId);

            return this;
        }

        public TestJobEntity withProcessInstanceId(String processInstanceId) {
            setProcessInstanceId(processInstanceId);

            return this;
        }

        public TestJobEntity withTenantId(String tenantId) {
            setTenantId(tenantId);

            return this;
        }

        public TestJobEntity withRetries(int retries) {
            setRetries(retries);

            return this;
        }

        public TestJobEntity withExceptionMessage(String exceptionMessage) {
            setExceptionMessage(exceptionMessage);

            return this;
        }
    }

    public static class RetryFailingDelegate implements JavaDelegate {

        public static final String EXCEPTION_MESSAGE = "Expected exception.";

        public static boolean shallThrow;
        public static List<Long> times = new ArrayList<Long>();

        static public void resetTimeList() {
            times = new ArrayList<Long>();
        }

        @Override
        public void execute(DelegateExecution execution) {

            times.add(System.currentTimeMillis());

            if (shallThrow) {
                throw new ActivitiException(EXCEPTION_MESSAGE);
            }
        }
    }
}
