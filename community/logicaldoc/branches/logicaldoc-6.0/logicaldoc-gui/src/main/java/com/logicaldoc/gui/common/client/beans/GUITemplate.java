package com.logicaldoc.gui.common.client.beans;

import java.io.Serializable;

public class GUITemplate implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id = 0;

	private String name;

	private String description;

	private boolean readonly=false;
	
	private GUIExtendedAttribute[] attributes;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public GUIExtendedAttribute[] getAttributes() {
		return attributes;
	}

	public void setAttributes(GUIExtendedAttribute[] attributes) {
		this.attributes = attributes;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}
}
