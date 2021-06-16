package org.camunda.bpm.extension.keycloak.showcase;



import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.Properties;


/**
 * The Camunda Showcase Spring Boot application.
 */
@Configuration
public class CamundaExtConfiguration {

	/**
	 * Secondary datasource.
	 * This is used only for publishing data to analytics.
	 * @return
	 */
	@Bean("analyticsDS")
	@ConfigurationProperties("analytics.datasource")
	public DataSource analyticsDS(){
		return DataSourceBuilder.create().build();
	}


	/**
	 * JDBC template for analytics datasource interaction.
	 * @param analyticsDS
	 * @return
	 */
	@Bean("analyticsJdbcTemplate")
	public NamedParameterJdbcTemplate analyticsJdbcTemplate(@Qualifier("analyticsDS") DataSource analyticsDS) {
		return new NamedParameterJdbcTemplate(analyticsDS);
	}

	@Bean
	@ConfigurationProperties(prefix = "websocket")
	public Properties messageBrokerProperties() {
		return new Properties();
	}


}
