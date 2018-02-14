package org.activiti.cloud.query;

import org.actviti.cloud.query.QueryApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = QueryApplication.class)
@DirtiesContext
public class ApplicationIT {

	@Test
	public void contextLoads() throws Exception {

	}

}