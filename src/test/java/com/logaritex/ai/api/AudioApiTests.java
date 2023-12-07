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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.AudioApi.SpeechRequest;
import com.logaritex.ai.api.AudioApi.SpeechRequest.Voice;
import com.logaritex.ai.api.AudioApi.TranscriptionRequest;
import com.logaritex.ai.api.AudioApi.TranscriptionResponseFormat;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
@RestClientTest(AudioApiTests.Config.class)
public class AudioApiTests {

	private final static String TEST_API_KEY = "test-api-key";

	@Autowired
	private AudioApi client;

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ObjectMapper objectMapper;

	@AfterEach
	void resetMockServer() {
		server.reset();
	}

	@Test
	public void createSpeech() throws JsonProcessingException {

		var requestBody = new SpeechRequest("model", "hello world", Voice.alloy);

		server.expect(requestTo("/v1/audio/speech"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(content().json(objectMapper.writeValueAsString(requestBody)))
				.andRespond(withSuccess(new byte[] { 1, 2, 3 }, MediaType.APPLICATION_OCTET_STREAM));

		var mp3 = client.createSpeech(requestBody);

		assertThat(mp3).isEqualTo(new byte[] { 1, 2, 3 });

		server.verify();
	}

	@Test
	public void testCreateTranscription() {

		var requestBody = new TranscriptionRequest(new byte[] { 1, 2, 3 }, "en",
				TranscriptionResponseFormat.text);

		MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
		multipartBody.add("file", new ByteArrayResource(requestBody.file()) {
			@Override
			public String getFilename() {
				return "audio.webm";
			}
		});
		multipartBody.add("model", requestBody.model());
		multipartBody.add("language", requestBody.language());
		// multipartBody.add("prompt", requestBody.prompt());
		multipartBody.add("response_format", requestBody.response_format().name());
		multipartBody.add("temperature", "" + requestBody.temperature());

		server.expect(requestTo("/v1/audio/transcriptions"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
				.andExpect(header(HttpHeaders.CONTENT_TYPE,
						CoreMatchers.containsString(MediaType.MULTIPART_FORM_DATA_VALUE)))
				.andExpect(content().multipartData(multipartBody))
				.andRespond(withSuccess("Hello World!", MediaType.TEXT_PLAIN));

		String transcript = client.createTranscription(requestBody);

		assertThat(transcript).isEqualTo("Hello World!");

		server.verify();

	}

	@SpringBootConfiguration
	static class Config {

		@Bean
		public AudioApi audioApi(RestClient.Builder builder) {
			return new AudioApi("", TEST_API_KEY, builder);
		}

	}

}
