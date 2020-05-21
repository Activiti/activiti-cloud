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
package org.activiti.cloud.starter.messages.test.mongodb;

import java.io.IOException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class MongodbApplicationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int CONTAINER_EXIT_CODE_OK = 0;
    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 60;

    private static GenericContainer container = new GenericContainer("mongo")
        .withExposedPorts(27017)
        .waitingFor(Wait.forLogMessage(".*waiting for connections on port.*", 1))
        .withCommand("--replSet", "docker-rs");

    private static MongoDBContainer mongoDBContainer = new MongoDBContainer();
    @Override
    public void initialize(ConfigurableApplicationContext context) {

        container.start();

        try {
            initReplicaSet();

            TestPropertyValues.of(
                "spring.data.mongodb.uri=mongodb://" + container.getContainerIpAddress() + ":" + container.getFirstMappedPort() + "/test"
            ).applyTo(context.getEnvironment());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void initReplicaSet() throws IOException, InterruptedException {
        final ExecResult execResultInitRs = container.execInContainer(
            buildMongoEvalCommand("rs.initiate();")
        );
        checkMongoNodeExitCode(execResultInitRs);

        final ExecResult execResultWaitForMaster = container.execInContainer(
            buildMongoEvalCommand(buildMongoWaitCommand())
        );

        checkMongoNodeExitCodeAfterWaiting(execResultWaitForMaster);
    }



    private void checkMongoNodeExitCodeAfterWaiting(final Container.ExecResult execResultWaitForMaster) {
        if (execResultWaitForMaster.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format(
                "A single node replica set was not initialized in a set timeout: %d attempts",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS
            );
            throw new RuntimeException(errorMessage);
        }
    }


    private void checkMongoNodeExitCode(final Container.ExecResult execResult) {
        if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format("An error occurred: %s", execResult.getStdout());
            throw new RuntimeException(errorMessage);
        }
    }

    private String buildMongoWaitCommand() {
        return String.format(
            "var attempt = 0; " +
                "while" +
                "(%s) " +
                "{ " +
                "if (attempt > %d) {quit(1);} " +
                "print('%s ' + attempt); sleep(100);  attempt++; " +
                " }",
            "db.runCommand( { isMaster: 1 } ).ismaster==false",
            AWAIT_INIT_REPLICA_SET_ATTEMPTS,
            "An attempt to await for a single node replica set initialization:"
        );
    }

    private String[] buildMongoEvalCommand(final String command) {
        return new String[]{"mongo", "--eval", command};
    }

}
