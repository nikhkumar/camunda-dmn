package com.nik.camunda.dmn.model;

import java.util.ArrayList;
import java.util.List;

public class RolePermission {

	private String role;
	private List<String> permission = new ArrayList<String>();
	
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public List<String> getPermission() {
		
		return permission;
	}
	public void setPermission(List<String> permission) {
		this.permission = permission;
	}
	@Override
	public String toString() {
		return "RolePermission [role=" + role + ", permission=" + permission + "]";
	}
	
}
