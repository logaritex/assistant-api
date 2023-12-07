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

package com.logaritex.ai.api.samples.chat;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.ChatCompletionApi;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletion;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionMessage;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionRequest;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionMessage.Role;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionMessage.ToolCall;
import com.logaritex.ai.api.Data;
import com.logaritex.ai.api.samples.function.WeatherFunction;
import com.logaritex.ai.api.samples.function.WeatherFunction.Response;

/**
 * Based on the OpenAI Function Calling tutorial:
 * https://platform.openai.com/docs/guides/function-calling/parallel-function-calling
 *
 * @author Christian Tzolov
 */
public class ChatCompletionFunctionToolDemo {

	public static void main(String[] args) {

		var weatherService = new WeatherFunction(System.getenv("OPEN_WEATHER_MAP_API_KEY"));

		ChatCompletionApi completionApi = new ChatCompletionApi(System.getenv("OPENAI_API_KEY"));

		// Step 1: send the conversation and available functions to the model
		var message = new ChatCompletionMessage(
				"What's the weather like in San Francisco, Tokyo, and Paris?",
				Role.user);

		var functionTool = new Data.FunctionTool(new Data.FunctionTool.Function(
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
						"""));

		List<ChatCompletionMessage> messages = new ArrayList<>(List.of(message));

		var chatCompletionRequest = new ChatCompletionRequest(
				messages,
				"gpt-4-1106-preview",
				List.of(functionTool),
				null); // null == auto

		ChatCompletion chatCompletion = completionApi.chatCompletion(chatCompletionRequest);

		ChatCompletionMessage responseMessage = chatCompletion.choices().get(0).message();

		// Step 2: check if the model wanted to call a function
		if (responseMessage.toolCalls() != null) {
			// Step 3: call the function
			// Note: the JSON response may not always be valid; be sure to handle errors

			// extend conversation with assistant's reply.
			// messages.add(new ChatMessage(responseMessage.content(), responseMessage.role(), null,
			// null, // ???
			// responseMessage.tool_calls()));
			messages.add(responseMessage);

			// Step 4: send the info for each function call and function response to the model.
			for (ToolCall toolCall : responseMessage.toolCalls()) {
				var functionName = toolCall.function().name();
				if ("getCurrentWeather".equals(functionName)) {
					WeatherFunction.Request weatherRequest = fromJson(toolCall.function().arguments(),
							WeatherFunction.Request.class);

					Response weatherResponse = weatherService.apply(weatherRequest);

					// extend conversation with function response.
					messages.add(
							new ChatCompletionMessage("" + weatherResponse.temp() + weatherRequest.unit(),
									Role.tool,
									null, toolCall.id(), null, null));
				}
			}

			ChatCompletion chatCompletion2 = completionApi.chatCompletion(new ChatCompletionRequest(
					messages,
					"gpt-4-1106-preview"));

			System.out.println(chatCompletion2);
		}

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
