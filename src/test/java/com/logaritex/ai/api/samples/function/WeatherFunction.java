/*
 * Copyright 2023-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.logaritex.ai.api.samples.function;

import java.util.Map;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/**
 * Use the Current Weather (https://openweathermap.org/current) service to retrieve local weather for a location.
 *
 * The Current Weather service is free but you need to register to get an apiKey.
 *
 * @author Christian Tzolov
 */

public class WeatherFunction implements Function<WeatherFunction.Request, WeatherFunction.Response> {

	private static final String baseUrl = "https://api.openweathermap.org";

	private final String apiKey;

	/**
	 * Crate new WeatherFunction.
	 *
	 * @param apiKey your openweathermap api key.
	 */
	public WeatherFunction(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Weather Function request.
	 */
	public record Request(String location, double lat, double lon, Unit unit) {
	}

	/**
	 * Temperature units.
	 */
	public enum Unit {
		/**
		 * Celsius.
		 */
		c("metric"),
		/**
		 * Farenthide.
		 */
		f("imperial");

		/**
		 * Human readable unit name.
		 */
		public final String unitName;

		private Unit(String text) {
			this.unitName = text;
		}
	}

	/**
	 * Weather Function response.
	 */
	public record Response(double temp, double feels_like, double temp_min, double temp_max, int pressure,
			int humidity) {
	}

	@Override
	public Response apply(Request request) {

		Map<String, Object> result = RestClient.create().get()
				.uri(baseUrl + "/data/2.5/weather?units={units}&lat={lat}&lon={lon}&appid={apiKye}",
						request.unit().unitName, request.lat(), request.lon(), this.apiKey)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

		Map<String, Number> main = (Map<String, Number>) result.get("main");

		return new Response(main.get("temp").doubleValue(),
				main.get("feels_like").doubleValue(),
				main.get("temp_min").doubleValue(),
				main.get("temp_max").doubleValue(),
				main.get("pressure").intValue(),
				main.get("humidity").intValue());
	}
}
