package com.logicaldoc.gui.frontend.client.services;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.logicaldoc.gui.common.client.ServerException;
import com.logicaldoc.gui.common.client.beans.GUIRetentionPolicy;

/**
 * The client side stub for the RetentionPolicies Service. This service gives
 * all needed methods to handle the retention policies.
 */
@RemoteServiceRelativePath("retentionpolicies")
public interface RetentionPoliciesService extends RemoteService {
	/**
	 * Deletes a given policy
	 */
	public void delete(long id) throws ServerException;

	/**
	 * Creates or updates a retention policy
	 */
	public GUIRetentionPolicy save(GUIRetentionPolicy policy) throws ServerException;

	/**
	 * Loads a given policy from the database
	 */
	public GUIRetentionPolicy getPolicy(long id) throws ServerException;

	/**
	 * Reorder the policies
	 */
	public void reorder(long[] ids) throws ServerException;
	
	/**
	 * Changes a policy enabled/disabled status
	 */
	public void changeStatus(long id, boolean enabled) throws ServerException;

}