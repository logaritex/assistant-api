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

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.Data.DataList;
import com.logaritex.ai.api.Data.ListRequest;
import org.hamcrest.CoreMatchers;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 *
 * @author Christian Tzolov
 */
@RestClientTest(AssistantApiTests.Config.class)
public class AssistantApiTests {

	private final static String TEST_API_KEY = "test-api-key";

	@Autowired
	private AssistantApi client;

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ObjectMapper objectMapper;

	@AfterEach
	void resetMockServer() {
		server.reset();
	}

	@Test
	public void createAssistant() throws JsonProcessingException {

		Data.Assistant expectedAssistant = new Data.Assistant("id1", "object", new Date().getTime(), "name",
				"description", "model",
				"instructions", List.of(), List.of(), Map.of());

		Data.AssistantRequestBody requestBody = new Data.AssistantRequestBody("model", "instructions");

		server.expect(requestTo("/v1/assistants"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(AssistantApi.OPEN_AI_BETA, AssistantApi.ASSISTANTS_V1))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andExpect(content().json(objectMapper.writeValueAsString(requestBody)))
				.andRespond(
						withSuccess(objectMapper.writeValueAsString(expectedAssistant), MediaType.APPLICATION_JSON));

		Data.Assistant assistant = client.createAssistant(requestBody);

		assertThat(assistant).isEqualTo(expectedAssistant);

		server.verify();
	}

	@Test
	public void retrieveAssistant() throws JsonProcessingException {

		Data.Assistant expectedAssistant = new Data.Assistant("id1", "object", new Date().getTime(), "name",
				"description", "model",
				"instructions", List.of(), List.of(), Map.of());

		server.expect(requestToUriTemplate("/v1/assistants/{assistant_id}", expectedAssistant.id()))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(AssistantApi.OPEN_AI_BETA, AssistantApi.ASSISTANTS_V1))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andRespond(
						withSuccess(objectMapper.writeValueAsString(expectedAssistant), MediaType.APPLICATION_JSON));

		Data.Assistant assistant = client.retrieveAssistant(expectedAssistant.id());

		assertThat(assistant).isEqualTo(expectedAssistant);

		server.verify();
	}

	@Test
	public void modifyAssistant() throws JsonProcessingException {

		Data.Assistant modifiedAssistant = new Data.Assistant("id1", "object", new Date().getTime(), "name",
				"description", "model",
				"instructions", List.of(), List.of(), Map.of());

		Data.AssistantRequestBody requestBody = new Data.AssistantRequestBody("model", "instructions");

		server.expect(requestToUriTemplate("/v1/assistants/{assistant_id}", modifiedAssistant.id()))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(AssistantApi.OPEN_AI_BETA, AssistantApi.ASSISTANTS_V1))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andExpect(content().json(objectMapper.writeValueAsString(requestBody)))
				.andRespond(
						withSuccess(objectMapper.writeValueAsString(modifiedAssistant), MediaType.APPLICATION_JSON));

		Data.Assistant assistant = client.modifyAssistant(requestBody, modifiedAssistant.id());

		assertThat(assistant).isEqualTo(modifiedAssistant);

