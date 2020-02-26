package org.activiti.cloud.services.notifications.graphql.web.api;

import java.util.Collections;
import java.util.Map;

import javax.validation.constraints.NotNull;

/**
 * GraphQL JSON HTTP Request Wrapper Class
 */
public class GraphQLQueryRequest {

    @NotNull
    private String query;

    private Map<String, Object> variables;

    private GraphQLQueryRequest(Builder builder) {
        this.query = builder.query;
        this.variables = builder.variables;
    }

    GraphQLQueryRequest() {
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return this.query;
    }

    /**
     * @return the variables
     */
    public Map<String, Object> getVariables() {
        return this.variables;
    }

    /**
     * Creates builder to build {@link GraphQLQueryRequest}.
     * @return created builder
     */
    public static IQueryStage builder() {
        return new Builder();
    }

    /**
     * Definition of a stage for staged builder.
     */
    public interface IQueryStage {

        /**
        * Builder method for query parameter.
        * @param query field to set
        * @return builder
        */
        public IBuildStage withQuery(String query);
    }

    /**
     * Definition of a stage for staged builder.
     */
    public interface IBuildStage {

        /**
        * Builder method for variables parameter.
        * @param variables field to set
        * @return builder
        */
        public IBuildStage withVariables(Map<String, Object> variables);

        /**
        * Builder method of the builder.
        * @return built class
        */
        public GraphQLQueryRequest build();
    }

    /**
     * Builder to build {@link GraphQLQueryRequest}.
     */
    public static final class Builder implements IQueryStage, IBuildStage {

        private String query;
        private Map<String, Object> variables = Collections.emptyMap();

        private Builder() {
        }

        @Override
        public IBuildStage withQuery(String query) {
            this.query = query;
            return this;
        }

        @Override
        public IBuildStage withVariables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        @Override
        public GraphQLQueryRequest build() {
            return new GraphQLQueryRequest(this);
        }
    }

}    
