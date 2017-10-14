package org.activiti.cloud.starter.tests.cmdendpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class WaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtil.class);

    public static void waitFor(AtomicBoolean check,
                               long timeout) throws InterruptedException {
        long elapsed = 0;
        long waitFor = 100;
        while (!check.get()) {
            if(elapsed < timeout) {
                LOGGER.info("Waiting for " + check + " to be true or " + elapsed + " timeout at: " + timeout + " ...");
                Thread.sleep(waitFor);
                elapsed += waitFor;
            }else{
                throw new IllegalStateException("Operation Time Out");
            }
        }
    }
}
