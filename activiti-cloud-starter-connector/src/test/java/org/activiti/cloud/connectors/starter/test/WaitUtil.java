package org.activiti.cloud.connectors.starter.test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitUtil.class);

    private static long defaultTimeOut = 1000;

    public static void waitFor(AtomicBoolean check) throws InterruptedException {
        waitFor(check,
                defaultTimeOut);
    }

    public static void waitFor(AtomicBoolean check,
                               long timeout) throws InterruptedException {
        long elapsed = 0;
        long waitFor = 100;
        while (!check.get()) {
            if (elapsed < timeout) {
                LOGGER.info("Waiting for " + check + " to be true or " + elapsed + " timeout at: " + timeout + " ...");
                Thread.sleep(waitFor);
                elapsed += waitFor;
            } else {
                throw new IllegalStateException("Operation Time Out");
            }
        }
    }

    public static void waitForCounterGreaterThanThreshold(AtomicInteger check,
                                                          int threshold) throws InterruptedException {
        waitForCounterGreaterThanThreshold(check,
                                           threshold,
                                           defaultTimeOut);
    }

    public static void waitForCounterGreaterThanThreshold(AtomicInteger check,
                                                          int threshold,
                                                          long timeout) throws InterruptedException {
        long elapsed = 0;
        long waitFor = 100;
        while (check.get() < threshold) {
            if (elapsed < timeout) {
                LOGGER.info("Waiting for " + check + " to be true or " + elapsed + " timeout at: " + timeout + " ...");
                Thread.sleep(waitFor);
                elapsed += waitFor;
            } else {
                throw new IllegalStateException("Operation Time Out");
            }
        }
    }
}
