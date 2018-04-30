package org.activiti.cloud.starter.audit.mongo.tests;

import org.activiti.cloud.starter.audit.configuration.EnableActivitiAuditMongo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableActivitiAuditMongo
@ComponentScan("org.activiti")
public class MongoAuditApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(MongoAuditApplication.class,
                              args);
    }

    @Override
    public void run(String... strings) throws Exception {

    }
}
