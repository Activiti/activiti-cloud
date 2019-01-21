package org.activiti.cloud.services.notifications.graphql.graphiql;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class GraphiQLConfigController {
	
	@Value("${graphiql.graphql.web.path:/graphql}")
	private String graphqlWebPath;

	@Value("${graphiql.graphql.ws.path:/ws/graphql}")
	private String graphqlWsPath;

    @GetMapping(value="config.js",  produces = "application/javascript")
    @ResponseStatus(HttpStatus.OK)
    public String getConfigJs() {
    	
    	String config = "window.GraphqlApi = {" +
    			"   graphqlWebPath: " + "\"" + graphqlWebPath + "\"," +
    			"   graphqlWsPath: " + "\"" + graphqlWsPath + "\"" +
    			"}";    	
    	
        return config;
    }	    
    
    @GetMapping(value="config.json",  produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getGraphiqlJson() {
        Map<String, Object> values = new LinkedHashMap<>();
        
        values.put("graphqlWebPath", graphqlWebPath);
        values.put("graphqlWsPath", graphqlWsPath);
        
        return ResponseEntity.ok(values);
    }	
    
}
