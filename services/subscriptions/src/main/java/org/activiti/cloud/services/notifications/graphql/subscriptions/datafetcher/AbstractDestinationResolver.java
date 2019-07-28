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
package org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import graphql.schema.DataFetchingEnvironment;

public abstract class AbstractDestinationResolver implements DataFetcherDestinationResolver {

    public AbstractDestinationResolver() {
    }
    
    protected abstract String any();
    protected abstract String wildcard();
    protected abstract String path();

    @Override
    public List<String> resolveDestinations(DataFetchingEnvironment environment) {
        String fieldName = resolveFieldName(environment);
        
        String[] argumentNames = resolveArgumentNames(environment);
        
        List<String> destinations = new ArrayList<>();

        // Build destinations from arguments
        if(environment.getArguments().size() > 0) {

            List<List<String>> arguments = Stream.of(argumentNames)
                .map(name -> resolveArgument(environment, name))
                .collect(Collectors.toList());
            
            // [[*],[a,b],[*]] => [[*,a,*], [*,b,*]]
            crossJoin(arguments).stream()
                          .map(list -> list.stream()
                                           .collect(Collectors.joining(path())))
                          .forEach(pattern -> destinations.add(fieldName + path() + pattern));
                
        } else {
            destinations.add(fieldName + path() + any());
        }

        return destinations;
    }
    
    protected String resolveFieldName(DataFetchingEnvironment environment) {
        return environment.getFields().iterator().next().getName();
        
    }
    

    protected String[] resolveArgumentNames(DataFetchingEnvironment environment) {
        return environment.getFieldDefinition()
                .getArguments()
                .stream()
                .map(arg -> arg.getName())
                .toArray(String[]::new);
    }
    

    private List<String> resolveArgument(DataFetchingEnvironment environment, String argumentName) {
        List<String> value = new ArrayList<>();
        
        Object argument = environment.getArgument(argumentName);
        
        if(argument instanceof List) {
            value.addAll(environment.getArgument(argumentName));
        } else if(argument != null ) {
            value.add(argument.toString());
        } else {
            value.add(wildcard());
        }
        
        return value;
    }


    public static <T> List<List<T>> crossJoin(List<List<T>> factors) {
        return new CartesianProduct<T>(factors).stream()
                                               .collect(Collectors.toList());
    }
    
    public static List<List<String>> zip(List<String> list1, List<String> list2 ) {
        return crossJoin(Arrays.asList(list1, list2));
    }

    static class CartesianProduct<T> implements Iterable<List<T>> {
        private final Iterable<? extends Iterable<T>> factors;

        public CartesianProduct(final Iterable<? extends Iterable<T>> factors) {
            this.factors = factors;
        }

        @Override
        public Iterator<List<T>> iterator() {
            return new CartesianProductIterator<>(factors);
        }
        
        public Stream<List<T>> stream() {
            return StreamSupport.stream(new CartesianProduct<>(factors).spliterator(), false);
        }
    }

    static class CartesianProductIterator<T> implements Iterator<List<T>> {
        private final List<Iterable<T>> factors;
        private final Stack<Iterator<T>> iterators;
        private final Stack<T> current;
        private List<T> next;
        private int index = 0;

        public CartesianProductIterator(final Iterable<? extends Iterable<T>> factors) {
            this.factors = StreamSupport.stream(factors.spliterator(), false)
                    .collect(Collectors.toList());
            if (this.factors.size() == 0) {
                index = -1;
            }
            iterators = new Stack<>();
            iterators.add(this.factors.get(0).iterator());
            current = new Stack<>();
            computeNext();
        }
        
        private void computeNext() {
            while (true) {
                if (iterators.get(index).hasNext()) {
                    current.add(iterators.get(index).next());
                    if (index == factors.size() - 1) {
                        next = new ArrayList<>(current);
                        current.pop();
                        return;
                    }
                    index++;
                    iterators.add(factors.get(index).iterator());
                } else {
                    index--;
                    if (index < 0) {
                        return;
                    }
                    iterators.pop();
                    current.pop();
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (next == null && index >= 0) {
                computeNext();
            }
            return next != null;
        }

        @Override
        public List<T> next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            List<T> result = next;
            next = null;
            return result;
        }
    }       
    
}
