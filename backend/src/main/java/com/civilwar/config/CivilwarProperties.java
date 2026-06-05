package com.civilwar.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "civilwar")
public record CivilwarProperties(
		Cors cors,
		Riot riot) {

	public record Cors(List<String> allowedOrigins) {
	}

	public record Riot(String baseUrl, String krBaseUrl) {
	}
}
