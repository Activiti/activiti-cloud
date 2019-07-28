package org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class AbstractDestinationResolverTest {

    @Test
    public void testCrossJoin() {
        List<String> a = Arrays.asList("*");
        List<String> b = Arrays.asList("a","b");
        List<String> c = Arrays.asList("c","d");
        List<String> d = Arrays.asList("e","f");
        List<String> e = Arrays.asList("*");
        
        List<List<String>> product = AbstractDestinationResolver.crossJoin(Arrays.asList(a,b,c,d,e));
        
        assertThat(product).hasSize(8);
        assertThat(product).containsOnly(Arrays.asList("*","a","c","e","*"),
                                         Arrays.asList("*","a","c","f","*"),
                                         Arrays.asList("*","a","d","e","*"),
                                         Arrays.asList("*","a","d","f","*"),
                                         Arrays.asList("*","b","c","e","*"),
                                         Arrays.asList("*","b","d","f","*"),
                                         Arrays.asList("*","b","d","e","*"),
                                         Arrays.asList("*","b","c","f","*"));
    }
}
