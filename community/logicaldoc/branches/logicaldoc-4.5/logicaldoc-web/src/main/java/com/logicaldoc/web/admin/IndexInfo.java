package com.logicaldoc.web.admin;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logicaldoc.core.document.Document;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.searchengine.Indexer;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.SettingsConfig;
import com.logicaldoc.web.SessionManagement;
import com.logicaldoc.web.i18n.Messages;

/**
 * Informations about full-text indexes and basic administration commands
 * 
 * @author Michael Scholz
 */
public class IndexInfo {
	protected static Log log = LogFactory.getLog(IndexInfo.class);

	private Indexer indexer;

	public IndexInfo() {
		indexer = (Indexer) Context.getInstance().getBean(Indexer.class);
	}

	public String getIndexDir() {
		SettingsConfig conf = (SettingsConfig) Context.getInstance().getBean(
				SettingsConfig.class);
		return conf.getValue("indexdir");
	}

	public String unlock() {
		if (SessionManagement.isValid()) {
			try {
				Indexer indexer = (Indexer) Context.getInstance().getBean(
						Indexer.class);
				indexer.unlock();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				Messages.addError(e.getMessage());
			}
		} else {
			return "login";
		}

		return null;
	}

	/**
	 * Optimises all indexed
	 * 
	 * @return
	 */
	public String optimize() {
		if (SessionManagement.isValid()) {
			try {
				Indexer indexer = (Indexer) Context.getInstance().getBean(
						Indexer.class);
				indexer.optimize();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				Messages.addError(e.getMessage());
			}
		} else {
			return "login";
		}

		return null;
	}

	/**
	 * Recreated all indexes in a new Thread
	 */
	public String recreate() {
		if (SessionManagement.isValid()) {
			final SettingsConfig conf = (SettingsConfig) Context.getInstance()
					.getBean(SettingsConfig.class);
			try {
				Indexer indexer = (Indexer) Context.getInstance().getBean(
						Indexer.class);
				indexer.recreateIndexes();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				Messages.addError(e.getMessage());
			}

			Runnable task = new Runnable() {
				public void run() {
					try {
						DocumentDAO documentDao = (DocumentDAO) Context
								.getInstance().getBean(DocumentDAO.class);
						Collection<Document> documents = documentDao.findAll();
						Iterator<Document> iter = documents.iterator();
						while (iter.hasNext()) {
							Document document = iter.next();
							documentDao.initialize(document);
							document.setIndexed(0);
							documentDao.store(document);
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						Messages.addError(e.getMessage());
					}
				}
			};

			Thread recreateThread = new Thread(task);
			recreateThread.start();
		} else {
			return "login";
		}

		return null;
	}

	public int getDocCount() {
		return indexer.getCount();
	}

	public boolean getLocked() {
		return indexer.isLocked();
	}
}