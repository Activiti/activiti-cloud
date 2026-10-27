/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.common.messaging.config.outbox;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.integration.support.locks.LockRegistry;

public class PollLockAdvice implements MethodInterceptor {

    private final LockRegistry lockRegistry;
    private final String lockKey;

    public PollLockAdvice(LockRegistry lockRegistry, String lockKey) {
        this.lockRegistry = lockRegistry;
        this.lockKey = lockKey;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if ("call".equals(invocation.getMethod().getName())) {
            final var lock = lockRegistry.obtain(lockKey);
            // Attempt to get the lock without blocking indefinitely
            if (lock.tryLock()) {
                try {
                    return invocation.proceed(); // Executes the poll task
                } finally {
                    lock.unlock();
                }
            }
            // Skip the poll execution if the lock cannot be acquired by this thread
            return null;
        }

        return invocation.proceed();
    }
}
