/******************************************************************************
 * Copyright (c) 2006, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at 
 * http://www.eclipse.org/legal/epl-v10.html and the Apache License v2.0
 * is available at http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses. 
 * 
 * Contributors:
 *   VMware Inc.
 *****************************************************************************/

package org.eclipse.gemini.blueprint.compendium.internal.cm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.eclipse.gemini.blueprint.compendium.config.MockConfigurationAdmin;
import org.eclipse.gemini.blueprint.compendium.internal.cm.ManagedServiceBeanManager;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.eclipse.gemini.blueprint.mock.MockBundleContext;

public class ConfigurationAdminManagerTest extends TestCase {

	private ConfigurationAdminManager cam;
	private String pid;
	private MockBundleContext bundleContext;
	private Map services;
	private Configuration cfg;


	protected void setUp() throws Exception {
		services = new LinkedHashMap();
		MockControl mc = MockControl.createNiceControl(Configuration.class);
		cfg = (Configuration) mc.getMock();
		mc.expectAndReturn(cfg.getProperties(), new Properties());
		mc.replay();
		bundleContext = new MockBundleContext() {

			public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
				services.put(service, properties);
				return super.registerService(clazzes, service, properties);
			}

			public Object getService(ServiceReference reference) {
				String[] clazzes = (String[]) reference.getProperty(Constants.OBJECTCLASS);
				if (clazzes[0].equals(ConfigurationAdmin.class.getName())) {
					return new MockConfigurationAdmin() {

						public Configuration getConfiguration(String pid) throws IOException {
							return cfg;
						}
					};
				}
				else
					return super.getService(reference);
			}

		};

		pid = "Peter Pan";
		cam = new ConfigurationAdminManager(pid, bundleContext);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testManagedServiceRegistration() throws Exception {
		assertTrue(services.isEmpty());
		assertNotNull(cam.getConfiguration());
		assertNotNull(services);
		assertFalse(services.isEmpty());
		assertEquals(1, services.size());
	}

	public void testManagedServiceProperties() {
		assertTrue(services.isEmpty());
		assertNotNull(cam.getConfiguration());

		Dictionary props = (Dictionary) services.values().iterator().next();
		assertEquals(pid, props.get(Constants.SERVICE_PID));
	}

	public void testManagedServiceInstance() {
		assertTrue(services.isEmpty());
		assertNotNull(cam.getConfiguration());
		Object serviceInstance = services.keySet().iterator().next();
		assertTrue(serviceInstance instanceof ManagedService);
	}

	public void testUpdateCallback() throws Exception {
		final List holder = new ArrayList(4);

		ManagedServiceBeanManager msbm = new ManagedServiceBeanManager() {

			public Object register(Object bean) {
				return null;
			}

			public void unregister(Object bean) {
			}

			public void updated(Map properties) {
				holder.add(properties);
			}
		};
		cam.setBeanManager(msbm);
		assertTrue(services.isEmpty());
		assertNotNull(cam.getConfiguration());

		ManagedService callback = (ManagedService) services.keySet().iterator().next();
		Dictionary props = new Properties();
		props.put("foo", "bar");
		props.put("spring", "source");

		assertTrue(holder.isEmpty());
		callback.updated(props);
		assertEquals(1, holder.size());
		assertEquals(props, holder.get(0));
	}
}
