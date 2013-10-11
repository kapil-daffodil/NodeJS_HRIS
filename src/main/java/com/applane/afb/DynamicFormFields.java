package com.applane.afb;

public class DynamicFormFields {

	private String label;
	private String type;
	private Object value;
	private String name;
	private String style;

	public DynamicFormFields(String label, String type, Object value, String name, String style) {
		this.setLabel(label);
		this.setType(type);
		this.setValue(value);
		this.setName(name);
		this.setStyle(style);
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

}
