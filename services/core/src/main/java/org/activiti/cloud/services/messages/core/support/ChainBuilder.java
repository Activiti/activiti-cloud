/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.messages.core.support;

import java.lang.reflect.Method;

import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;


public class ChainBuilder<T, R> {
    private Class<? extends Chain<T, R>> clazz;
    
    public static <T,R> ChainBuilder<T,R> of(Class<? extends Chain<T, R>> clazz) {
        return new ChainBuilder<>(clazz);
    }

    private HandlerImpl<T, R> first;

    private ChainBuilder(Class<? extends Chain<T, R>> clazz) {
        this.clazz = clazz;
    }

    public SuccessorBuilder first(Handler<T, R> handler) {
        first = new HandlerImpl<>(handler);
        return new SuccessorBuilder(first);
    }

    public class SuccessorBuilder {
        private HandlerImpl<T, R> current;

        private SuccessorBuilder(HandlerImpl<T, R> current) {
            this.current = current;
        }

        public SuccessorBuilder then(Handler<T, R> successor) {
            HandlerImpl<T, R> successorWrapper = new HandlerImpl<>(successor);
            current.setSuccessor(successorWrapper);
            current = successorWrapper;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <I extends Chain<T, R>> I build() {
            return (I) Proxy.newProxyInstance(ChainBuilder.class.getClassLoader(), 
                                              new Class[] { clazz }, 
                                              new ChainInvocationHandler<T,R>(new ChainImpl<T, R>(first)));
        }
    }

    static final class ChainInvocationHandler<T, R> implements InvocationHandler {
        
        private Chain<T, R> chain;
        
        public ChainInvocationHandler(Chain<T, R> chain) {
            this.chain = chain;
        }
        
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(chain, args);
        }
    }
    
    private static class ChainImpl<T, R> implements Chain<T, R> {
        private final Handler<T, R> first;

        public ChainImpl(Handler<T, R> first) {
            this.first = first;
        }

        @Override
        public R handle(T t) {
            return first.handle(t);
        }
    }

    private static class HandlerImpl<T, R> implements Handler<T, R> {
        private final Handler<T, R> delegate;
        private Handler<T, R> successor;

        public HandlerImpl(Handler<T, R> delegate) {
            this.delegate = delegate;
        }

        private void setSuccessor(HandlerImpl<T, R> successor) {
            this.successor = successor;
        }

        @Override
        public R handle(T t) {
            R result = delegate.handle(t);
            
            if (result != null) {
                return result;
            }
            else if (successor != null) {
                return successor.handle(t);
            }
            
            return null;
        }
    }
}
