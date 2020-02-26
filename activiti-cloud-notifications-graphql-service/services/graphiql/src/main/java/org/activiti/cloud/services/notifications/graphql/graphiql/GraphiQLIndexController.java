package org.activiti.cloud.services.notifications.graphql.graphiql;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnWebApplication
public class GraphiQLIndexController {
	
	@Value("${graphiql.index:/graphiql/graphiql.html}")
	private String graphiqlHtml;

    @GetMapping("/graphiql")
    public String getIndex() {
        return "forward:/"+graphiqlHtml;
    }

}
