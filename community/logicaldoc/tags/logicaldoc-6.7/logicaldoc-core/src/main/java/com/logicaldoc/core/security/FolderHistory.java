package com.logicaldoc.core.security;

import com.logicaldoc.core.document.AbstractHistory;

/**
 * History entry due to an event on a folder.
 * 
 * @author Marco Meschieri - Logical Objects
 * @since 6.4
 */
public class FolderHistory extends AbstractHistory {	

	@Override
	public Object clone() throws CloneNotSupportedException {
		FolderHistory history = new FolderHistory();
		history.setDate(getDate());
		history.setDocId(getDocId());
		history.setFolderId(getFolderId());
		history.setUser(getUser());
		history.setEvent(getEvent());
		history.setComment(getComment());
		history.setTitle(getTitle());
		history.setVersion(getVersion());
		history.setPath(getPath());
		history.setNotified(getNotified());
		history.setSessionId(getSessionId());
		history.setNew(getNew());
		history.setFilename(getFilename());
		history.setUserId(getUserId());
		history.setUserName(getUserName());

		return history;
	}
}