package com.smartfactory.scada;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.smartfactory.scada.alarm.mapper.AlarmMapper;
import com.smartfactory.scada.chatbot.mapper.ChatbotMapper;
import com.smartfactory.scada.energy.mapper.EnergyMapper;
import com.smartfactory.scada.esg.mapper.EsgMapper;
import com.smartfactory.scada.facility.mapper.FacilityMapper;
import com.smartfactory.scada.plant.mapper.PlantMapper;
import com.smartfactory.scada.simulation.mapper.SimulationMapper;
import com.smartfactory.scada.user.mapper.UserMapper;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
class ScadaApplicationTests {

	@MockitoBean
	private UserMapper userMapper;

	@MockitoBean
	private PlantMapper plantMapper;

	@MockitoBean
	private FacilityMapper facilityMapper;

	@MockitoBean
	private EnergyMapper energyMapper;

	@MockitoBean
	private EsgMapper esgMapper;

	@MockitoBean
	private AlarmMapper alarmMapper;

	@MockitoBean
	private SimulationMapper simulationMapper;

	@MockitoBean
	private ChatbotMapper chatbotMapper;

	@Test
	void contextLoads() {
	}

}
