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

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.ChatCompletionApi;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionChunk;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionMessage;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionMessage.Role;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionRequest;
import reactor.core.publisher.Flux;

/**
 *
 * @author Christian Tzolov
 */
public class ChatCompletionStreamingDemo {
	public static void main(String[] args) throws JsonProcessingException {

		ChatCompletionApi completionApi = new ChatCompletionApi(System.getenv("OPENAI_API_KEY"));

		var streamRequest = new ChatCompletionRequest(
				List.of(new ChatCompletionMessage(
						"What is the capital of Bulgaria?",
						Role.user)),
				"gpt-4-1106-preview",
				true, // stream = true
				0.8f);

		System.out.println(
				"Request: " + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(streamRequest));

		Flux<ChatCompletionChunk> flux = completionApi.chatCompletionStream(streamRequest);

		// flux.subscribe(
		// content -> {
		// content.choices().forEach(choice -> {
		// // System.out.println("role: " + choice.delta().getRole());
		// // System.out.println("content: " + choice.delta().content());

		// });
		// },
		// error -> System.out.println("Error receiving SSE: " + error),
		// () -> System.out.println("Completed!!!"));

		// var b = flux.blockLast();

		List<ChatCompletionChunk> chunks = flux.collectList().block();
		System.out.println("Chunks: " + chunks);

	}

}
