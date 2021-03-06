/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.cf.commands;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.orion.server.cf.CFProtocolConstants;
import org.eclipse.orion.server.cf.objects.Route;
import org.eclipse.orion.server.cf.objects.Target;
import org.eclipse.orion.server.cf.utils.HttpUtil;
import org.eclipse.orion.server.core.ServerStatus;
import org.eclipse.osgi.util.NLS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetRoutesCommand extends AbstractCFCommand {
	private final Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.cf"); //$NON-NLS-1$

	private String commandName;
	private String domainName;
	private String hostName;
	private boolean orphaned;

	private List<Route> routes;

	public GetRoutesCommand(Target target, boolean orphaned) {
		super(target);
		this.commandName = "Get Routes";
		this.orphaned = orphaned;
	}

	public GetRoutesCommand(Target target, String domainName, String hostName) {
		super(target);
		this.commandName = "Get Routes";
		this.domainName = domainName;
		this.hostName = hostName;
	}

	public List<Route> getRoutes() {
		assertWasRun();
		return routes;
	}

	@Override
	protected ServerStatus _doIt() {
		try {
			URI targetURI = URIUtil.toURI(target.getUrl());
			ServerStatus getRouteStatus;

			if(domainName != null && hostName != null){
				GetDomainsCommand getDomainsCommand = new GetDomainsCommand(target, domainName);
				ServerStatus getDomainsStatus = (ServerStatus) getDomainsCommand.doIt();
				if (!getDomainsStatus.isOK())
					return getDomainsStatus;

				String domainId = getDomainsCommand.getDomains().get(0).getGuid();

				URI routesURI = targetURI.resolve("/v2/routes");
				GetMethod getRouteMethod = new GetMethod(routesURI.toString());
				ServerStatus confStatus = HttpUtil.configureHttpMethod(getRouteMethod, target.getCloud());
				if (!confStatus.isOK())
					return confStatus;
				
				getRouteMethod.setQueryString("inline-relations-depth=1&q=" + URLEncoder.encode(CFProtocolConstants.V2_KEY_HOST + ":" + hostName + ";" + CFProtocolConstants.V2_KEY_DOMAIN_GUID + ":" + domainId, "UTF8"));

				getRouteStatus = HttpUtil.executeMethod(getRouteMethod);
				if (!getRouteStatus.isOK())
					return getRouteStatus;

			} else {
				String routesURL = target.getSpace().getCFJSON().getJSONObject(CFProtocolConstants.V2_KEY_ENTITY).getString(CFProtocolConstants.V2_KEY_ROUTES_URL);

				URI routesURI = targetURI.resolve(routesURL);
				GetMethod getRoutesMethod = new GetMethod(routesURI.toString());
				ServerStatus confStatus = HttpUtil.configureHttpMethod(getRoutesMethod, target.getCloud());
				if (!confStatus.isOK())
					return confStatus;
				
				getRoutesMethod.setQueryString("inline-relations-depth=1"); //$NON-NLS-1$

				getRouteStatus = HttpUtil.executeMethod(getRoutesMethod);
				if (!getRouteStatus.isOK())
					return getRouteStatus;

			}

			JSONObject routesJSON = getRouteStatus.getJsonData();
			if (routesJSON.getInt(CFProtocolConstants.V2_KEY_TOTAL_RESULTS) < 1) {
				return new ServerStatus(IStatus.OK, HttpServletResponse.SC_OK, null, null);
			}

			JSONObject result = new JSONObject();
			routes = new ArrayList<Route>();
			JSONArray resources = routesJSON.getJSONArray(CFProtocolConstants.V2_KEY_RESOURCES);
			for (int k = 0; k < resources.length(); ++k) {
				JSONObject routeJSON = resources.getJSONObject(k);
				Route route = new Route();
				route.setCFJSON(routeJSON);

				if (!orphaned || route.getCFJSON().getJSONObject(CFProtocolConstants.V2_KEY_ENTITY).getJSONArray(CFProtocolConstants.V2_KEY_APPS).length() == 0) {
					routes.add(route);
					result.append("Routes", route.toJSON());
				}
			}
			return new ServerStatus(Status.OK_STATUS, HttpServletResponse.SC_OK, result);
		} catch (Exception e) {
			String msg = NLS.bind("An error occured when performing operation {0}", commandName); //$NON-NLS-1$
			logger.error(msg, e);
			return new ServerStatus(IStatus.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg, e);
		}
	}
}
