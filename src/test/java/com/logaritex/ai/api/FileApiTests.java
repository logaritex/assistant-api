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
import com.logaritex.ai.api.Data.File;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 *
 * @author Christian Tzolov
 */
@RestClientTest(FileApiTests.Config.class)
public class FileApiTests {

	private final static String TEST_API_KEY = "test-api-key";

	@Autowired
	private FileApi client;

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ObjectMapper objectMapper;

	@AfterEach
	void resetMockServer() {
		server.reset();
	}

	@Test
	public void listFiles() throws JsonProcessingException {

		var file1 = new Data.File("id1", 66, new Date().getTime(), "filename",
				"object", "purpose", "status", "status_details");
		var file2 = new Data.File("id2", 66, new Date().getTime(), "filename",
				"object", "purpose", "status", "status_details");

		DataList<Data.File> expectedList = new DataList<>("", List.of(file1, file2), file1.id(),
				file2.id(), false);

		server.expect(requestToUriTemplate("/v1/files?purpose={purpose}", File.Purpose.ASSISTANTS.getText()))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expectedList), MediaType.APPLICATION_JSON));

		DataList<Data.File> fileDataList = client.listFiles(File.Purpose.ASSISTANTS);

		assertThat(fileDataList).isEqualTo(expectedList);

		server.verify();
	}

	@Test
	public void uploadFile() throws JsonProcessingException {

		var fileResource = new DefaultResourceLoader().getResource("classpath:/MSFT.csv");

		var expectedFile = new Data.File("id", 66, new Date().getTime(), "filename",
				"object", "purpose", "status", "status_details");

		server.expect(requestTo("/v1/files"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.MULTIPART_FORM_DATA_VALUE)))
				.andExpect(content().multipartDataContains(
						Map.of("purpose", File.Purpose.ASSISTANTS.getText(),
								"file", fileResource)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expectedFile), MediaType.APPLICATION_JSON));

		Data.File file = client.uploadFile(fileResource, File.Purpose.ASSISTANTS);

		assertThat(file).isEqualTo(expectedFile);

		server.verify();
	}

	@Test
	public void retrieveFile() throws JsonProcessingException {

		var expectedFile = new Data.File("id1", 66, new Date().getTime(), "filename",
				"object", "purpose", "status", "status_details");

		server.expect(requestToUriTemplate("/v1/files/{file_id}", expectedFile.id()))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andRespond(withSuccess(objectMapper.writeValueAsString(expectedFile), MediaType.APPLICATION_JSON));

		Data.File file = client.retrieveFile(expectedFile.id());

		assertThat(file).isEqualTo(expectedFile);

		server.verify();
	}

	@Test
	public void retrieveFileContent() throws JsonProcessingException {

		var expectedFile = new Data.File("id1", 66, new Date().getTime(), "filename",
				"object", "purpose", "status", "status_details");

		server.expect(requestToUriTemplate("/v1/files/{file_id}/content", expectedFile.id()))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andRespond(withSuccess(objectMapper.writeValueAsBytes(expectedFile), MediaType.APPLICATION_JSON));

		byte[] content = client.retrieveFileContent(expectedFile.id());

		assertThat(content).isNotEmpty();

		server.verify();
	}

	@Test
	public void deleteFile() throws JsonProcessingException {
		String fileId = "63";

		Data.DeletionStatus expectedStatus = new Data.DeletionStatus(TEST_API_KEY, TEST_API_KEY, null);

		server.expect(requestToUriTemplate("/v1/files/{file_id}", fileId))
				.andExpect(method(HttpMethod.DELETE))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andRespond(withSuccess(objectMapper.writeValueAsBytes(expectedStatus), MediaType.APPLICATION_JSON));

		Data.DeletionStatus status = client.deleteFile(fileId);

		assertThat(status).isEqualTo(expectedStatus);

		server.verify();
	}

	@SpringBootConfiguration
	static class Config {

		@Bean
		public FileApi fileApi(RestClient.Builder builder) {
			return new FileApi("", TEST_API_KEY, builder);
		}

	}
}
