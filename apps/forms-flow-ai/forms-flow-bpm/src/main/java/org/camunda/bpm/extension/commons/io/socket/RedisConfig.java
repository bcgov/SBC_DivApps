package org.camunda.bpm.extension.commons.io.socket;

import org.camunda.bpm.extension.commons.io.ITaskEvent;
import org.camunda.bpm.extension.commons.io.event.TaskEventTopicListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.Properties;

/**
 * Configuration for Message Broker.
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@Configuration
public class RedisConfig implements ITaskEvent {

    @Autowired
    private Properties messageBrokerProperties;

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(messageBrokerProperties.getProperty("messageBroker.host"),
                Integer.valueOf(messageBrokerProperties.getProperty("messageBroker.port")));
        redisStandaloneConfiguration.setPassword(messageBrokerProperties.getProperty("messageBroker.passcode"));
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            @Qualifier("taskMessageListenerAdapter") MessageListenerAdapter taskMessageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(taskMessageListenerAdapter, new PatternTopic(getTopicNameForTask()));
        return container;
    }


    @Bean("taskMessageListenerAdapter")
    MessageListenerAdapter chatMessageListenerAdapter(TaskEventTopicListener taskEventTopicListener) {
        return new MessageListenerAdapter(taskEventTopicListener, getExecutorName());
    }

    @Bean
    StringRedisTemplate template(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    private String getExecutorName() { return "receiveTaskMessage";}


}