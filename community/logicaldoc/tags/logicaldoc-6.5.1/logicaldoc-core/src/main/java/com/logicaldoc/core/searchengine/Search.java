package com.logicaldoc.core.searchengine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.java.plugin.registry.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import com.logicaldoc.core.ExtendedAttribute;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.security.User;
import com.logicaldoc.core.security.dao.UserDAO;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.ContextProperties;
import com.logicaldoc.util.plugin.PluginRegistry;

/**
 * This class executes a search against the full-text indexes
 * 
 * @author Michael Scholz
 */
public abstract class Search {
	protected static Logger log = LoggerFactory.getLogger(Search.class);

	protected boolean moreHitsPresent = false;

	protected SearchOptions options;

	protected List<Hit> hits = new ArrayList<Hit>();

	protected long estimatedHitsNumber = 0;

	protected long execTime = 0;

	protected User searchUser;

	protected String suggestion;

	public static Search get(SearchOptions opt) {
		// Acquire the 'Search' extensions of the core plugin
		PluginRegistry registry = PluginRegistry.getInstance();
		Collection<Extension> extensions = new ArrayList<Extension>();
		try {
			extensions = registry.getExtensions("logicaldoc-core", "Search");
		} catch (Throwable e) {
			log.error(e.getMessage());
		}

		Search search = null;

		for (Extension ext : extensions) {
			int type = Integer.parseInt(ext.getParameter("type").valueAsString());
			if (type != opt.getType())
				continue;

			String className = ext.getParameter("class").valueAsString();
			try {
				search = (Search) Class.forName(className).newInstance();
				search.setOptions(opt);
			} catch (Throwable e) {
				log.error(e.getMessage());
			}
			break;
		}

		return search;
	}

	public static SearchOptions newOptions(int type) {
		// Acquire the 'Search' extensions of the core plugin
		PluginRegistry registry = PluginRegistry.getInstance();
		Collection<Extension> extensions = new ArrayList<Extension>();
		try {
			extensions = registry.getExtensions("logicaldoc-core", "Search");
		} catch (Throwable e) {
			log.error(e.getMessage());
		}

		SearchOptions options = null;

		for (Extension ext : extensions) {
			int t = Integer.parseInt(ext.getParameter("type").valueAsString());
			if (t != type)
				continue;

			String className = ext.getParameter("options").valueAsString();
			try {
				options = (SearchOptions) Class.forName(className).newInstance();
				options.setType(type);
			} catch (Throwable e) {
				log.error(e.getMessage());
			}
			break;
		}

		return options;
	}

	protected Search() {
	}

	/**
	 * Perform the search
	 * 
	 * @return The list of hits
	 */
	public final List<Hit> search() {
		log.info("Launch search");
		log.info("Expression: " + options.getExpression());

		UserDAO uDao = (UserDAO) Context.getInstance().getBean(UserDAO.class);
		searchUser = uDao.findById(options.getUserId());
		uDao.initialize(searchUser);
		if (searchUser == null) {
			log.warn("Unexisting user");
			return hits;
		} else {
			log.info("Search User: " + searchUser.getUserName());
		}

		Date start = new Date();
		hits.clear();
		suggestion = null;
		moreHitsPresent = false;

		try {
			internalSearch();
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		ContextProperties config = (ContextProperties) Context.getInstance().getBean(ContextProperties.class);
		String extattrs = config.getProperty("search.extattr");

		if (StringUtils.isNotEmpty(extattrs) && !hits.isEmpty()) {
			// the names of the extended attributes to show
			List<String> attrs = Arrays.asList(extattrs.trim().split(","));

			log.debug("Start searching for extended attributes: " + attrs);

			// populate a map of id-Hit
			final Map<Long, Hit> map = new HashMap<Long, Hit>();
			for (Hit h : hits) {
				map.put(h.getId(), h);
			}

			DocumentDAO ddao = (DocumentDAO) Context.getInstance().getBean(DocumentDAO.class);
			StringBuffer query = new StringBuffer(
					"select ld_docid, ld_name, ld_type, ld_stringvalue, ld_intvalue, ld_doublevalue, ld_datevalue ");
			query.append(" from ld_document_ext where ld_docid in ");
			query.append(map.keySet().toString().replace('[', '(').replace(']', ')'));
			query.append(" and ld_name in ");
			query.append(attrs.toString().replaceAll("\\[", "('").replaceAll("\\]", "')").replaceAll(",", "','").replaceAll(" ", ""));

			ddao.query(query.toString(), null, new RowMapper<Long>() {
				@Override
				public Long mapRow(ResultSet rs, int row) throws SQLException {
					Long docId = rs.getLong(1);
					String name = rs.getString(2);

					Hit hit = map.get(docId);
					if (hit == null)
						return null;

					ExtendedAttribute ext = new ExtendedAttribute();
					ext.setType(rs.getInt(3));
					ext.setStringValue(rs.getString(4));
					ext.setIntValue(rs.getLong(5));
					ext.setDoubleValue(rs.getDouble(6));
					ext.setDateValue(rs.getDate(7));
					hit.getAttributes().put(name, ext);

					return null;
				}
			}, null);

			log.debug("End searching for extended attributes");
		}

		Date finish = new Date();
		execTime = finish.getTime() - start.getTime();
		log.info("Search finished in " + execTime + "ms");

		return hits;
	}

	/**
	 * Concrete implementations must give a particular search algorithm that
	 * populates the hits list.
	 */
	protected abstract void internalSearch() throws Exception;

	public List<Hit> getHits() {
		return hits;
	}

	public boolean isMoreHitsPresent() {
		return moreHitsPresent;
	}

	public void setMoreHitsPresent(boolean moreHitsPresent) {
		this.moreHitsPresent = moreHitsPresent;
	}

	public long getEstimatedHitsNumber() {
		return estimatedHitsNumber;
	}

	/**
	 * Query execution time in milliseconds
	 */
	public long getExecTime() {
		return execTime;
	}

	public SearchOptions getOptions() {
		return options;
	}

	public void setOptions(SearchOptions options) {
		this.options = options;
	}

	public String getSuggestion() {
		return suggestion;
	}
}