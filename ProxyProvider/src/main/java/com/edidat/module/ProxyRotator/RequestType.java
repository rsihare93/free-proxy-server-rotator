package com.edidat.module.ProxyRotator;

public enum RequestType {

	GET("GET"), EXIT("EXIT"), HEARTBEAT("HEARTBEAT");
	String name;

	private RequestType(String name) {
		this.name = name;
	}

}
