package org.activiti.cloud.query;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {QueryApplication.class})
@WebAppConfiguration
public class ApplicationIT {

	@Test
	public void contextLoads() throws Exception {

	}

}