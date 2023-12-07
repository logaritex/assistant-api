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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.ChatCompletionApi;
import com.logaritex.ai.api.ChatCompletionApi.EmbeddingRequest;

/**
 *
 * @author Christian Tzolov
 */
public class EmbeddingDemo {

	public static void main(String[] args) throws JsonProcessingException {
		ChatCompletionApi completionApi = new ChatCompletionApi(System.getenv("OPENAI_API_KEY"));

		var request = new EmbeddingRequest<String>("Hello World!");
		// var request = new EmbeddingRequest<List<Integer>>(
		// 		List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

		System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request));

		var response = completionApi.embeddings(request);

		System.out.println(response);
	}

}
