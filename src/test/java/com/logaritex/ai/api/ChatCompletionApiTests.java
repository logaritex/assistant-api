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

package com.logaritex.ai.api;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionMessage;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionMessage.Role;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletionRequest;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletion;
import com.logaritex.ai.api.ChatCompletionApi.ChatCompletion.Usage;
import com.logaritex.ai.api.ChatCompletionApi.DataList;
import com.logaritex.ai.api.ChatCompletionApi.Embedding;
import com.logaritex.ai.api.ChatCompletionApi.EmbeddingRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 *
 * @author Christian Tzolov
 */
@RestClientTest(ChatCompletionApiTests.Config.class)
public class ChatCompletionApiTests {

	private final static String TEST_API_KEY = "test-api-key";

	@Autowired
	private ChatCompletionApi client;

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ObjectMapper objectMapper;

	@AfterEach
	void resetMockServer() {
		server.reset();
	}

	@Test
	public void chatCompletion() throws JsonProcessingException {

		var request = new ChatCompletionRequest(
				List.of(new ChatCompletionMessage("What is the capital of Bulgaria?", ChatCompletionMessage.Role.user)),
				"gpt-4-1106-preview", 0.8f);

		var choice = new ChatCompletion.Choice(null, 0,
				new ChatCompletionMessage("This is a response", Role.assistant));

		var expectedCompletion = new ChatCompletion("id", List.of(choice), null, "model", "sf", "object",
				new Usage(6, 6, 6));

		// var dataList = new DataList<>("", List.of(expectedEmbedding), null, null, false);
		server.expect(requestTo("/v1/chat/completions"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(content().json(objectMapper.writeValueAsString(request)))
				.andRespond(
						withSuccess(objectMapper.writeValueAsString(expectedCompletion), MediaType.APPLICATION_JSON));

		var completion = client.chatCompletion(request);

		assertThat(completion.choices()).hasSize(1);
		assertThat(completion.choices()).isEqualTo(List.of(choice));

		server.verify();
	}

	@Test
	public void embeddingSingleText() throws JsonProcessingException {

		var request = new EmbeddingRequest<String>("Hello World!");

		var expectedEmbedding = new Embedding(0, List.of(1.0f, 2.0f, 3.0f));

		var dataList = new DataList<>("", List.of(expectedEmbedding), null, null, false);
		server.expect(requestTo("/v1/embeddings"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(content().json(objectMapper.writeValueAsString(request)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(dataList), MediaType.APPLICATION_JSON));

		var embedding = client.embeddings(request);

		assertThat(embedding.data()).hasSize(1);
		assertThat(embedding.data().get(0).embedding()).isEqualTo(List.of(1.0f, 2.0f, 3.0f));

		server.verify();
	}

	@Test
	public void embeddingTextList() throws JsonProcessingException {

		var request = new EmbeddingRequest<List<String>>(
				List.of("Hello World!", "How are you?", "I am fine, thank you!"));

		var expectedEmbedding1 = new Embedding(0, List.of(1.0f, 2.0f, 3.0f));
		var expectedEmbedding2 = new Embedding(0, List.of(1.0f, 2.0f, 3.0f));
		var expectedEmbedding3 = new Embedding(0, List.of(1.0f, 2.0f, 3.0f));

		var dataList = new DataList<>("", List.of(expectedEmbedding1, expectedEmbedding2, expectedEmbedding3), null,
				null, false);

		server.expect(requestTo("/v1/embeddings"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(content().json(objectMapper.writeValueAsString(request)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(dataList), MediaType.APPLICATION_JSON));

		var embedding = client.embeddings(request);

		assertThat(embedding.data()).hasSize(3);
		assertThat(embedding.data().get(0).embedding()).isEqualTo(List.of(1.0f, 2.0f, 3.0f));
		assertThat(embedding.data().get(1).embedding()).isEqualTo(List.of(1.0f, 2.0f, 3.0f));
		assertThat(embedding.data().get(2).embedding()).isEqualTo(List.of(1.0f, 2.0f, 3.0f));

		server.verify();
	}

	@Test
	public void embeddingSingleToken() throws JsonProcessingException {

		var request = new EmbeddingRequest<List<Integer>>(List.of(1, 2, 3));

		var expectedEmbedding = new Embedding(0, List.of(1.0f, 2.0f, 3.0f));

		var dataList = new DataList<>("", List.of(expectedEmbedding), null, null, false);
		server.expect(requestTo("/v1/embeddings"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(content().json(objectMapper.writeValueAsString(request)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(dataList), MediaType.APPLICATION_JSON));

		var embedding = client.embeddings(request);

		assertThat(embedding.data()).hasSize(1);
		assertThat(embedding.data().get(0).embedding()).isEqualTo(List.of(1.0f, 2.0f, 3.0f));

		server.verify();
	}

	@Test
	public void embeddingTokenList() throws JsonProcessingException {

		var request = new EmbeddingRequest<List<List<Integer>>>(
				List.of(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7, 8, 9)));

		var expectedEmbedding1 = new Embedding(0, List.of(1.0f, 2.0f, 3.0f));
		var expectedEmbedding2 = new Embedding(0, List.of(1.0f, 2.0f, 3.0f));
		var expectedEmbedding3 = new Embedding(0, List.of(1.0f, 2.0f, 3.0f));

		var dataList = new DataList<>("", List.of(expectedEmbedding1, expectedEmbedding2, expectedEmbedding3), null,
				null, false);

		server.expect(requestTo("/v1/embeddings"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(content().json(objectMapper.writeValueAsString(request)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(dataList), MediaType.APPLICATION_JSON));

		var embedding = client.embeddings(request);

		assertThat(embedding.data()).hasSize(3);
		assertThat(embedding.data().get(0).embedding()).isEqualTo(List.of(1.0f, 2.0f, 3.0f));
		assertThat(embedding.data().get(1).embedding()).isEqualTo(List.of(1.0f, 2.0f, 3.0f));
		assertThat(embedding.data().get(2).embedding()).isEqualTo(List.of(1.0f, 2.0f, 3.0f));

		server.verify();
	}

	@SpringBootConfiguration
	static class Config {

		@Bean
		public ChatCompletionApi chatCompletionApi(RestClient.Builder builder) {
			return new ChatCompletionApi("", TEST_API_KEY, builder);
		}

	}

}
