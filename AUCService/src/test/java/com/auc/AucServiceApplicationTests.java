package com.auc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.ogb.auc.AucServiceApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AucServiceApplication.class)
@WebAppConfiguration
public class AucServiceApplicationTests {

	@Test
	public void contextLoads() {
	}

}
