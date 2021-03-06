package com.logicaldoc.webservice.folder;

import java.io.IOException;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

import com.logicaldoc.webservice.auth.Right;

/**
 * Folder Web Service client.
 * 
 * @author Matteo Caruso - Logical Objects
 * @since 5.2
 */
public class FolderClient implements FolderService {

	private FolderService client;

	public FolderClient(String endpoint) throws IOException {
		JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();

		factory.getInInterceptors().add(new LoggingInInterceptor());
		factory.getOutInterceptors().add(new LoggingOutInterceptor());
		factory.setServiceClass(FolderService.class);
		factory.setAddress(endpoint);
		client = (FolderService) factory.create();
	}

	@Override
	public WSFolder create(String sid, WSFolder folder) throws Exception {
		return client.create(sid, folder);
	}

	@Override
	public void delete(String sid, long folderId) throws Exception {
		client.delete(sid, folderId);
	}

	@Override
	public WSFolder getFolder(String sid, long folderId) throws Exception {
		return client.getFolder(sid, folderId);
	}

	@Override
	public boolean isReadable(String sid, long folderId) throws Exception {
		return client.isReadable(sid, folderId);
	}

	@Override
	public WSFolder[] list(String sid, long folderId) throws Exception {
		return client.list(sid, folderId);
	}

	@Override
	public void move(String sid, long folderId, long parentId) throws Exception {
		client.move(sid, folderId, parentId);
	}

	@Override
	public void rename(String sid, long folderId, String name) throws Exception {
		client.rename(sid, folderId, name);
	}

	@Override
	public WSFolder[] listChildren(String sid, long folderId) throws Exception {
		return client.listChildren(sid, folderId);
	}

	@Override
	public WSFolder getRootFolder(String sid) throws Exception {
		return client.getRootFolder(sid);
	}

	@Override
	public boolean isWriteable(String sid, long folderId) throws Exception {
		return client.isWriteable(sid, folderId);
	}

	@Override
	public WSFolder[] getPath(String sid, long folderId) throws Exception {
		return client.getPath(sid, folderId);
	}

	@Override
	public Right[] getGrantedGroups(String sid, long folderId) throws Exception {
		return client.getGrantedGroups(sid, folderId);
	}

	@Override
	public Right[] getGrantedUsers(String sid, long folderId) throws Exception {
		return client.getGrantedUsers(sid, folderId);
	}

	@Override
	public void grantGroup(String sid, long folderId, long groupId, int permissions, boolean recursive)
			throws Exception {
		client.grantGroup(sid, folderId, groupId, permissions, recursive);
	}

	@Override
	public void grantUser(String sid, long folderId, long userId, int permissions, boolean recursive) throws Exception {
		client.grantUser(sid, folderId, userId, permissions, recursive);
	}

	@Override
	public void update(String sid, WSFolder folder) throws Exception {
		client.update(sid, folder);
	}

	@Override
	public WSFolder createPath(String sid, long parentId, String path) throws Exception {
		return client.createPath(sid, parentId, path);
	}
}