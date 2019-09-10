package org.activiti.cloud.starter.tests.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.activiti.cloud.services.job.executor.JobMessageHandler;
import org.activiti.cloud.services.job.executor.JobMessageHandlerFactory;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.h2.tools.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

public class MultipleRbJobExecutorIT {
    private static final Logger logger = LoggerFactory.getLogger(MultipleRbJobExecutorIT.class);

    private static final String ASYNC_TASK = "asyncTask";
    
    private static ConfigurableApplicationContext h2Ctx;
    private static ConfigurableApplicationContext rbCtx1;
    private static ConfigurableApplicationContext rbCtx2;
    
    @Configuration
    @Profile("h2")
    static class H2Application {
        
        @Bean(initMethod = "start", destroyMethod = "stop")
        public Server inMemoryH2DatabaseaServer() throws SQLException {
            return Server.createTcpServer(
              "-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", "9090");
        }        
    }
    
    @SpringBootApplication
    @ActivitiRuntimeBundle
    static class RbApplication {
        @Bean
        public JobMessageHandlerFactory jobMessageHandlerFactory() {
            return new JobMessageHandlerFactory() {

                @Override
                public MessageHandler create(ProcessEngineConfigurationImpl configuration) {
                    return Mockito.spy(new JobMessageHandler(configuration));
                }
            };
        }        
    }

    @BeforeClass
    public static void setUp() {
        h2Ctx = new SpringApplicationBuilder(H2Application.class).web(WebApplicationType.NONE)
                                                                 .profiles("h2")
                                                                 .run();

        rbCtx1 = new SpringApplicationBuilder(RbApplication.class).properties("server.port=8081")
                                                                  .run();

        rbCtx2 = new SpringApplicationBuilder(RbApplication.class).properties("server.port=8082")
                                                                  .run();
    }

    @AfterClass
    public static void tearDown() {
        rbCtx1.close();
        rbCtx2.close();
        h2Ctx.close();
    }

    @Test
    public void contextLoads() throws Exception {
        assertThat(h2Ctx).isNotNull();        
        assertThat(rbCtx1).isNotNull();        
        assertThat(rbCtx2).isNotNull();        
    }
    
    @Test
    public void shouldDistributeAsyncJobsBetweenMultipleRbReplicas() throws InterruptedException {
        //given
        int jobCount = 100;
        CountDownLatch jobsCompleted = new CountDownLatch(jobCount);
        
        RuntimeService runtimeService = rbCtx1.getBean(RuntimeService.class);
        RepositoryService repositoryService = rbCtx1.getBean(RepositoryService.class);
        ManagementService managementService = rbCtx1.getBean(ManagementService.class);
        
        JobMessageHandler jobMessageHandler1 = rbCtx1.getBean(JobMessageHandler.class);
        JobMessageHandler jobMessageHandler2 = rbCtx2.getBean(JobMessageHandler.class);
        
        rbCtx1.getBean(RuntimeService.class).addEventListener(new CountDownLatchActvitiEventListener(jobsCompleted), 
                                                              ActivitiEventType.JOB_EXECUTION_SUCCESS );

        rbCtx2.getBean(RuntimeService.class).addEventListener(new CountDownLatchActvitiEventListener(jobsCompleted), 
                                                              ActivitiEventType.JOB_EXECUTION_SUCCESS );
        
        String processDefinitionId = repositoryService.createProcessDefinitionQuery()
                                                      .processDefinitionKey(ASYNC_TASK)
                                                      .singleResult()
                                                      .getId();
        //when
        for(int i=0; i<jobCount; i++)
            runtimeService.createProcessInstanceBuilder()
                          .processDefinitionId(processDefinitionId)
                          .start();

        //then
        assertThat(jobsCompleted.await(1, TimeUnit.MINUTES)).as("should distribute and complete all jobs between rb replicas")
                                                            .isTrue();
        
        await("the async executions should complete and no more jobs should exist")
            .untilAsserted(() -> {
                   assertThat(runtimeService.createExecutionQuery()
                                            .processDefinitionKey(ASYNC_TASK).count()).isEqualTo(0);
                    
                   assertThat(managementService.createJobQuery()
                                               .processDefinitionId(processDefinitionId)
                                               .count()).isEqualTo(0);
                                                                                   });
        // rb1 message handler is invoked
        verify(jobMessageHandler1, atLeastOnce()).handleMessage(ArgumentMatchers.<Message<?>>any());

        // rb2 message handler is invoked
        verify(jobMessageHandler2, atLeastOnce()).handleMessage(ArgumentMatchers.<Message<?>>any());
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
        
}
