package com.baeldung.auth.config;

import java.util.NoSuchElementException;

import javax.servlet.ServletContext;

import org.keycloak.Config;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.baeldung.auth.config.KeycloakServerProperties.AdminUser;

public class EmbeddedKeycloakApplication extends KeycloakApplication {

	private static final Logger LOG = LoggerFactory.getLogger(EmbeddedKeycloakApplication.class);

	static KeycloakServerProperties keycloakServerProperties;

	protected void loadConfig() {
		JsonConfigProviderFactory factory = new RegularJsonConfigProviderFactory();
        Config.init(factory.create()
            .orElseThrow(() -> new NoSuchElementException("No value present")));
	}

	public EmbeddedKeycloakApplication() {

		super();
		// https://github.com/keycloak/keycloak/commit/35f622f48eda1a054efcd56fafe0ede9f8686b75
		// https://github.com/keycloak/keycloak/blob/35f622f48eda1a054efcd56fafe0ede9f8686b75/testsuite/utils/src/main/java/org/keycloak/testsuite/TestPlatform.java
		ServletContext context = Resteasy.getContextData(ServletContext.class);
		context.setAttribute(KeycloakSessionFactory.class.getName(), KeycloakApplication.getSessionFactory());

		createMasterRealmAdminUser();

		createBaeldungRealm();
	}

	private void createMasterRealmAdminUser() {

		KeycloakSession session = getSessionFactory().create();

		ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);

		AdminUser admin = keycloakServerProperties.getAdminUser();

		try {
			session.getTransactionManager().begin();
			applianceBootstrap.createMasterRealmUser(admin.getUsername(), admin.getPassword());
			session.getTransactionManager().commit();
		} catch (Exception ex) {
			LOG.warn("Couldn't create keycloak master admin user: {}", ex.getMessage());
			session.getTransactionManager().rollback();
		}

		session.close();
	}

	private void createBaeldungRealm() {
		KeycloakSession session = getSessionFactory().create();

		try {
			session.getTransactionManager().begin();

			RealmManager manager = new RealmManager(session);
			Resource lessonRealmImportFile = new ClassPathResource(keycloakServerProperties.getRealmImportFile());

			manager.importRealm(
					JsonSerialization.readValue(lessonRealmImportFile.getInputStream(), RealmRepresentation.class));

			session.getTransactionManager().commit();
		} catch (Exception ex) {
			LOG.warn("Failed to import Realm json file: {}", ex.getMessage());
			session.getTransactionManager().rollback();
		}

		session.close();
	}
}
