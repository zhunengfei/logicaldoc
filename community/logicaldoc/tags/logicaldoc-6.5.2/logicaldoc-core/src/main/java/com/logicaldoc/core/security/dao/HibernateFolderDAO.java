package com.logicaldoc.core.security.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import com.logicaldoc.core.ExtendedAttribute;
import com.logicaldoc.core.HibernatePersistentObjectDAO;
import com.logicaldoc.core.document.Document;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.security.Folder;
import com.logicaldoc.core.security.FolderGroup;
import com.logicaldoc.core.security.FolderHistory;
import com.logicaldoc.core.security.Group;
import com.logicaldoc.core.security.Permission;
import com.logicaldoc.core.security.User;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.ContextProperties;
import com.logicaldoc.util.sql.SqlUtil;

/**
 * Hibernate implementation of <code>FolderDAO</code>
 * 
 * @author Marco Meschieri - Logical Objects
 * @since 6.0
 */
public class HibernateFolderDAO extends HibernatePersistentObjectDAO<Folder> implements FolderDAO {

	private UserDAO userDAO;

	private FolderHistoryDAO historyDAO;

	private ContextProperties config;

	protected HibernateFolderDAO() {
		super(Folder.class);
		super.log = LoggerFactory.getLogger(HibernateFolderDAO.class);
	}

	public UserDAO getUserDAO() {
		return userDAO;
	}

	public void setUserDAO(UserDAO userDAO) {
		this.userDAO = userDAO;
	}

	@Override
	public boolean store(Folder folder) {
		return store(folder, null);
	}

