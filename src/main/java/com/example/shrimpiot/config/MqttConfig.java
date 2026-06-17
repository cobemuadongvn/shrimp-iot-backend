package com.example.shrimpiot.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(MqttProperties.class)
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttConfig {

    @Bean
    public DefaultMqttPahoClientFactory mqttClientFactory(MqttProperties props) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{props.getBrokerUrl()});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        if (props.getUsername() != null && !props.getUsername().isBlank()) {
            options.setUserName(props.getUsername());
        }

        if (props.getPassword() != null && !props.getPassword().isBlank()) {
            options.setPassword(props.getPassword().toCharArray());
        }

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new QueueChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
            MqttProperties props,
            DefaultMqttPahoClientFactory factory,
            @Qualifier("mqttInboundChannel") MessageChannel mqttInboundChannel
    ) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        props.getBackendClientId() + "-in",
                        factory,
                        props.getTelemetryTopicPattern(),
                        props.getAckTopicPattern(),
                        props.getStatusTopicPattern()
                );

        adapter.setQos(props.getQos());
        adapter.setOutputChannel(mqttInboundChannel);
        return adapter;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler(
            MqttProperties props,
            DefaultMqttPahoClientFactory factory
    ) {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler(props.getBackendClientId() + "-out", factory);

        handler.setAsync(false);
        handler.setDefaultQos(props.getQos());
        return handler;
    }
}
