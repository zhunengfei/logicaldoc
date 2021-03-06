package com.logicaldoc.core.text.parser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logicaldoc.util.charset.CharsetDetector;
import com.logicaldoc.util.charset.CharsetMatch;

/**
 * Class for parsing text (*.txt) files.
 * 
 * @author Michael Scholz
 * @author Alessandro Gasparini - Logical Objects
 * @since 3.5
 */
public class TXTParser extends AbstractParser {

	protected static Log logger = LogFactory.getLog(TXTParser.class);

	public void parse(File file) {

		FileInputStream fis = null;
		BufferedInputStream bis = null;
		try {
			fis = new FileInputStream(file);
			bis = new BufferedInputStream(fis);

			// Determine the most probable encoding
			String msEncoding = null;
			try {
				CharsetDetector cd = new CharsetDetector();
				cd.setText(bis);
				CharsetMatch cm = cd.detect();
				if (cm != null) {
					if (Charset.isSupported(cm.getName()))
						msEncoding = cm.getName();
				}
			} catch (Throwable th) {
			}
			logger.debug("Detected encoding: " + msEncoding);
			Reader reader = extractText(bis, null, msEncoding);

			content = readText(reader, "UTF-8");
		} catch (Exception ex) {
			logger.warn("Failed to extract TXT text content", ex);
			content = "";
		} finally {
			try {
				if (bis != null)
					bis.close();
				if (fis != null)
					fis.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Wraps the given input stream to an {@link InputStreamReader} using the
	 * given encoding, or the platform default encoding if the encoding is not
	 * given or is unsupported.
	 * 
	 * @param stream
	 *            binary stream
	 * @param type
	 *            ignored
	 * @param encoding
	 *            character encoding, optional
	 * @return reader for the plain text content
	 * @throws IOException
	 *             if the binary stream can not be closed in case of an encoding
	 *             issue
	 */
	public Reader extractText(InputStream stream, String type, String encoding) throws IOException {
		try {
			if (encoding != null) {
				return new InputStreamReader(stream, encoding);
			}
		} catch (UnsupportedEncodingException e) {
			logger.warn("Unsupported encoding '" + encoding + "', using default ("
					+ System.getProperty("file.encoding") + ") instead.");
		}
		return new InputStreamReader(stream);
	}
}