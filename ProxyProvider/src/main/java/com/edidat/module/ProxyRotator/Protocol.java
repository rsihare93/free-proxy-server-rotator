package com.edidat.module.ProxyRotator;

public enum Protocol {

	HTTP("HTTP"), HTTPS("HTTPS");

	String name;

	private Protocol(String name) {
		this.name = name;
	}

}