		server.verify();
	}

	@Test
	public void listAssistants() throws JsonProcessingException {

		var assistant1 = new Data.Assistant("id1", "object", new Date().getTime(), "name", "description", "model",
				"instructions", List.of(), List.of(), Map.of());
		var assistant2 = new Data.Assistant("id2", "object", new Date().getTime(), "name", "description", "model",
				"instructions", List.of(), List.of(), Map.of());

		DataList<Data.Assistant> expectedList = new DataList<>("", List.of(assistant1, assistant2), assistant1.id(),
				assistant2.id(), false);

		ListRequest request = new ListRequest();

		server.expect(requestToUriTemplate("/v1/assistants?order={order}&limit={limit}&before={before}&after={after}",
				request.order(), request.limit(), request.before(), request.after()))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(AssistantApi.OPEN_AI_BETA, AssistantApi.ASSISTANTS_V1))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expectedList), MediaType.APPLICATION_JSON));

		DataList<Data.Assistant> assistantDataList = client.listAssistants(request);

		assertThat(assistantDataList).isEqualTo(expectedList);

		server.verify();
	}

	@Test
	public void deleteAssistant() throws JsonProcessingException {
		String assistantId = "63";

		Data.DeletionStatus expectedStatus = new Data.DeletionStatus(TEST_API_KEY, TEST_API_KEY, null);

		server.expect(requestToUriTemplate("/v1/assistants/{assistant_id}", assistantId))
				.andExpect(method(HttpMethod.DELETE))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andRespond(withSuccess(objectMapper.writeValueAsBytes(expectedStatus), MediaType.APPLICATION_JSON));

		Data.DeletionStatus status = client.deleteAssistant(assistantId);

		assertThat(status).isEqualTo(expectedStatus);

		server.verify();
	}

	@Test
	public void createAssistantFile() throws JsonProcessingException {

		String assistantId = "63";

		var file = new Data.File("id1", 66, new Date().getTime(), "filename",
				"object", "purpose", "status", "status_details");

		server.expect(requestToUriTemplate("/v1/assistants/{assistant_id}/files", assistantId))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(AssistantApi.OPEN_AI_BETA, AssistantApi.ASSISTANTS_V1))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andExpect(content().json(objectMapper.writeValueAsString(Map.of("file_id", file.id()))))
				.andRespond(
						withSuccess(objectMapper.writeValueAsString(file), MediaType.APPLICATION_JSON));

		Data.File assistantFile = client.createAssistantFile(assistantId, file.id());

		assertThat(assistantFile).isEqualTo(file);

		server.verify();
	}

	@Test
	public void retrieveAssistantFile() throws JsonProcessingException {

		Data.Assistant assistant = new Data.Assistant("id1", "object", new Date().getTime(), "name",
				"description", "model",
				"instructions", List.of(), List.of(), Map.of());

		var file = new Data.File("id1", 66, new Date().getTime(), "filename",
				"object", "purpose", "status", "status_details");

		server.expect(requestToUriTemplate("/v1/assistants/{assistant_id}/files/{file_id}", assistant.id(), file.id()))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(AssistantApi.OPEN_AI_BETA, AssistantApi.ASSISTANTS_V1))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andRespond(
						withSuccess(objectMapper.writeValueAsString(file), MediaType.APPLICATION_JSON));

		Data.File retrievedFile = client.retrieveAssistantFile(assistant.id(), file.id());

		assertThat(retrievedFile).isEqualTo(file);

		server.verify();
	}

	@Test
	public void deleteAssistantFile() throws JsonProcessingException {
		String assistantId = "63";
		String fileId = "99";

		Data.DeletionStatus expectedStatus = new Data.DeletionStatus(TEST_API_KEY, TEST_API_KEY, true);

		server.expect(requestToUriTemplate("/v1/assistants/{assistant_id}/files/{file_id}", assistantId, fileId))
				.andExpect(method(HttpMethod.DELETE))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andRespond(withSuccess(objectMapper.writeValueAsBytes(expectedStatus), MediaType.APPLICATION_JSON));

		Data.DeletionStatus status = client.deleteAssistantFile(assistantId, fileId);

		assertThat(status).isEqualTo(expectedStatus);

		server.verify();
	}

	@Test
	public void listAssistantsFiles() throws JsonProcessingException {

		String assistantId = "63";

		var file1 = new Data.File("id1", 66, new Date().getTime(), "filename",
				"object", "purpose", "status", "status_details");
		var file2 = new Data.File("id2", 66, new Date().getTime(), "filename",
				"object", "purpose", "status", "status_details");

		DataList<Data.File> expectedList = new DataList<>("", List.of(file1, file2), file1.id(),
				file2.id(), false);

		server.expect(requestToUriTemplate("/v1/assistants/{assistant_id}/files", assistantId))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(AssistantApi.OPEN_AI_BETA, AssistantApi.ASSISTANTS_V1))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expectedList), MediaType.APPLICATION_JSON));

		DataList<Data.File> assistantFileList = client.listAssistantFiles(assistantId);

		assertThat(assistantFileList).isEqualTo(expectedList);

		server.verify();
	}

	@SpringBootConfiguration
	static class Config {

		@Bean
		public AssistantApi assistantApi(RestClient.Builder builder) {
			return new AssistantApi("", TEST_API_KEY, builder);
		}

	}
}
