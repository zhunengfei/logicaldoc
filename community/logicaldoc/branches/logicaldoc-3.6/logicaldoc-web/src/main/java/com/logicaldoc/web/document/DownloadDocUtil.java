package com.logicaldoc.web.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.tools.ant.filters.StringInputStream;

import com.logicaldoc.core.document.Document;
import com.logicaldoc.core.document.DocumentManager;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.security.Menu;
import com.logicaldoc.core.security.UserDoc;
import com.logicaldoc.core.security.dao.MenuDAO;
import com.logicaldoc.core.security.dao.UserDocDAO;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.MimeTypeConfig;

/**
 * some helper utilities to download a document but also to add the document to
 * the recent files of the user
 * 
 * @author Sebastian Stein
 */
public class DownloadDocUtil {
	/**
	 * adds the given document to the recent files entry of the user
	 * 
	 * @param userName
	 *            the username of the user accessing the file
	 * @param menuId
	 *            id of the menu the user accessed
	 */
	public static void addToRecentFiles(String userName, int menuId) {
		UserDoc userdoc = new UserDoc();
		userdoc.setMenuId(menuId);
		userdoc.setUserName(userName);

		UserDocDAO uddao = (UserDocDAO) Context.getInstance().getBean(
				UserDocDAO.class);
		uddao.store(userdoc);
	}

	/**
	 * extracts the mimetype of the document
	 */
	public static String getMimeType(Menu docId) {
		if (docId == null) {
			return null;
		}

		String extension = docId.getMenuRef().substring(
				docId.getMenuRef().lastIndexOf(".") + 1);
		MimeTypeConfig mtc = (MimeTypeConfig) Context.getInstance().getBean(
				MimeTypeConfig.class);
		String mimetype = mtc.getMimeApp(extension);

		if ((mimetype == null) || mimetype.equals("")) {
			mimetype = "application/octet-stream";
		}

		return mimetype;
	}

	/**
	 * Sends the specified document to the response object; the client will
	 * receive it as a download
	 * 
	 * @param response
	 *            the document is written to this object
	 * @param docId
	 *            Id of the document
	 * @param docVerId
	 *            name of the version; if null the latest version will returned
	 */
	public static void downloadDocument(HttpServletResponse response,
			int docId, String docVerId) throws FileNotFoundException,
			IOException {
		// get menu and document
		MenuDAO mdao = (MenuDAO) Context.getInstance().getBean(MenuDAO.class);
		Menu menu = mdao.findByPrimaryKey(docId);
		DocumentDAO ddao = (DocumentDAO) Context.getInstance().getBean(
				DocumentDAO.class);
		Document doc = ddao.findByMenuId(docId);

		if ((menu == null) || (doc == null)) {
			throw new FileNotFoundException();
		}

		// get the mimetype
		String mimetype = DownloadDocUtil.getMimeType(menu);
		DocumentManager documentManager = (DocumentManager) Context
				.getInstance().getBean(DocumentManager.class);
		File file = documentManager.getDocumentFile(doc, docVerId);
		if (!file.exists()) {
			throw new FileNotFoundException(file.getPath());
		}

		// it seems everything is fine, so we can now start writing to the
		// response object
		response.setContentType(mimetype);
		response.setHeader("Content-Disposition", "attachment; filename=\""
				+ doc.getMenu().getMenuRef() + "\"");

		// Headers required by Internet Explorer
		response.setHeader("Pragma", "public");
		response.setHeader("Cache-Control",
				"must-revalidate, post-check=0,pre-check=0");
		response.setHeader("Expires", "0");

		InputStream is = new FileInputStream(file);
		OutputStream os;
		os = response.getOutputStream();

		int letter = 0;

		try {
			while ((letter = is.read()) != -1) {
				os.write(letter);
			}
		} finally {
			os.flush();
			os.close();
			is.close();
		}
	}

	/**
	 * Sends the specified document's indexed text to the response object; the
	 * client will receive it as a download
	 * 
	 * @param response
	 *            the document is written to this object
	 * @param docId
	 *            Id of the document
	 * @param docVerId
	 *            name of the version; if null the latest version will returned
	 */
	public static void downloadDocumentText(HttpServletResponse response,
			int docId) throws FileNotFoundException, IOException {
		// get menu and document
		MenuDAO mdao = (MenuDAO) Context.getInstance().getBean(MenuDAO.class);
		Menu menu = mdao.findByPrimaryKey(docId);
		DocumentDAO ddao = (DocumentDAO) Context.getInstance().getBean(
				DocumentDAO.class);
		Document doc = ddao.findByMenuId(docId);

		if ((menu == null) || (doc == null)) {
			throw new FileNotFoundException();
		}

		String mimetype = "text/plain";

		// it seems everything is fine, so we can now start writing to the
		// response object
		response.setContentType(mimetype);
		response.setHeader("Content-Disposition", "attachment; filename=\""
				+ doc.getMenu().getMenuRef() + ".txt" + "\"");

		// Headers required by Internet Explorer
		response.setHeader("Pragma", "public");
		response.setHeader("Cache-Control",
				"must-revalidate, post-check=0,pre-check=0");
		response.setHeader("Expires", "0");

		DocumentManager manager = (DocumentManager) Context.getInstance()
				.getBean(DocumentManager.class);
		String content = manager.getDocumentContent(doc.getDocId());

		InputStream is = new StringInputStream(content);
		OutputStream os;
		os = response.getOutputStream();
		int letter = 0;
		try {
			while ((letter = is.read()) != -1) {
				os.write(letter);
			}
		} finally {
			os.flush();
			os.close();
			is.close();
		}
	}

	/**
	 * sends the specified document to the response object; the client will
	 * receive it as a download
	 * 
	 * @param response
	 *            the document is written to this object
	 * @param docId
	 *            Id of the document
	 * @param docVerId
	 *            name of the version; if null the latest version will returned
	 */
	public static void downloadDocument(HttpServletResponse response,
			String docId, String docVerId) throws FileNotFoundException,
			IOException {
		downloadDocument(response, Integer.parseInt(docId), docVerId);
	}
}
