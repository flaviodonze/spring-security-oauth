/*******************************************************************************
 * $URL: $
 *
 * Copyright (c) 2020 SCODi Solutions AG, CH-6375 Beckenried
 *******************************************************************************/
package com.baeldung.auth.config;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;

/**
 *
 *
 * @author  created by Author: fdo, last update by $Author: $
 * @version $Revision: $, $Date: $
 */
/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSessionServletFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		servletRequest.setCharacterEncoding("UTF-8");

		final HttpServletRequest request = (HttpServletRequest) servletRequest;

		KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) servletRequest.getServletContext()
				.getAttribute(KeycloakSessionFactory.class.getName());
		KeycloakSession session = sessionFactory.create();
		Resteasy.pushContext(KeycloakSession.class, session);
		ClientConnection connection = new ClientConnection() {
			@Override
			public String getRemoteAddr() {
				return request.getRemoteAddr();
			}

			@Override
			public String getRemoteHost() {
				return request.getRemoteHost();
			}

			@Override
			public int getRemotePort() {
				return request.getRemotePort();
			}

			@Override
			public String getLocalAddr() {
				return request.getLocalAddr();
			}

			@Override
			public int getLocalPort() {
				return request.getLocalPort();
			}
		};

		Resteasy.pushContext(ClientConnection.class, connection);

		KeycloakTransaction tx = session.getTransactionManager();
		Resteasy.pushContext(KeycloakTransaction.class, tx);
		tx.begin();

		try {
			filterChain.doFilter(servletRequest, servletResponse);
		} finally {
			if (servletRequest.isAsyncStarted()) {
				servletRequest.getAsyncContext().addListener(createAsyncLifeCycleListener(session));
			} else {
				closeSession(session);
			}
		}
	}

	private AsyncListener createAsyncLifeCycleListener(final KeycloakSession session) {
		return new AsyncListener() {
			@Override
			public void onComplete(AsyncEvent event) {
				closeSession(session);
			}

			@Override
			public void onTimeout(AsyncEvent event) {
				closeSession(session);
			}

			@Override
			public void onError(AsyncEvent event) {
				closeSession(session);
			}

			@Override
			public void onStartAsync(AsyncEvent event) {
			}
		};
	}

	private void closeSession(KeycloakSession session) {
		// KeycloakTransactionCommitter is responsible for committing the transaction, but if an exception is thrown it's not invoked and transaction
		// should be rolled back
		if (session.getTransactionManager() != null && session.getTransactionManager().isActive()) {
			session.getTransactionManager().rollback();
		}

		session.close();
		Resteasy.clearContextData();
	}

	@Override
	public void destroy() {
	}
}