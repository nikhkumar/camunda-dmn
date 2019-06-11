package com.nik.camunda.dmn.model;

public class DMNResponse {

	private String status;
	private int code;
	private String message;
	private RPResponse result;
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int resCode) {
		this.code = resCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public RPResponse getResult() {
		return result;
	}
	public void setResult(RPResponse result) {
		this.result = result;
	}
	
	
	
}
