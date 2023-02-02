package org.activiti.cloud.common.messaging.functional;

import java.util.function.Consumer;

@FunctionalInterface
public interface ConsumerConnector<T> extends Connector<T, Void>, Consumer<T> {

    default Void apply(T t) {
        this.accept(t);
        return null;
    }
}
