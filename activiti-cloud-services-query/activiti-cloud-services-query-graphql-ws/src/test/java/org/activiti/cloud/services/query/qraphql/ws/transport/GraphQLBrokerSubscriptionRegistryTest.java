package org.activiti.cloud.services.query.qraphql.ws.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.activiti.cloud.services.query.qraphql.ws.transport.GraphQLBrokerSubscriptionRegistry.SessionSubscriptionInfo;
import org.junit.Before;
import org.junit.Test;


public class GraphQLBrokerSubscriptionRegistryTest {

    private GraphQLBrokerSubscriptionRegistry testSubject;

    @Before
    public void setUp() throws Exception {
        this.testSubject = new GraphQLBrokerSubscriptionRegistry();
    }

    @Test
    public void testSubscribeGraphQLBrokerChannelSubscriber() throws InterruptedException {
        // given
        GraphQLBrokerChannelSubscriber subscriber = mock(GraphQLBrokerChannelSubscriber.class);
        CountDownLatch countDownLatch = new CountDownLatch(1);

        // when
        testSubject.subscribe("sessionId", "subscriptionId", subscriber, () -> countDownLatch.countDown());

        // then
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();

        SessionSubscriptionInfo result = testSubject.get("sessionId");
        assertThat(result.getSubscriber("subscriptionId")).isEqualTo(subscriber);

    }

    @Test
    public void testUnsubscribe() throws InterruptedException {
        // given
        testSubscribeGraphQLBrokerChannelSubscriber();
        AtomicReference<GraphQLBrokerChannelSubscriber> reference = new AtomicReference<>(null);

        // when
        testSubject.unsubscribe("sessionId", (subscriber) -> reference.set(subscriber));

        // then
        assertThat(reference.get()).isNotNull();

        SessionSubscriptionInfo result = testSubject.get("sessionId");
        assertThat(result.getSubscriber("subscriptionId")).isNull();

    }

    @Test
    public void testUnsubscribeConsumerOfGraphQLBrokerChannelSubscriber() throws InterruptedException {
        // given
        testSubscribeGraphQLBrokerChannelSubscriber();
        AtomicReference<GraphQLBrokerChannelSubscriber> reference = new AtomicReference<>(null);

        // when
        testSubject.unsubscribe("sessionId", "subscriptionId", (subscriber) -> reference.set(subscriber));

        // then
        assertThat(reference.get()).isNotNull();

        SessionSubscriptionInfo result = testSubject.get("sessionId");
        assertThat(result.getSubscriber("subscriptionId")).isNull();

    }

    @Test
    public void testGet() {
        // given

        // when
        SessionSubscriptionInfo result = testSubject.get("sessionId");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSubscriber("subscriptionId")).isNull();
    }

}
