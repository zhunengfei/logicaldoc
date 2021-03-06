package com.logicaldoc.web;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Log4jConfigurer;

import com.logicaldoc.util.config.ContextProperties;
import com.logicaldoc.util.config.LoggingConfigurator;
import com.logicaldoc.util.io.ZipUtil;
import com.logicaldoc.util.plugin.PluginRegistry;

/**
 * Listener that initialises relevant system stuffs during application startup
 * 
 * @author Alessandro Gasparini - Logical Objects
 * @since 3.0
 */
public class ApplicationInitializer implements ServletContextListener, HttpSessionListener {

	private static Log log = LogFactory.getLog(ApplicationInitializer.class);

	public static boolean needRestart = false;

	/**
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent sce) {
		Log4jConfigurer.shutdownLogging();
	}

	/**
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();

		// Initialize logging
		String log4jPath = null;
		try {
			URL configFile = null;
			try {
				configFile = LoggingConfigurator.class.getClassLoader().getResource("/log.xml");
			} catch (Throwable t) {
			}

			if (configFile == null)
				configFile = LoggingConfigurator.class.getClassLoader().getResource("log.xml");

			log4jPath = URLDecoder.decode(configFile.getPath(), "UTF-8");

			// Setup the correct logs folder
			ContextProperties config = new ContextProperties();
			LoggingConfigurator lconf = new LoggingConfigurator();
			lconf.setLogsRoot(config.getProperty("conf.logdir"));
			lconf.write();

			Log4jConfigurer.initLogging(log4jPath);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		// Prepare the plugins dir
		String pluginsDir = context.getRealPath("/WEB-INF/lib");

		// Initialize plugins
		PluginRegistry.getInstance().init(pluginsDir);

		// Reinitialize logging because some plugins may have added new
		// categories
		try {
			Log4jConfigurer.shutdownLogging();
			Log4jConfigurer.initLogging(log4jPath);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		// Clean the upload folder
		File uploadDir = new File(context.getRealPath("upload"));
		try {
			if (uploadDir.exists())
				FileUtils.forceDelete(uploadDir);
		} catch (IOException e) {
			log.warn(e.getMessage());
			e.printStackTrace();
		}

		needRestart = PluginRegistry.getInstance().isRestartRequired();

		// Try to unpack new plugins
		try {
			unpackPlugins(context);
		} catch (IOException e) {
			log.warn(e.getMessage());
			e.printStackTrace();
		}

		if (needRestart)
			System.out.println("The application needs to be restarted");
	}

	/**
	 * Unpacks the plugin that are newly installed and positioned in the plugins
	 * folder
	 * 
	 * @throws IOException
	 */
	private void unpackPlugins(ServletContext context) throws IOException {
		File pluginsDir = PluginRegistry.getPluginsDir();
		File[] archives = pluginsDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.contains("plugin"))
					return true;
				else
					return false;
			}
		});
		File webappDir = new File(context.getRealPath("/"));
		if (archives != null)
			for (File archive : archives) {
				System.out.println("Unpack plugin " + archive.getName());
				ZipUtil.unzip(archive.getPath(), webappDir.getPath());
				FileUtils.forceDelete(archive);
				needRestart = true;
			}
	}

	@Override
	public void sessionCreated(HttpSessionEvent event) {

	}

	/**
	 * Frees temporary upload folders.
	 */
	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession session = event.getSession();
		String id = session.getId();
		File uploadFolder = new File(session.getServletContext().getRealPath("upload"));
		uploadFolder = new File(uploadFolder, id);
		try {
			if (uploadFolder.exists())
				FileUtils.forceDelete(uploadFolder);
		} catch (Throwable e) {
			log.warn(e.getMessage());
		}
	}
}