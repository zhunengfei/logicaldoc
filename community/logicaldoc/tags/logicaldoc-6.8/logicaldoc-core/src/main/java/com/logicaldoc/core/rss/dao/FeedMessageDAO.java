package com.logicaldoc.core.rss.dao;

import com.logicaldoc.core.PersistentObjectDAO;
import com.logicaldoc.core.rss.FeedMessage;

/**
 * This class is a DAO-service for feed messages.
 * 
 * @author Matteo Caruso - Logical Objects
 * @version 6.1
 */
public interface FeedMessageDAO extends PersistentObjectDAO<FeedMessage> {
	/**
	 * This method checks if there are some feed messages not already read.
	 * 
	 * @return true if at least one feed message is not already read.
	 */
	public boolean checkNotRead();

	/**
	 * This method finds a feed message by guid.
	 * 
	 * @param guid Guid of the feedc message.
	 * @return FeedMessage with given guid.
	 */
	public FeedMessage findByGuid(String guid);

	/**
	 * This method deletes all feed messages older that 1 year.
	 */
	public void deleteOld();
}