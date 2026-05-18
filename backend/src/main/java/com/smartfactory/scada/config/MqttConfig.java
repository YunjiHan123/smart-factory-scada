package com.smartfactory.scada.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.smartfactory.scada.energy.service.EnergyMeasurementMqttService;

@Configuration
public class MqttConfig {

	@Value("${mqtt.broker-url}")
	private String brokerUrl;

	@Value("${mqtt.client-id}")
	private String clientId;

	@Value("${mqtt.username:}")
	private String username;

	@Value("${mqtt.password:}")
	private String password;

	@Value("${mqtt.topic}")
	private String topic;

	@Bean
	public MqttPahoClientFactory mqttClientFactory() {
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setServerURIs(new String[] {brokerUrl});
		connectOptions.setAutomaticReconnect(true);
		connectOptions.setCleanSession(true);
		connectOptions.setConnectionTimeout(10);
		connectOptions.setKeepAliveInterval(20);
		if (!username.isBlank()) {
			connectOptions.setUserName(username);
		}
		if (!password.isBlank()) {
			connectOptions.setPassword(password.toCharArray());
		}

		DefaultMqttPahoClientFactory mqttClientFactory = new DefaultMqttPahoClientFactory();
		mqttClientFactory.setConnectionOptions(connectOptions);
		return mqttClientFactory;
	}

	@Bean
	public TaskExecutor mqttTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("mqtt-worker-");
		taskExecutor.setCorePoolSize(4);
		taskExecutor.setMaxPoolSize(8);
		taskExecutor.setQueueCapacity(200);
		taskExecutor.initialize();
		return taskExecutor;
	}

	@Bean
	public MessageChannel mqttInputChannel(TaskExecutor mqttTaskExecutor) {
		// Process incoming MQTT messages on worker threads instead of the broker callback thread.
		return new ExecutorChannel(mqttTaskExecutor);
	}

	@Bean
	public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
		MqttPahoClientFactory mqttClientFactory,
		MessageChannel mqttInputChannel
	) {
		MqttPahoMessageDrivenChannelAdapter adapter =
			new MqttPahoMessageDrivenChannelAdapter(clientId + "-inbound", mqttClientFactory, topic);

		DefaultPahoMessageConverter messageConverter = new DefaultPahoMessageConverter();
		messageConverter.setPayloadAsBytes(false);

		adapter.setCompletionTimeout(5_000);
		adapter.setConverter(messageConverter);
		adapter.setQos(1);
		adapter.setOutputChannel(mqttInputChannel);
		return adapter;
	}

	@Bean
	public MessageHandler mqttMessageHandler(EnergyMeasurementMqttService energyMeasurementMqttService) {
		return message -> {
			String receivedTopic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
			String payload = String.valueOf(message.getPayload());
			energyMeasurementMqttService.handleMessage(receivedTopic, payload);
		};
	}

	@Bean
	public IntegrationFlow mqttInboundFlow(MessageChannel mqttInputChannel, MessageHandler mqttMessageHandler) {
		return IntegrationFlow
			.from(mqttInputChannel)
			.handle(mqttMessageHandler)
			.get();
	}
}
