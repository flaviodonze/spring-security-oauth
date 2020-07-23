package com.baeldung.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import com.baeldung.auth.config.KeycloakServerProperties;

@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
@EnableConfigurationProperties({ KeycloakServerProperties.class })
public class AuthorizationServerApp extends SpringBootServletInitializer {

	private static final Logger LOG = LoggerFactory.getLogger(AuthorizationServerApp.class);

	public static void main(String[] args) throws Exception {
		SpringApplication.run(AuthorizationServerApp.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(AuthorizationServerApp.class);
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> onApplicationReadyEventListener(ServerProperties serverProperties,
			KeycloakServerProperties keycloakServerProperties) {

		return (evt) -> {

			Integer port = serverProperties.getPort();
			String keycloakContextPath = keycloakServerProperties.getContextPath();

			LOG.info("Embedded Keycloak started: http://localhost:{}{} to use keycloak", port, keycloakContextPath);
		};
	}

}