	@Override
	public boolean store(Folder folder, FolderHistory transaction) {
		boolean result = true;

		try {
			if (folder.getSecurityRef() != null)
				folder.getFolderGroups().clear();

			if (transaction != null) {
				folder.setCreator(transaction.getUser().getFullName());
				folder.setCreatorId(transaction.getUserId());
				if (folder.getId() == 0 && transaction.getEvent() == null)
					transaction.setEvent(FolderHistory.EVENT_FOLDER_CREATED);
			}

			getHibernateTemplate().saveOrUpdate(folder);
			saveFolderHistory(folder, transaction);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
			result = false;
		}

		return result;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Folder> findByUserId(long userId) {
		List<Folder> coll = new ArrayList<Folder>();

		try {
			User user = userDAO.findById(userId);
			if (user == null)
				return coll;

			// The administrators can see all folders
			if (user.isInGroup("admin"))
				return findAll();

			Set<Group> precoll = user.getGroups();
			if (!precoll.isEmpty()) {
				// First of all collect all folders that define it's own
				// policies
				StringBuffer query = new StringBuffer("select distinct(_folder) from Folder _folder  ");
				query.append(" left join _folder.folderGroups as _group ");
				query.append(" where _group.groupId in (");

				boolean first = true;
				Iterator iter = precoll.iterator();
				while (iter.hasNext()) {
					if (!first)
						query.append(",");
					Group ug = (Group) iter.next();
					query.append(Long.toString(ug.getId()));
					first = false;
				}
				query.append(")");
				coll = (List<Folder>) getHibernateTemplate().find(query.toString());

				if (coll.isEmpty()) {
					return coll;
				} else {

					// Now collect all folders that references the policies of
					// the
					// previously found folders
					List<Folder> tmp = new ArrayList<Folder>();
					query = new StringBuffer("select _folder from Folder _folder  where _folder.securityRef in (");
					first = true;
					for (Folder folder : coll) {
						if (!first)
							query.append(",");
						query.append(Long.toString(folder.getId()));
						first = false;
					}
					query.append(")");
					tmp = (List<Folder>) getHibernateTemplate().find(query.toString());

					for (Folder folder : tmp) {
						if (!coll.contains(folder))
							coll.add(folder);
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return coll;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Folder> findByUserId(long userId, long parentId) {
		List<Folder> coll = new ArrayList<Folder>();

		try {
			User user = userDAO.findById(userId);
			if (user == null)
				return coll;
			if (user.isInGroup("admin"))
				return findByWhere("_entity.id!=_entity.parentId and _entity.parentId=" + parentId,
						" order by _entity.name ", null);
			/*
			 * Search for all those folderes that defines its own security
			 * policies
			 */
			StringBuffer query1 = new StringBuffer();
			Set<Group> precoll = user.getGroups();
			if (precoll.isEmpty())
				return coll;

			query1.append("select distinct(_entity) from Folder _entity ");
			query1.append(" left join _entity.folderGroups as _group");
			query1.append(" where _group.groupId in (");

			boolean first = true;
			Iterator iter = precoll.iterator();
			while (iter.hasNext()) {
				if (!first)
					query1.append(",");
				Group ug = (Group) iter.next();
				query1.append(Long.toString(ug.getId()));
				first = false;
			}
			query1.append(") and _entity.parentId = ? and _entity.id!=_entity.parentId");

			coll = (List<Folder>) getHibernateTemplate().find(query1.toString(), parentId);

			/*
			 * Now search for all other folders that references accessible
			 * folders
			 */
			StringBuffer query2 = new StringBuffer(
					"select _entity from Folder _entity where _entity.deleted=0 and _entity.parentId=? ");
			query2.append(" and _entity.securityRef in (");
			query2.append("    select distinct(B.id) from Folder B ");
			query2.append(" left join B.folderGroups as _group");
			query2.append(" where _group.groupId in (");

			first = true;
			iter = precoll.iterator();
			while (iter.hasNext()) {
				if (!first)
					query2.append(",");
				Group ug = (Group) iter.next();
				query2.append(Long.toString(ug.getId()));
				first = false;
			}
			query2.append("))");

			List<Folder> coll2 = (List<Folder>) getHibernateTemplate().find(query2.toString(), new Long[] { parentId });
			for (Folder folder : coll2) {
				if (!coll.contains(folder))
					coll.add(folder);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		Collections.sort(coll, new Comparator<Folder>() {
			@Override
			public int compare(Folder o1, Folder o2) {
				return -1 * o1.getName().compareTo(o2.getName());
			}
		});
		return coll;
	}

	@Override
	public List<Folder> findChildren(long parentId, Integer max) {
		return findByWhere("_entity.parentId = ? and _entity.id!=_entity.parentId", new Object[] { parentId },
				"order by _entity.name", max);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<Folder> findChildren(long parentId, long userId) {
		List<Folder> coll = new ArrayList<Folder>();
		try {
			User user = userDAO.findById(userId);
			if (user.isInGroup("admin"))
				return findChildren(parentId, null);

			Set<Group> groups = user.getGroups();
			if (groups.isEmpty())
				return coll;

			/*
			 * Search for the folders that define its own policies
			 */
			StringBuffer query1 = new StringBuffer("select distinct(_entity) from Folder _entity  ");
			query1.append(" left join _entity.folderGroups as _group ");
			query1.append(" where _group.groupId in (");

			boolean first = true;
			Iterator iter = groups.iterator();
			while (iter.hasNext()) {
				if (!first)
					query1.append(",");
				Group ug = (Group) iter.next();
				query1.append(Long.toString(ug.getId()));
				first = false;
			}
			query1.append(") and _entity.parentId=" + parentId);
			query1.append(" and not(_entity.id=" + parentId + ")");

			coll = (List<Folder>) getHibernateTemplate().find(query1.toString(), null);

			/*
			 * Now search for all other folders that references accessible
			 * folders
			 */
			StringBuffer query2 = new StringBuffer(
					"select _entity from Folder _entity where _entity.deleted=0 and _entity.parentId=? ");
			query2.append(" and _entity.securityRef in (");
			query2.append("    select distinct(B.id) from Folder B ");
			query2.append(" left join B.folderGroups as _group");
			query2.append(" where _group.groupId in (");

			first = true;
			iter = groups.iterator();
			while (iter.hasNext()) {
				if (!first)
					query2.append(",");
				Group ug = (Group) iter.next();
				query2.append(Long.toString(ug.getId()));
				first = false;
			}
			query2.append("))");
			query2.append(" and not(_entity.id=" + parentId + ")");

			List<Folder> coll2 = (List<Folder>) getHibernateTemplate().find(query2.toString(), new Long[] { parentId });
			for (Folder folder : coll2) {
				if (!coll.contains(folder))
					coll.add(folder);
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
			return coll;
		}
		return coll;
	}

	@Override
	public List<Folder> findByParentId(long parentId) {
		List<Folder> coll = new ArrayList<Folder>();
		List<Folder> temp = findChildren(parentId, null);
		Iterator<Folder> iter = temp.iterator();

		while (iter.hasNext()) {
			Folder folder = iter.next();
			coll.add(folder);

			List<Folder> coll2 = findByParentId(folder.getId());

			if (coll2 != null) {
				coll.addAll(coll2);
			}
		}

		return coll;
	}

	@Override
	public boolean isWriteEnable(long folderId, long userId) {
		return isPermissionEnabled(Permission.WRITE, folderId, userId);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean isReadEnable(long folderId, long userId) {
		boolean result = true;
		try {
			User user = userDAO.findById(userId);
			if (user == null)
				return false;
			if (user.isInGroup("admin"))
				return true;

			long id = folderId;
			Folder folder = findById(folderId);
			if (folder.getSecurityRef() != null)
				id = folder.getSecurityRef().longValue();

			Set<Group> Groups = user.getGroups();
			if (Groups.isEmpty())
				return false;

			StringBuffer query = new StringBuffer("select distinct(_entity) from Folder _entity  ");
			query.append(" left join _entity.folderGroups as _group ");
			query.append(" where _group.groupId in (");

			boolean first = true;
			Iterator iter = Groups.iterator();
			while (iter.hasNext()) {
				if (!first)
					query.append(",");
				Group ug = (Group) iter.next();
				query.append(Long.toString(ug.getId()));
				first = false;
			}
			query.append(") and _entity.id=?");

			List<FolderGroup> coll = (List<FolderGroup>) getHibernateTemplate().find(query.toString(),
					new Object[] { new Long(id) });
			result = coll.size() > 0;
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
			result = false;
		}

		return result;
	}

	@Override
	public Collection<Long> findFolderIdByUserId(long userId) {
		return findFolderIdByUserIdAndPermission(userId, Permission.READ);
	}

	@Override
	public boolean hasWriteAccess(Folder folder, long userId) {
		if (isWriteEnable(folder.getId(), userId) == false) {
			return false;
		}

		List<Folder> children = findByParentId(folder.getId());

		for (Folder subFolder : children) {
			if (!hasWriteAccess(subFolder, userId)) {
				return false;
			}
		}

		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Folder> findByGroupId(long groupId) {
		List<Folder> coll = new ArrayList<Folder>();

		// The administrators can see all folderes
		if (groupId == Group.GROUPID_ADMIN)
			return findAll();

		try {
			/*
			 * Search for folderes that define its own security policies
			 */
			StringBuffer query = new StringBuffer("select distinct(_entity) from Folder _entity  ");
			query.append(" left join _entity.folderGroups as _group ");
			query.append(" where _entity.deleted=0 and _group.groupId =" + groupId);

			coll = (List<Folder>) getHibernateTemplate().find(query.toString(), null);

			/*
			 * Now search for all other folderes that references the previous
			 * ones
			 */
			if (!coll.isEmpty()) {
				StringBuffer query2 = new StringBuffer("select _entity from Folder _entity where _entity.deleted=0 ");
				query2.append(" and _entity.securityRef in (");
				boolean first = true;
				for (Folder folder : coll) {
					if (!first)
						query2.append(",");
					query2.append(Long.toString(folder.getId()));
					first = false;
				}
				query2.append(")");
				List<Folder> coll2 = (List<Folder>) getHibernateTemplate().find(query2.toString(), null);
				for (Folder folder : coll2) {
					if (!coll.contains(folder))
						coll.add(folder);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return coll;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Long> findIdByUserId(long userId, long parentId) {
		List<Long> ids = new ArrayList<Long>();
		try {
			User user = userDAO.findById(userId);
			if (user == null)
				return ids;
			if (user.isInGroup("admin"))
				return findIdsByWhere("_entity.parentId=" + parentId, null, null);

			StringBuffer query1 = new StringBuffer();
			Set<Group> precoll = user.getGroups();
			Iterator iter = precoll.iterator();
			if (!precoll.isEmpty()) {
				query1 = new StringBuffer("select distinct(A.ld_folderid) from ld_foldergroup A, ld_folder B "
						+ " where B.ld_deleted=0 and A.ld_folderid=B.ld_id AND (B.ld_parentid=" + parentId
						+ " OR B.ld_id=" + parentId + ")" + " AND A.ld_groupid in (");
				boolean first = true;
				while (iter.hasNext()) {
					if (!first)
						query1.append(",");
					Group ug = (Group) iter.next();
					query1.append(Long.toString(ug.getId()));
					first = false;
				}
				query1.append(")");

				ids = (List<Long>) queryForList(query1.toString(), Long.class);

				/*
				 * Now find all folders referencing the previously found ones
				 */
				StringBuffer query2 = new StringBuffer("select B.ld_id from ld_folder B where B.ld_deleted=0 ");
				query2.append(" and B.ld_parentid=" + parentId);
				query2.append("	and B.ld_securityref in (");
				query2.append(query1.toString());
				query2.append(")");

				List<Long> folderids2 = (List<Long>) queryForList(query2.toString(), Long.class);
				for (Long folderid : folderids2) {
					if (!ids.contains(folderid))
						ids.add(folderid);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return ids;
	}

	@Override
	public List<Folder> findByName(String name) {
		return findByName(null, name, true);
	}

	@Override
	public List<Folder> findByName(Folder parent, String name, boolean caseSensitive) {
		StringBuffer query = null;
		if (caseSensitive)
			query = new StringBuffer("_entity.name like '" + SqlUtil.doubleQuotes(name) + "' ");
		else
			query = new StringBuffer("lower(_entity.name) like '" + SqlUtil.doubleQuotes(name.toLowerCase()) + "' ");

		if (parent != null)
			query.append(" AND _entity.parentId = " + parent.getId());
		return findByWhere(query.toString(), null, null);
	}

	@Override
	public String computePathExtended(long folderId) {
		Folder folder = findById(folderId);
		if (folder == null)
			return null;
		String path = folderId != Folder.ROOTID ? folder.getName() : "";
		while (folder != null && folder.getId() != folder.getParentId() && folder.getId() != Folder.ROOTID) {
			folder = findById(folder.getParentId());
			if (folder != null)
				path = (folder.getId() != Folder.ROOTID ? folder.getName() : "") + "/" + path;
		}
		if (!path.startsWith("/"))
			path = "/" + path;
		return path;
	}

	/**
	 * Utility method that logs into the DB the transaction that involved the
	 * passed folder. The transaction must be provided with userId and userName.
	 * 
	 * @param folder
	 * @param transaction
	 */
	private void saveFolderHistory(Folder folder, FolderHistory transaction) {
		if (transaction == null || !historyDAO.isEnabled() || "bulkload".equals(config.getProperty("runlevel")))
			return;

		transaction.setNotified(0);
		transaction.setFolderId(folder.getId());
		transaction.setTitle(folder.getId() != Folder.ROOTID ? folder.getName() : "/");
		String deletedFolderPathExtended = null;
		if (StringUtils.isEmpty(transaction.getPath()))
			transaction.setPath(computePathExtended(folder.getId()));
		else
			deletedFolderPathExtended = transaction.getPath();
		transaction.setComment("");

		historyDAO.store(transaction);

		// Check if is necessary to add a new history entry for the parent
		// folder. This operation is not recursive, because we want to notify
		// only the parent folder.
		if (folder.getId() != folder.getParentId() && folder.getId() != Folder.ROOTID) {
			Folder parent = findById(folder.getParentId());
			// The parent folder can be 'null' when the user wants to delete a
			// folder with sub-folders under it (method 'deleteAll()').
			if (parent != null) {
				FolderHistory parentHistory = new FolderHistory();
				parentHistory.setFolderId(parent.getId());
				parentHistory.setTitle(parent.getId() != Folder.ROOTID ? parent.getName() : "/");
				if (deletedFolderPathExtended != null)
					parentHistory.setPath(deletedFolderPathExtended);
				else
					parentHistory.setPath(computePathExtended(folder.getId()));

				parentHistory.setUser(transaction.getUser());
				if (transaction.getEvent().equals(FolderHistory.EVENT_FOLDER_CREATED)
						|| transaction.getEvent().equals(FolderHistory.EVENT_FOLDER_MOVED)) {
					parentHistory.setEvent(FolderHistory.EVENT_FOLDER_SUBFOLDER_CREATED);
				} else if (transaction.getEvent().equals(FolderHistory.EVENT_FOLDER_RENAMED)) {
					parentHistory.setEvent(FolderHistory.EVENT_FOLDER_SUBFOLDER_RENAMED);
				} else if (transaction.getEvent().equals(FolderHistory.EVENT_FOLDER_PERMISSION)) {
					parentHistory.setEvent(FolderHistory.EVENT_FOLDER_SUBFOLDER_PERMISSION);
				} else if (transaction.getEvent().equals(FolderHistory.EVENT_FOLDER_DELETED)) {
					parentHistory.setEvent(FolderHistory.EVENT_FOLDER_SUBFOLDER_DELETED);
				}
				parentHistory.setComment("");
				parentHistory.setSessionId(transaction.getSessionId());
				parentHistory.setComment(transaction.getComment());

				historyDAO.store(parentHistory);
			}
		}
	}

	@Override
	public List<Folder> findByNameAndParentId(String name, long parentId) {
		return findByWhere("_entity.parentId = " + parentId + " and _entity.name like '" + SqlUtil.doubleQuotes(name)
				+ "'", null, null);
	}

	@Override
	public List<Folder> findParents(long folderId) {
		Folder folder = findById(folderId);
		List<Folder> coll = new ArrayList<Folder>();
		try {
			while (folder.getId() != Folder.ROOTID && folder.getId() != folder.getParentId()) {
				folder = findById(folder.getParentId());
				if (folder != null)
					coll.add(0, folder);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return coll;
	}

	@Override
	public boolean isPermissionEnabled(Permission permission, long folderId, long userId) {
		Set<Permission> permissions = getEnabledPermissions(folderId, userId);
		return permissions.contains(permission);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restore(long folderId, boolean parents) {
		bulkUpdate("set ld_deleted=0 where ld_id=" + folderId, null);

		// Restore parents
		if (parents) {
			String query = "select ld_parentid from ld_folder where ld_id =" + folderId;
			List<Long> folders = (List<Long>) super.queryForList(query, Long.class);
			for (Long id : folders) {
				if (id.longValue() != folderId)
					restore(id, parents);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public Set<Permission> getEnabledPermissions(long folderId, long userId) {
		Set<Permission> permissions = new HashSet<Permission>();

		try {
			User user = userDAO.findById(userId);
			if (user == null)
				return permissions;

			// If the user is an administrator bypass all controls
			if (user.isInGroup("admin")) {
				return Permission.all();
			}

			Set<Group> groups = user.getGroups();
			if (groups.isEmpty())
				return permissions;

			// If the folder defines a security ref, use another folder to find
			// the policies
			long id = folderId;
			Folder folder = findById(folderId);
			if (folder.getSecurityRef() != null) {
				id = folder.getSecurityRef().longValue();
				log.debug("Use the security reference " + id);
			}

			StringBuffer query = new StringBuffer(
					"select A.ld_write as LDWRITE, A.ld_add as LDADD, A.ld_security as LDSECURITY, A.ld_immutable as LDIMMUTABLE, A.ld_delete as LDDELETE, A.ld_rename as LDRENAME, A.ld_import as LDIMPORT, A.ld_export as LDEXPORT, A.ld_sign as LDSIGN, A.ld_archive as LDARCHIVE, A.ld_workflow as LDWORKFLOW, A.ld_download as LDDOWNLOAD");
			query.append(" from ld_foldergroup A");
			query.append(" where ");
			query.append(" A.ld_folderid=" + id);
			query.append(" and A.ld_groupid in (");

			boolean first = true;
			Iterator<Group> iter = groups.iterator();
			while (iter.hasNext()) {
				if (!first)
					query.append(",");
				Group ug = (Group) iter.next();
				query.append(Long.toString(ug.getId()));
				first = false;
			}
			query.append(")");

			Connection con = null;
			Statement stmt = null;
			ResultSet rs = null;

			try {
				con = getSession().connection();
				stmt = con.createStatement();
				rs = stmt.executeQuery(query.toString());
				while (rs.next()) {
					permissions.add(Permission.READ);
					if (rs.getInt("LDADD") == 1)
						permissions.add(Permission.ADD);
					if (rs.getInt("LDEXPORT") == 1)
						permissions.add(Permission.EXPORT);
					if (rs.getInt("LDIMPORT") == 1)
						permissions.add(Permission.IMPORT);
					if (rs.getInt("LDDELETE") == 1)
						permissions.add(Permission.DELETE);
					if (rs.getInt("LDIMMUTABLE") == 1)
						permissions.add(Permission.IMMUTABLE);
					if (rs.getInt("LDSECURITY") == 1)
						permissions.add(Permission.SECURITY);
					if (rs.getInt("LDRENAME") == 1)
						permissions.add(Permission.RENAME);
					if (rs.getInt("LDWRITE") == 1)
						permissions.add(Permission.WRITE);
					if (rs.getInt("LDDELETE") == 1)
						permissions.add(Permission.DELETE);
					if (rs.getInt("LDSIGN") == 1)
						permissions.add(Permission.SIGN);
					if (rs.getInt("LDARCHIVE") == 1)
						permissions.add(Permission.ARCHIVE);
					if (rs.getInt("LDWORKFLOW") == 1)
						permissions.add(Permission.WORKFLOW);
					if (rs.getInt("LDDOWNLOAD") == 1)
						permissions.add(Permission.DOWNLOAD);
				}
			} finally {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (con != null)
					con.close();
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return permissions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Long> findFolderIdByUserIdAndPermission(long userId, Permission permission) {
		/*
		 * Important: use an HashSet because of extremely quick in existence
		 * checks.
		 */
		Set<Long> ids = new HashSet<Long>();
		try {
			User user = userDAO.findById(userId);
			if (user == null)
				return ids;

			// The administrators have all permissions on all folders
			if (user.isInGroup("admin"))
				return queryForList("select ld_id from ld_folder where ld_deleted=0", Long.class);

			Set<Group> precoll = user.getGroups();

			if (!precoll.isEmpty()) {
				/*
				 * Check folders that specify its own permissions
				 */
				StringBuffer query1 = new StringBuffer(
						"select distinct(A.ld_folderid) from ld_foldergroup A, ld_folder B "
								+ " where A.ld_folderid=B.ld_id and B.ld_deleted=0 ");
				if (permission != Permission.READ)
					query1.append(" and A.ld_" + permission.getName() + "=1 ");
				query1.append(" and A.ld_groupid in (");

				Iterator<Group> iter = precoll.iterator();
				boolean first = true;
				while (iter.hasNext()) {
					if (!first)
						query1.append(",");
					Group ug = (Group) iter.next();
					query1.append(Long.toString(ug.getId()));
					first = false;
				}
				query1.append(")");

				ids.addAll((List<Long>) queryForList(query1.toString(), Long.class));

				/*
				 * Now search for those folderes that references the previously
				 * found ones
				 */
				StringBuffer query2 = new StringBuffer("select B.ld_id from ld_folder B where B.ld_deleted=0 ");
				query2.append(" and B.ld_securityref in (" + query1.toString() + ")");

				List<Long> frefs = (List<Long>) queryForList(query2.toString(), Long.class);
				for (Long folderId : frefs) {
					if (!ids.contains(folderId))
						ids.add(folderId);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return ids;
	}

	public FolderHistoryDAO getHistoryDAO() {
		return historyDAO;
	}

	public void setHistoryDAO(FolderHistoryDAO historyDAO) {
		this.historyDAO = historyDAO;
	}

	@Override
	public void deleteAll(List<Folder> folders, FolderHistory transaction) {
		for (Folder folder : folders) {
			try {
				FolderHistory deleteHistory = (FolderHistory) transaction.clone();
				deleteHistory.setEvent(FolderHistory.EVENT_FOLDER_DELETED);
				deleteHistory.setFolderId(folder.getId());
				deleteHistory.setPath(computePathExtended(folder.getId()));
				delete(folder.getId(), deleteHistory);
			} catch (CloneNotSupportedException e) {
				log.error(e.getMessage(), e);
			}
		}

	}

	@Override
	public boolean delete(long folderId, FolderHistory transaction) {
		if (folderId == Folder.ROOTID)
			throw new RuntimeException("Not allowed to delete the Root folder");

		boolean result = true;
		try {
			Folder folder = (Folder) getHibernateTemplate().get(Folder.class, folderId);
			folder.setDeleted(1);
			transaction.setEvent(FolderHistory.EVENT_FOLDER_DELETED);
			transaction.setFolderId(folderId);
			store(folder, transaction);
		} catch (Throwable e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
			result = false;
		}

		return result;
	}

	@Override
	public boolean applyRithtToTree(long id, FolderHistory transaction) {
		boolean result = true;
		try {
			transaction.setEvent(FolderHistory.EVENT_FOLDER_PERMISSION);
			Folder parent = findById(id);
			Long securityRef = id;
			if (parent.getSecurityRef() != null)
				securityRef = parent.getSecurityRef();

			// Iterate over all children setting the security reference
			List<Folder> children = findChildren(id, null);
			for (Folder folder : children) {
				if (!securityRef.equals(folder.getSecurityRef())) {
					FolderHistory tr = (FolderHistory) transaction.clone();
					tr.setFolderId(folder.getId());
					folder.setSecurityRef(securityRef);
					folder.getFolderGroups().clear();
					store(folder, tr);
				}
				applyRithtToTree(folder.getId(), transaction);
			}
		} catch (Throwable e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
			result = false;
		}

		return result;
	}

	@Override
	public boolean applyMetadataToTree(long id, FolderHistory transaction) {
		boolean result = true;

		Folder parent = findById(id);
		if (parent == null)
			return result;

		try {
			initialize(parent);
			transaction.setEvent(FolderHistory.EVENT_FOLDER_CHANGED);

			// Iterate over all children setting the template and field values
			List<Folder> children = findChildren(id, null);
			for (Folder folder : children) {
				initialize(folder);

				FolderHistory tr = (FolderHistory) transaction.clone();
				tr.setFolderId(folder.getId());

				folder.setTemplate(parent.getTemplate());
				for (String name : parent.getAttributeNames()) {
					ExtendedAttribute ext = (ExtendedAttribute) parent.getAttributes().get(name).clone();
					folder.getAttributes().put(name, ext);
				}

				store(folder, tr);
				getHibernateTemplate().flush();

				if (!applyMetadataToTree(folder.getId(), transaction))
					return false;
			}
		} catch (Throwable e) {
			if (log.isErrorEnabled())
				log.error(e.getMessage(), e);
			result = false;
		}
		return result;
	}

	@Override
	public Folder create(Folder parent, String name, FolderHistory transaction) {
		Folder folder = new Folder();
		folder.setName(name);
		folder.setParentId(parent.getId());

		if (parent.getSecurityRef() != null)
			folder.setSecurityRef(parent.getSecurityRef());
		else
			folder.setSecurityRef(parent.getId());

		setUniqueName(folder);
		if (transaction != null)
			transaction.setEvent(FolderHistory.EVENT_FOLDER_CREATED);

		/*
		 * Replicate the parent's metadata
		 */
		if (parent.getTemplate() != null) {
			initialize(parent);
			folder.setTemplate(parent.getTemplate());
			for (String att : parent.getAttributeNames()) {
				ExtendedAttribute ext = null;
				try {
					ext = (ExtendedAttribute) parent.getAttributes().get(att).clone();
				} catch (CloneNotSupportedException e) {

				}
				folder.getAttributes().put(att, ext);
			}
		}

		if (store(folder, transaction) == false)
			return null;
		return folder;
	}

	@Override
	public Folder createPath(Folder parent, String path, FolderHistory transaction) {
		StringTokenizer st = new StringTokenizer(path, "/", false);

		initialize(parent);
		
		Folder folder = parent;
		while (st.hasMoreTokens()) {
			String name = st.nextToken();
			List<Folder> childs = findByName(folder, name, true);
			Folder dir = null;
			if (childs.isEmpty())
				try {
					dir = create(folder, name, transaction != null ? (FolderHistory) transaction.clone() : null);
				} catch (CloneNotSupportedException e) {
				}
			else {
				dir = childs.iterator().next();
				initialize(dir);
			}
			folder = dir;
		}
		return folder;
	}

	@Override
	public Folder findByPath(String pathExtended) {
		StringTokenizer st = new StringTokenizer(pathExtended, "/", false);
		Folder folder = findById(Folder.ROOTID);
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (StringUtils.isEmpty(token))
				continue;
			List<Folder> list = findByName(folder, token, true);
			if (list.isEmpty()) {
				folder = null;
				break;
			}
			folder = list.get(0);
		}
		return folder;
	}

	@Override
	public void setUniqueName(Folder folder) {
		int counter = 1;
		String folderName = folder.getName();

		List<String> collisions = (List<String>) queryForList(
				"select lower(ld_name) from ld_folder where ld_deleted=0 and ld_parentid=" + folder.getParentId()
						+ " and lower(ld_name) like'" + SqlUtil.doubleQuotes(folderName.toLowerCase()) + "%'",
				String.class);
		while (collisions.contains(folder.getName().toLowerCase())) {
			folder.setName(folderName + "(" + (counter++) + ")");
		}
	}

	@Override
	public void move(Folder source, Folder target, FolderHistory transaction) throws Exception {
		assert (source != null);
		assert (target != null);
		assert (transaction != null);
		assert (transaction.getUser() != null);

		if (isInPath(source.getId(), target.getId()))
			throw new IllegalArgumentException("Cannot move a dolder inside the same path");

		// initialize(source);

		// Change the parent folder
		source.setParentId(target.getId());

		// Ensure unique folder name in a folder
		setUniqueName(source);

		// Modify folder history entry
		transaction.setEvent(FolderHistory.EVENT_FOLDER_MOVED);

		store(source, transaction);
	}

	@Override
	public List<Folder> deleteTree(long folderId, FolderHistory transaction) throws Exception {
		List<Folder> notDeleted = deleteTree(findById(folderId), transaction);
		return notDeleted;
	}

	@Override
	public List<Folder> deleteTree(Folder folder, FolderHistory transaction) throws Exception {
		assert (folder != null);
		assert (transaction != null);
		assert (transaction.getUser() != null);

		List<Folder> deletableFolders = new ArrayList<Folder>();
		List<Folder> notDeletableFolders = new ArrayList<Folder>();

		Collection<Long> deletableIds = findFolderIdByUserIdAndPermission(transaction.getUserId(), Permission.DELETE);

		if (deletableIds.contains(folder.getId())) {
			deletableFolders.add(folder);
		} else {
			notDeletableFolders.add(folder);
			return notDeletableFolders;
		}

		try {
			// Retrieve all the sub-folders
			List<Folder> subfolders = findByParentId(folder.getId());

			for (Folder subfolder : subfolders) {
				if (deletableIds.contains(subfolder.getId())) {
					deletableFolders.add(subfolder);
				} else {
					notDeletableFolders.add(subfolder);
				}
			}

			for (Folder deletableFolder : deletableFolders) {
				boolean foundDocImmutable = false;
				boolean foundDocLocked = false;

				DocumentDAO documentDAO = (DocumentDAO) Context.getInstance().getBean(DocumentDAO.class);
				List<Document> docs = documentDAO.findByFolder(deletableFolder.getId(), null);

				for (Document doc : docs) {
					if (doc.getImmutable() == 1 && !transaction.getUser().isInGroup("admin")) {
						// If it he isn't an administrator he cannot delete a
						// folder containing immutable documents
						foundDocImmutable = true;
						continue;
					}
				}
				if (foundDocImmutable || foundDocLocked) {
					notDeletableFolders.add(deletableFolder);
				}
			}

			// Avoid deletion of the entire path of an undeletable folder
			for (Folder notDeletable : notDeletableFolders) {
				Folder parent = notDeletable;
				while (true) {
					if (deletableFolders.contains(parent))
						deletableFolders.remove(parent);
					if (parent.equals(folder))
						break;
					parent = findById(parent.getParentId());
				}
			}

			// Modify history entry
			deleteAll(deletableFolders, transaction);

			getSession().flush();

			// Delete orphaned documents
			DocumentDAO documentDAO = (DocumentDAO) Context.getInstance().getBean(DocumentDAO.class);
			documentDAO.deleteOrphaned(transaction.getUserId());

			return notDeletableFolders;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return notDeletableFolders;
		}
	}

	@Override
	public List<Folder> find(String name) {
		return findByName(null, "%" + name + "%", false);
	}

	@Override
	public boolean isInPath(long folderId, long targetId) {
		for (Folder folder : findParents(targetId)) {
			if (folder.getId() == folderId)
				return true;
		}
		return false;
	}

	@Override
	public int count(boolean computeDeleted) {
		return queryForInt("SELECT COUNT(A.ld_id) FROM ld_document A "
				+ (computeDeleted ? "" : "where A.ld_deleted = 0 "));
	}

	@Override
	public List<Folder> findWorkspaces() {
		return findByWhere(" (not _entity.id=" + Folder.ROOTID + ") and _entity.parentId=" + Folder.ROOTID
				+ " and _entity.type=" + Folder.TYPE_WORKSPACE, "order by lower(_entity.name)", null);
	}

	@Override
	public void findTreeIds(long parentId, long userId, Integer depth, Collection<Long> ids) {
		if (depth != null && depth.intValue() < 1)
			return;

		List<Folder> children = findChildren(parentId, userId);
		for (Folder folder : children) {
			if (!ids.contains(folder.getId()))
				ids.add(folder.getId());
		}

		int d = -1;
		if (depth != null)
			d = depth - 1;

		for (Folder folder : children) {
			findTreeIds(folder.getId(), userId, d, ids);
		}
	}

	@Override
	public void initialize(Folder folder) {
		getHibernateTemplate().refresh(folder);

		for (FolderGroup fg : folder.getFolderGroups()) {
			fg.getWrite();
		}

		for (String attribute : folder.getAttributes().keySet()) {
			folder.getAttributes().get(attribute).getValue();
		}
	}

	public void setConfig(ContextProperties config) {
		this.config = config;
	}
}