package com.civilwar.riot;

import java.net.URI;
import java.util.List;

import com.civilwar.config.CivilwarProperties;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/riot-api")
public class RiotProxyController {

	private static final String RIOT_TOKEN_HEADER = "X-Riot-Token";
	private static final List<String> FORWARD_HEADERS = List.of(
			HttpHeaders.ACCEPT,
			HttpHeaders.CONTENT_TYPE,
			RIOT_TOKEN_HEADER);

	private final RestClient restClient;
	private final CivilwarProperties properties;

	public RiotProxyController(CivilwarProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.builder().build();
	}

	@RequestMapping("/**")
	public ResponseEntity<byte[]> proxy(
			HttpServletRequest request,
			@RequestHeader(value = RIOT_TOKEN_HEADER, required = false) String riotToken,
			@RequestBody(required = false) byte[] body) {
		String targetPath = request.getRequestURI().substring("/riot-api".length());
		String query = request.getQueryString();
		String targetUrl = properties.riot().baseUrl() + targetPath
				+ (query == null || query.isBlank() ? "" : "?" + query);

		HttpMethod method = HttpMethod.valueOf(request.getMethod());

		try {
			RestClient.RequestBodySpec spec = restClient.method(method)
					.uri(URI.create(targetUrl))
					.headers(headers -> copyForwardHeaders(request, headers));

			ResponseEntity<byte[]> response = (body != null && body.length > 0)
					? spec.body(body).retrieve().toEntity(byte[].class)
					: spec.retrieve().toEntity(byte[].class);

			return ResponseEntity.status(response.getStatusCode())
					.headers(filterResponseHeaders(response.getHeaders()))
					.body(response.getBody());
		} catch (RestClientResponseException ex) {
			return ResponseEntity.status(ex.getStatusCode())
					.headers(filterResponseHeaders(ex.getResponseHeaders()))
					.contentType(MediaType.APPLICATION_JSON)
					.body(ex.getResponseBodyAsByteArray());
		}
	}

	private void copyForwardHeaders(HttpServletRequest request, HttpHeaders headers) {
		for (String headerName : FORWARD_HEADERS) {
			String value = request.getHeader(headerName);
			if (value != null && !value.isBlank()) {
				headers.set(headerName, value);
			}
		}
	}

	private HttpHeaders filterResponseHeaders(HttpHeaders source) {
		HttpHeaders headers = new HttpHeaders();
		source.forEach((name, values) -> {
			if (HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(name)
					|| HttpHeaders.CONNECTION.equalsIgnoreCase(name)) {
				return;
			}
			headers.put(name, values);
		});
		return headers;
	}
}
