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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.Data;
import com.logaritex.ai.api.Data.Message;
import com.logaritex.ai.api.Data.Run;
import com.logaritex.ai.api.Data.Run.RequiredAction.ToolCall;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.CollectionUtils;

/**
 * Uses the OpenAI Assistant API with calling custom functions:
 * https://platform.openai.com/docs/assistants/tools/function-calling
 *
 * Prerequisites:
 *
 * - (free) Register with OpenWeather (https://openweathermap.org/) to get an api-key and set it to the
 * OPEN_WEATHER_MAP_API_KEY env. variable.
 *
 * - (paid) Register with OpenAI API (https://openai.com/) and get an api-key and set it to OPENAI_API_KEY env.
 * variable.
 *
 * @author Christian Tzolov
 */
public class OpenAiFunctionTool {

	private static final Log logger = LogFactory.getLog(OpenAiFunctionTool.class);

	public static void main(String[] args) throws JsonMappingException, JsonProcessingException, InterruptedException {

		var weatherService = new WeatherFunction(System.getenv("OPEN_WEATHER_MAP_API_KEY"));

		var cityNameService = new CityNameFunction(System.getenv("OPEN_WEATHER_MAP_API_KEY"), 10);

		var assistantApi = new AssistantApi(System.getenv("OPENAI_API_KEY"));

		// 1. Create an Assistant with two function definitions.
		Data.Assistant assistant = assistantApi.createAssistant(new Data.AssistantRequestBody(
				"gpt-4-1106-preview",
				"Weather and City names Assistant ", "",
				"You are a weather bot. Use the provided functions to answer questions.",

				// 1.1 Define the description and the input formats for the WeatherFunction and the CityNameFunction
				// functions. Uses JSON Schema to define the functions arguments.
				List.of(new Data.FunctionTool(new Data.FunctionTool.Function(
						"Get the weather in location",
						"getCurrentWeather", """
								{
									"type": "object",
									"properties": {
										"location": {
											"type": "string",
											"description": "The city and state e.g. San Francisco, CA"
										},
										"lat": {
											"type": "number",
											"description": "The city latitude"
										},
										"lon": {
											"type": "number",
											"description": "The city longitude"
										},
										"unit": {
											"type": "string",
											"enum": ["c", "f"]
										}
									},
									"required": ["location", "lat", "lon", "unit"]
								}
								""")),
						new Data.FunctionTool(new Data.FunctionTool.Function(
								"Get the local names of a city",
								"getCityLocalNames", """
										{
											"type": "object",
											"properties": {
												"city": {
													"type": "string",
													"description": "The city e.g. San Francisco"
												},
												"country": {
													"type": "string",
													"description": "The country code e.g. NL"
												}
											},
											"required": ["city", "country"]
										}
										"""))),
				List.of(), // no files
				Map.of())); // no metadata
		logger.info("1. Create an Assistant with two function definitions. Assistant Id: " + assistant.id());

		// 2. Create an empty Thread (represents a session between your user and your application).
		Data.Thread thread = assistantApi.createThread(new Data.ThreadRequest());
		logger.info(" 2. Create an empty Thread: " + thread.id());

		// 3. Add a new user Message to the Thread.
		var userMessage = assistantApi.createMessage(
				new Data.MessageRequest(Data.Role.user,
						"What is the weather in Amsterdam, Netherlands and the known local names for this place?"),
				thread.id());
		logger.info("3. Add a new user Message to the Thread: " + userMessage);

		// 4. Crate a new Run - represents an execution run on a Thread with an Assistant.
		Data.Run run = assistantApi.createRun(
				thread.id(), // run this Thread,
				new Data.RunRequest(assistant.id())); // with this Assistant.
		logger.info(" 4. Crate a new Run - represents an execution run on a Thread with an Assistant. Run Id = "
				+ run.id());

		logger.info(
				"4.1 As Run is asynchronous, wait until it completes and handle the internal transition states such as requires_action");
		// 4.1 As Run is asynchronous, wait until it completes and handle
		// the internal transition states such as requires_action
		while (run.status() != Run.Status.completed || run.status() == Run.Status.cancelled) {

			java.lang.Thread.sleep(1000);

			run = assistantApi.retrieveRun(thread.id(), run.id());

			if (run.status() == Run.Status.requires_action) {

				List<ToolCall> toolCalls = run.required_action().submit_tool_outputs().tool_calls();

				List<Data.ToolOutputs.ToolOutput> toolOutputs = new ArrayList<>();

				for (ToolCall toolCall : toolCalls) {

					if (toolCall.function().name().equals("getCurrentWeather")) {

						// Function arguments are expected the defined getCurrentWeather, JSON schema format like:
						// {"location": "Amsterdam, Netherlands", "lat": 52.3676, "lon": 4.9041, "unit": "c"}
						WeatherFunction.Request weatherRequest = fromJson(toolCall.function().arguments(),
								WeatherFunction.Request.class);

						logger.info(
								"4.2 As 4.2.1 Delegate to the weatherService to retrieve the current temperature. The weatherRequest ="
										+ weatherRequest);
						// 4.2.1 Delegate to the weatherService to retrieve the current temperature.
						var weatherResponse = weatherService.apply(weatherRequest);

						// 4.2.2 and the weatherService output to the tool outputs.
						toolOutputs.add(new Data.ToolOutputs.ToolOutput(toolCall.id(),
								"" + weatherResponse.temp() + weatherRequest.unit()));
					}
					else if (toolCall.function().name().equals("getCityLocalNames")) {

						// Function arguments are expected the defined getCityLocalNames, JSON schema format like:
						// {"city": "Amsterdam", "country": "NL"}
						CityNameFunction.Request request = fromJson(toolCall.function().arguments(),
								CityNameFunction.Request.class);

						logger.info(" 4.3.1 Delegate to the cityNameService to retrieve the local city names. Request = " + request);
						// 4.3.1 Delegate to the cityNameService to retrieve the local city names.
						String cityNames = cityNameService.apply(request);

						// 4.3.2 add the cityNameService output to the tool outputs.
						toolOutputs.add(new Data.ToolOutputs.ToolOutput(toolCall.id(), cityNames));
					}
				}

				logger.info("4.4 send the tool outputs to the waiting Run process. Size: " + toolOutputs.size());
				// 4.4 send the tool outputs to the waiting Run process.
				if (!CollectionUtils.isEmpty(toolOutputs)) {
					assistantApi.submitToolOutputsToRun(thread.id(), run.id(), new Data.ToolOutputs(toolOutputs));
				}
			}
		}

		logger.info("5. Retrieve Thread's messages. Result contains all 'assistant' and 'user' messages.");
		// 5. Retrieve Thread's messages. Result contains all 'assistant' and 'user' messages.
		Data.DataList<Data.Message> messages = assistantApi.listMessages(
				new Data.ListRequest(),
				thread.id());

		// System.out.println("Message count: " + messages.data().size());
		// System.out.println("Thread messages: " + messages.data());

		logger.info(" 6. extract only the assistant messages.");
		// 6. extract only the assistant messages.
		List<Message> assistantMessages = messages.data().stream().filter(m -> m.role() == Data.Role.assistant)
				.toList();

		System.out.println(assistantMessages);

		logger.info(" 7. Delete the demo resources.");
		// 7. Delete the demo resources.
		// Comment out the deletion if you want to reuse the Assistant in
		// https://platform.openai.com/assistants
		// assistantApi.deleteThread(thread.id());
		// assistantApi.deleteAssistant(assistant.id());

	}

	private static <T> T fromJson(String json, Class<T> targetClass) {
		try {
			return new ObjectMapper().readValue(json, targetClass);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
