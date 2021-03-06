package com.logicaldoc.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logicaldoc.core.document.DiscussionComment;
import com.logicaldoc.core.document.Document;
import com.logicaldoc.core.document.dao.DiscussionThreadDAO;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.generic.Generic;
import com.logicaldoc.core.generic.dao.GenericDAO;
import com.logicaldoc.util.Context;
import com.logicaldoc.web.document.DocumentRecord;
import com.logicaldoc.web.document.DocumentsRecordsManager;

public class HomeBean {

	protected static Log log = LogFactory.getLog(HomeBean.class);

	private boolean messagesExpanded = true;

	private boolean lastCommentsExpanded = false;

	private boolean lastDocumentsExpanded = false;

	private boolean lastDownloadsExpanded = false;

	private boolean lastLockedExpanded = false;

	private boolean tagCloudsExpanded = true;

	public boolean isMessagesExpanded() {
		return messagesExpanded;
	}

	public void setMessagesExpanded(boolean messagesExpanded) {
		this.messagesExpanded = messagesExpanded;
	}

	/**
	 * Retrieves the list of last changed documents (by user in session) from
	 * the database
	 */
	public List<DocumentRecord> getLastModifiedDocuments() {

		List<DocumentRecord> lastModified = new ArrayList<DocumentRecord>();

		HashMap hm = new HashMap();

		if (SessionManagement.isValid()) {
			try {
				long userId = SessionManagement.getUserId();

				DocumentDAO docdao = (DocumentDAO) Context.getInstance().getBean(DocumentDAO.class);
				Collection<Document> documents = (Collection<Document>) docdao.findLastModifiedByUserId(userId, 10);
				for (Document document : documents) {
					if (!hm.containsKey(document.getId())) {
						DocumentRecord dr = new DocumentRecord(document.getId(), null,
								DocumentsRecordsManager.GROUP_INDENT_STYLE_CLASS,
								DocumentsRecordsManager.GROUP_ROW_STYLE_CLASS);
						hm.put(document.getId(), dr);

						lastModified.add(dr);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		return lastModified;
	}

	/**
	 * Retrieves the list of last accessed documents (by user in session) from
	 * the database
	 */
	public List<DocumentRecord> getLastDownloads() {
		List<DocumentRecord> lastDownloads = new ArrayList<DocumentRecord>();

		if (SessionManagement.isValid()) {
			try {
				long userId = SessionManagement.getUserId();
				DocumentDAO docDao = (DocumentDAO) Context.getInstance().getBean(DocumentDAO.class);

				Collection<Document> docColl = docDao.findLastDownloadsByUserId(userId, 10);
				for (Document doc : docColl) {
					lastDownloads.add(new DocumentRecord(doc.getId(), null,
							DocumentsRecordsManager.GROUP_INDENT_STYLE_CLASS,
							DocumentsRecordsManager.GROUP_ROW_STYLE_CLASS));
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		return lastDownloads;
	}

	/**
	 * Retrieves the list of last comments from the database
	 */
	public Collection<DiscussionComment> getLastComments() {
		List<DiscussionComment> lastcomments = new ArrayList<DiscussionComment>();

		if (SessionManagement.isValid()) {
			try {
				long userId = SessionManagement.getUserId();
				DiscussionThreadDAO artDao = (DiscussionThreadDAO) Context.getInstance().getBean(
						DiscussionThreadDAO.class);
				Collection<DiscussionComment> articles = artDao.findCommentsByUserId(userId, 10);

				if (articles != null) {
					// revert the list, it should be in asc order by time
					lastcomments.addAll(articles);
					Collections.reverse(lastcomments);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		return lastcomments;
	}

	/**
	 * Retrieves the list of Locked documents (by user in session) from the
	 * database
	 */
	public List<DocumentRecord> getLockedDocs() {
		List<DocumentRecord> lastdocs = new ArrayList<DocumentRecord>();
		if (SessionManagement.isValid()) {
			try {
				DocumentDAO docdao = (DocumentDAO) Context.getInstance().getBean(DocumentDAO.class);
				Collection<Document> documents = (Collection<Document>) docdao.findLockedByUserId(SessionManagement
						.getUserId());
				for (Document document : documents) {
					lastdocs.add(new DocumentRecord(document.getId(), null,
							DocumentsRecordsManager.GROUP_INDENT_STYLE_CLASS,
							DocumentsRecordsManager.GROUP_ROW_STYLE_CLASS));
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return lastdocs;
	}

	public boolean isLastCommentsExpanded() {
		return lastCommentsExpanded;
	}

	public void setLastCommentsExpanded(boolean lastCommentsExpanded) {
		this.lastCommentsExpanded = lastCommentsExpanded;
	}

	public boolean isLastDocumentsExpanded() {
		return lastDocumentsExpanded;
	}

	public void setLastDocumentsExpanded(boolean lastDocumentsExpanded) {
		this.lastDocumentsExpanded = lastDocumentsExpanded;
	}

	public boolean isLastDownloadsExpanded() {
		return lastDownloadsExpanded;
	}

	public void setLastDownloadsExpanded(boolean lastDownloadsExpanded) {
		this.lastDownloadsExpanded = lastDownloadsExpanded;
	}

	/**
	 * Generate tag clouds from lists of tags
	 */
	public List<TagCloud> getTagClouds() {
		List<TagCloud> tags = new ArrayList<TagCloud>();
		GenericDAO gendao = (GenericDAO) Context.getInstance().getBean(GenericDAO.class);
		Generic generic = gendao.findByAlternateKey("tagcloud", "-");
		if (generic == null)
			return tags;
		else
			gendao.initialize(generic);

		for (String tag : generic.getAttributeNames()) {
			TagCloud tc = new TagCloud(tag);
			StringTokenizer st = new StringTokenizer(generic.getValue(tag), "|", false);
			tc.setOccurence(Integer.parseInt(st.nextToken()));
			tc.setScale(Integer.parseInt(st.nextToken()));
			tags.add(tc);
		}

		// Sort the tags collection by name
		Comparator<TagCloud> compName = new TagCloudComparatorName();
		Collections.sort(tags, compName);

		return tags;
	}

	public boolean isTagCloudsExpanded() {
		return tagCloudsExpanded;
	}

	public void setTagCloudsExpanded(boolean tagCloudsExpanded) {
		this.tagCloudsExpanded = tagCloudsExpanded;
	}

	class TagCloudComparatorName implements Comparator<TagCloud> {
		public int compare(TagCloud tc0, TagCloud tc1) {
			return tc0.getTag().compareTo(tc1.getTag());
		}
	}

	public boolean isLastLockedExpanded() {
		return lastLockedExpanded;
	}

	public void setLastLockedExpanded(boolean lastLockedExpanded) {
		this.lastLockedExpanded = lastLockedExpanded;
	}
}
