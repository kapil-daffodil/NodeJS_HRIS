package com.applane.afb;

import java.util.ArrayList;


public class DynamicFormTag {

	private String id;
	private String target;
	private String action;
	private String method;
	private String heading;
	private String headStyle;
	private String formStyle;
	private ArrayList<DynamicFormFields> fields;

	public DynamicFormTag(String id, String target, String action, String method, ArrayList<DynamicFormFields> fields, String heading, String headStyle, String formStyle) {
		this.setId(id);
		this.setTarget(target);
		this.setAction(action);
		this.setMethod(method);
		this.setFields(fields);
		this.setHeading(heading);
		this.setHeadStyle(headStyle);
		this.setFormStyle(formStyle);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public ArrayList<DynamicFormFields> getFields() {
		return fields;
	}

	public void setFields(ArrayList<DynamicFormFields> fields) {
		this.fields = fields;
	}

	public String getHeading() {
		return heading;
	}

	public void setHeading(String heading) {
		this.heading = heading;
	}

	public String getHeadStyle() {
		return headStyle;
	}

	public void setHeadStyle(String headstyle) {
		this.headStyle = headstyle;
	}

	public String getFormStyle() {
		return formStyle;
	}

	public void setFormStyle(String formStyle) {
		this.formStyle = formStyle;
	}

}
