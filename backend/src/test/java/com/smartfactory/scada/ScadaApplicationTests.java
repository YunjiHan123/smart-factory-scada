package com.smartfactory.scada;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.smartfactory.scada.user.mapper.UserMapper;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
class ScadaApplicationTests {

	@MockitoBean
	private UserMapper userMapper;

	@Test
	void contextLoads() {
	}

}
