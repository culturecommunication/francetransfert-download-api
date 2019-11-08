package com.opengroup.jsbapi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * just to verify spring boot bootstrap successfully
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class StarterkitApplicationTest {
    @Test
    public void whenSpringContextIsBootstrapped_thenNoExceptions() {
    }
}
