package com.logicaldoc.web.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logicaldoc.core.security.Menu;
import com.logicaldoc.core.security.dao.MenuDAO;
import com.logicaldoc.util.Context;
import com.logicaldoc.web.SessionManagement;
import com.logicaldoc.web.StyleBean;
import com.logicaldoc.web.i18n.Messages;
import com.logicaldoc.web.util.FacesUtil;

/**
 * <p>
 * The MenuBarBean class determines which menu item fired the ActionEvent and
 * stores the modified id information in a String. MenuBarBean also controls the
 * orientation of the Menu Bar.
 * </p>
 * 
 * @author Marco Meschieri - Logical Objects
 * @since 3.0
 */
public class MenuBarBean {
	protected static Log logger = LogFactory.getLog(MenuBarBean.class);

	// records which menu item fired the event
	private String actionFired;

	// records the param value for the menu item which fired the event
	private String param;

	// orientation of the menubar ("horizontal" or "vertical")
	private String orientation = "horizontal";

	// list for the dynamic menus w/ getter & setter
	protected List<MenuItem> model = new ArrayList<MenuItem>();

	private NavigationBean navigation;

	/**
	 * Get the param value for the menu item which fired the event.
	 * 
	 * @return the param value
	 */
	public String getParam() {
		return param;
	}

	/**
	 * Set the param value.
	 */
	public void setParam(String param) {
		this.param = param;
	}

	/**
	 * Get the modified ID of the fired action.
	 * 
	 * @return the modified ID
	 */
	public String getActionFired() {
		return actionFired;
	}

	public List<MenuItem> getModel() {
		if (model.isEmpty()) {
			createMenuItems();
		}

		return model;
	}

	public void setModel(List<MenuItem> model) {
		this.model = model;
	}

	/**
	 * Identify the ID of the element that fired the event and return it in a
	 * form suitable for display.
	 * 
	 * @param e the event that fired the listener
	 */
	public void primaryListener(ActionEvent e) {
		MenuItem menu = (MenuItem) e.getSource();

		if (StringUtils.isNotEmpty(menu.getContent().getTemplate())) {
			navigation.setSelectedPanel(menu.getContent());
		}
	}

	/**
	 * Get the orientation of the menu ("horizontal" or "vertical")
	 * 
	 * @return the orientation of the menu
	 */
	public String getOrientation() {
		return orientation;
	}

	/**
	 * Set the orientation of the menu ("horizontal" or "vertical").
	 * 
	 * @param orientation the new orientation of the menu
	 */
	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	/**
	 * Finds all first-level menus accessible by the current user
	 * 
	 * @return
	 */
	protected void createMenuItems() {
		model.clear();
		StyleBean style = (StyleBean) Context.getInstance().getBean(StyleBean.class);
		long userId = SessionManagement.getUserId();
		PageContentBean page = new PageContentBean("home", "home");
		page.setContentTitle(Messages.getMessage("home"));
		page.setDisplayText(Messages.getMessage("home"));
		page.setIcon(style.getImagePath("home.png"));

		MenuItem item = createMenuItem(" " + Messages.getMessage("home"), null, "#{menuBar.primaryListener}", null,
				null, style.getImagePath("home.png"), false, null, null, page);
		model.add(item);

		try {
			if (userId > 0) {
				MenuDAO menuDao = (MenuDAO) Context.getInstance().getBean(MenuDAO.class);
				Collection<Menu> menus = menuDao.findByUserId(userId, Menu.MENUID_HOME, Menu.MENUTYPE_MENU);

				for (Menu menu : menus) {
					if (menu.getId() != Menu.MENUID_HOME)
						createMenuStructure(menu, null);
				}
			}
		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
		}

		MenuItem helpMenu = createMenuItem(" " + Messages.getMessage("help"), "m-help", "#{menuBar.primaryListener}",
				null, null, style.getImagePath("help.png"), false, null, null);

		String helpUrl = style.getProductHelp();
		helpMenu.getChildren().add(
				createMenuItem(" " + Messages.getMessage("help.online"), "m-helpcontents", null, null, helpUrl, style
						.getImagePath("help.png"), false, "_blank", null));
		helpMenu.getChildren().add(
				createMenuItem(" " + Messages.getMessage("bug.report"), "m-bugreport", null, null, "http://bugs.logicaldoc.com", style
						.getImagePath("bug.png"), false, "_blank", null));

		PageContentBean infoPage = new PageContentBean("info", "info");
		infoPage.setContentTitle(Messages.getMessage("info"));

		String product = style.getProductName();
		helpMenu.getChildren().add(
				createMenuItem(" " + Messages.getMessage("about") + " " + product, "m-about",
						"#{menuBar.primaryListener}", null, null, style.getImagePath("info.png"), false, null,
						"LDLargeMenuItem", infoPage));
		model.add(helpMenu);
	}

	private void createMenuStructure(Menu menu, MenuItem parent) {
		StyleBean style = (StyleBean) Context.getInstance().getBean(StyleBean.class);
		PageContentBean page;
		MenuItem item;
		page = new PageContentBean("m-" + Long.toString(menu.getId()));

		if (StringUtils.isNotEmpty(menu.getRef())) {
			page.setTemplate(menu.getRef());
		}

		page.setContentTitle(Messages.getMessage(menu.getText()));
		page.setIcon(style.getImagePath(menu.getIcon()));

		item = createMenuItem(" " + Messages.getMessage(menu.getText()), "m-" + Long.toString(menu.getId()),
				"#{menuBar.primaryListener}", null, null, (menu.getIcon() != null) ? style.getImagePath(menu.getIcon())
						: null, false, null, null, page);

		if (parent == null) {
			model.add(item);
		} else {
			parent.getChildren().add(item);
		}

		// For 'Documents' menu, skip children
		if (menu.getId() != Menu.MENUID_DOCUMENTS) {
			MenuDAO menuDao = (MenuDAO) Context.getInstance().getBean(MenuDAO.class);
			Collection<Menu> children = menuDao.findByUserId(SessionManagement.getUserId(), menu.getId());

			for (Menu child : children) {
				if (child.getId() != menu.getId())
					createMenuStructure(child, item);
			}
		}
	}

	protected MenuItem createMenuItem(String label, String id, String actionListener, String action, String link,
			String icon, boolean immediate, String target, PageContentBean content) {
		return createMenuItem(label, id, actionListener, action, link, icon, immediate, target, null, content);
	}

	protected MenuItem createMenuItem(String label, String id, String actionListener, String action, String link,
			String icon, boolean immediate, String target, String styleClass, PageContentBean content) {
		MenuItem menuItem = new MenuItem();
		menuItem.setValue(label);

		if (id != null) {
			menuItem.setId(id);
		}

		if (actionListener != null) {
			menuItem.setActionListener(FacesUtil.createActionListenerMethodBinding(actionListener));
		}

		if (action != null) {
			menuItem.setAction(FacesUtil.createActionMethodBinding(action));
		}

		if (link != null) {
			menuItem.setLink(link);
		}

		if (icon != null) {
			menuItem.setIcon(icon);
		}

		menuItem.setImmediate(immediate);

		if (target != null) {
			menuItem.setTarget(target);
		}

		if (content != null) {
			menuItem.setContent(content);
		}

		if (styleClass != null)
			menuItem.setStyleClass(styleClass);
		else {
			if (label.length() > 15)
				menuItem.setStyleClass("LDLargeMenuItem");
			else
				menuItem.setStyleClass("LDNormalMenuItem");
		}
		return menuItem;
	}

	public void setNavigation(NavigationBean navigation) {
		this.navigation = navigation;
	}
}
