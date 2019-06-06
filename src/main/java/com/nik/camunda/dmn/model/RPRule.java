package com.nik.camunda.dmn.model;

import java.util.ArrayList;
import java.util.List;

public class RPRule {

	private List<RolePermission> rules = new ArrayList<>();

	public List<RolePermission> getRules() {
		return rules;
	}

	public void setRules(List<RolePermission> rules) {
		this.rules = rules;
	}

	@Override
	public String toString() {
		return "RPRule [rules=" + rules + "]";
	}
	
	
		
}
