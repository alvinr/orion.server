/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.webide.server.openid;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator for the server servlet bundle. Responsible for tracking the HTTP
 * service and registering/unregistering servlets.
 */
public class Activator implements BundleActivator {
	public static final String PI_OPENID_SERVLETS = "org.eclipse.e4.webide.server.openid"; //$NON-NLS-1$

	public static volatile BundleContext bundleContext;
	static Activator singleton;

	public static Activator getDefault() {
		return singleton;
	}

	public BundleContext getContext() {
		return bundleContext;
	}

	public void start(BundleContext context) throws Exception {
		singleton = this;
		bundleContext = context;

	}

	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}
}
