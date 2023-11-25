package com.logaritex.ai.api.samples.function;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/**
 * Use the https://openweathermap.org/api/geocoding-api to retrieve local names for a location.
 *
 * The geocoding service is free but you need to register to get an apiKey.
 *
 * @author Christian Tzolov
 */
public class CityNameFunction implements Function<CityNameFunction.Request, String> {

	private static final String baseUrl = "https://api.openweathermap.org";

	private final String apiKey;

	private final int responseLimit;

	/**
	 * Create CityNameFunction.
	 *
	 * @param apiKey openweathermap api key.
	 * @param limit Number of responses.
	 */
	public CityNameFunction(String apiKey, int limit) {
		this.apiKey = apiKey;
		this.responseLimit = limit;
	}

	/**
	 * Get city local names request.
	 */
	public record Request(String city, String country) {

	}

	/**
	 * Get city local names response.
	 */
	public record Response(String name, Map<String, String> local_names, double lat, double lon, String country,
			String state) {
	}

	@Override
	public String apply(CityNameFunction.Request request) {

		List<CityNameFunction.Response> result = RestClient.create().get()
				.uri(baseUrl + "/geo/1.0/direct?q={city},{country}&limit={limit}&appid={apiKey}",
						request.city(), request.country(), this.responseLimit, this.apiKey)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

		return result.stream().filter(r -> r.local_names != null).findFirst().get()
				.local_names().entrySet()
				.stream()
				.map(e -> e.getValue() + " (" + e.getKey() + ")")
				.collect(Collectors.joining(","));
	}

}