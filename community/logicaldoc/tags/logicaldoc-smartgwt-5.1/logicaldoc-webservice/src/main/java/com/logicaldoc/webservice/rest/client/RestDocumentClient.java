package com.logicaldoc.webservice.rest.client;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.activation.DataHandler;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.AttachmentBuilder;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logicaldoc.webservice.model.WSDocument;
import com.logicaldoc.webservice.rest.DocumentService;

public class RestDocumentClient extends AbstractRestClient {

	protected static Logger log = LoggerFactory.getLogger(RestDocumentClient.class);

	DocumentService proxy = null;

	public RestDocumentClient(String endpoint, String username, String password) {
		this(endpoint, username, password, -1);
	}

	public RestDocumentClient(String endpoint, String username, String password, int timeout) {
		super(endpoint, username, password, timeout);

		JacksonJsonProvider provider = new JacksonJsonProvider();
		
		if ((username == null) || (password == null)) {
			proxy = JAXRSClientFactory.create(endpoint, DocumentService.class, Arrays.asList(provider));
		} else {
			proxy = JAXRSClientFactory.create(endpoint, DocumentService.class, Arrays.asList(provider), username, password, null);
		}
		
		if (timeout > 0) {
			HTTPConduit conduit = WebClient.getConfig(proxy).getHttpConduit();
			HTTPClientPolicy policy = new HTTPClientPolicy();
			policy.setReceiveTimeout(timeout);
			conduit.setClient(policy);
		}		
	}

	public WSDocument create(WSDocument document, File packageFile) throws Exception {
		WebClient.client(proxy).type(MediaType.MULTIPART_FORM_DATA);
		WebClient.client(proxy).accept(MediaType.APPLICATION_JSON);

		ObjectWriter ow = new ObjectMapper().writer();
		String jsonStr = ow.writeValueAsString(document);

		Attachment docAttachment = new AttachmentBuilder().id("document").object(jsonStr).mediaType("application/json")
				.contentDisposition(new ContentDisposition("form-data; name=\"document\"")).build();
		Attachment fileAttachment = new Attachment("content", new FileInputStream(packageFile), new ContentDisposition(
				"form-data; name=\"content\"; filename=\"" + packageFile.getName() + "\""));

		List<Attachment> atts = new LinkedList<Attachment>();
		atts.add(docAttachment);
		atts.add(fileAttachment);

		return proxy.create(atts);
	}

	public WSDocument create(WSDocument document, DataHandler dataHandler) throws Exception {

		WebClient.client(proxy).type(MediaType.MULTIPART_FORM_DATA);
		WebClient.client(proxy).accept(MediaType.APPLICATION_JSON);

		ObjectWriter ow = new ObjectMapper().writer();
		String jsonStr = ow.writeValueAsString(document);

		Attachment docAttachment = new AttachmentBuilder().id("document").object(jsonStr).mediaType("application/json")
				.contentDisposition(new ContentDisposition("form-data; name=\"document\"")).build();
		Attachment fileAttachment = new AttachmentBuilder().id("content").dataHandler(dataHandler)
				.mediaType("application/octet-stream")
				.contentDisposition(new ContentDisposition("form-data; name=\"content\"")).build();

		List<Attachment> atts = new LinkedList<Attachment>();
		atts.add(docAttachment);
		atts.add(fileAttachment);

		return proxy.create(atts);
	}

	public WSDocument[] list(long folderId) throws Exception {

		WebClient.client(proxy).type("*/*");

		return proxy.list(folderId);
	}

	public WSDocument[] listDocuments(long folderId, String fileName) throws Exception {
		WebClient.client(proxy).type("*/*");
		WebClient.client(proxy).accept(MediaType.APPLICATION_JSON);

		return proxy.listDocuments(folderId, fileName);
	}

	public WSDocument getDocument(long docId) throws Exception {
		WebClient.client(proxy).accept(MediaType.APPLICATION_JSON);

		return proxy.getDocument(docId);
	}

	public void delete(long docId) throws Exception {

		WebClient.client(proxy).accept(MediaType.APPLICATION_JSON);

		proxy.delete(docId);
	}

	public DataHandler getContent(long docId) throws Exception {

		// WebClient.client(proxy).type("*/*");
		WebClient.client(proxy).accept(MediaType.APPLICATION_OCTET_STREAM);

		return proxy.getContent(docId);
	}

	public void update(WSDocument document) throws Exception {
		
		WebClient.client(proxy).type(MediaType.APPLICATION_JSON);
		WebClient.client(proxy).accept(MediaType.APPLICATION_JSON);

		proxy.update(document);
	}

}
