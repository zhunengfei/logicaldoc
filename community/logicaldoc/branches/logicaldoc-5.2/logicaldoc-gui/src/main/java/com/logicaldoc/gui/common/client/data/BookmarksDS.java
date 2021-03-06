package com.logicaldoc.gui.common.client.data;

import com.logicaldoc.gui.common.client.Session;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;

/**
 * Datasource to retrieve the bookmarks of the current user.
 * 
 * @author Marco Meschieri - Logical Objects
 * @since 6.0
 */
public class BookmarksDS extends DataSource {

	private static BookmarksDS instance;

	public static BookmarksDS get() {
		if (instance == null)
			instance = new BookmarksDS();
		return instance;
	}

	private BookmarksDS() {
		setTitleField("name");
		setRecordXPath("/list/bookmark");
		DataSourceTextField name = new DataSourceTextField("name");
		DataSourceTextField id = new DataSourceTextField("id");
		id.setPrimaryKey(true);
		id.setHidden(true);
		id.setRequired(true);
		DataSourceImageField icon = new DataSourceImageField("icon");
		DataSourceTextField description = new DataSourceTextField("description");
		DataSourceIntegerField position = new DataSourceIntegerField("position");
		DataSourceTextField userId = new DataSourceTextField("userId");
		DataSourceTextField docId = new DataSourceTextField("docId");
		DataSourceTextField folderId = new DataSourceTextField("folderId");

		setFields(id, name, description, icon, userId, docId, position, folderId);
		setClientOnly(true);
		setDataURL("data/bookmarks.xml?sid=" + Session.get().getSid());
	}
}