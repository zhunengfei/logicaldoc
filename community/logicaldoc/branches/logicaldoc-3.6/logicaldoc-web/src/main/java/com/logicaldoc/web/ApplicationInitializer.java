package com.logicaldoc.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.util.Log4jConfigurer;

import com.logicaldoc.core.SystemProperty;

/**
 * Listener that initialises relevant system stuffs during application startup
 * 
 * @author Alessandro Gasparini
 * @version $Id:$
 * @since 3.0
 */
public class ApplicationInitializer implements ServletContextListener {
	private static final String WEB_INF_BOOT_PROPERTIES = "WEB-INF/boot.properties";

	/**
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent sce) {
		Log4jConfigurer.shutdownLogging();
	}

	public static Properties loadBootProperties(ServletContext context) {
		Properties boot = new Properties();

		try {
			boot.load(new FileInputStream(context
					.getRealPath(WEB_INF_BOOT_PROPERTIES)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return boot;
	}

	/**
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		Properties boot = loadBootProperties(context);
		String repository = boot
				.getProperty(SystemProperty.LOGICALDOC_REPOSITORY);

		// replace system properties
		if (repository.indexOf("$") != -1) {
			repository = StrSubstitutor.replaceSystemProperties(repository);
		}

		boot.setProperty(SystemProperty.LOGICALDOC_REPOSITORY,
				initRepositoryPath(repository));
		boot.setProperty(SystemProperty.LOGICALDOC_APP_ROOTDIR,
				initRootPath(context));
		boot.setProperty(SystemProperty.LOGICALDOC_APP_PLUGINSDIR,
				initPluginsPath(context));
		boot.setProperty(SystemProperty.LOGICALDOC_PLUGINSREGISTRY,
				initPluginRegistry());

		try {
			String log4jPath = context
					.getRealPath("/WEB-INF/classes/log4j.xml");
			System.out.println("log4jPath = " + log4jPath);
			Log4jConfigurer.initLogging(log4jPath);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		saveBootProperties(boot, context);

		// Initialize plugins
		com.logicaldoc.util.PluginRegistry.getInstance().init();
	}

	private String initPluginRegistry() {
		System.setProperty(SystemProperty.LOGICALDOC_PLUGINSREGISTRY,
				"com.logicaldoc.web.PluginRegistry");

		return "com.logicaldoc.web.PluginRegistry";
	}

	public static void saveBootProperties(Properties boot,
			ServletContext context) {
		// Save properties for the next bootstrap
		try {
			boot.store(new FileOutputStream(context
					.getRealPath(WEB_INF_BOOT_PROPERTIES)), "");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected String initRepositoryPath(String logicaldocHome) {
		String homePath = StringUtils.replace(logicaldocHome, "\\", "/");
		homePath = StringUtils.removeEnd(homePath, "/");
		System.err.println("LOGICALDOC_REPOSITORY = " + homePath);
		System.setProperty(SystemProperty.LOGICALDOC_REPOSITORY, homePath);

		return homePath;
	}

	protected String initRootPath(final ServletContext context) {
		String rootPath = StringUtils.replace(context
				.getRealPath(StringUtils.EMPTY), "\\", "/");
		rootPath = StringUtils.removeEnd(rootPath, "/");
		System.setProperty(SystemProperty.LOGICALDOC_APP_ROOTDIR, rootPath);

		return rootPath;
	}

	protected String initPluginsPath(final ServletContext context) {
		String pluginsPath = StringUtils.replace(context
				.getRealPath(StringUtils.EMPTY), "\\", "/");
		pluginsPath = StringUtils.removeEnd(pluginsPath, "/");
		pluginsPath += "/WEB-INF/plugins";

		File dir = new File(pluginsPath);
		dir.mkdirs();
		dir.mkdir();
		System.setProperty(SystemProperty.LOGICALDOC_APP_PLUGINSDIR,
				pluginsPath);

		return pluginsPath;
	}
}