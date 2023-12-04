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

import java.util.function.Consumer;

import com.logaritex.ai.api.Data.DataList;
import com.logaritex.ai.api.Data.File;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Java client for the OpenAI Files API: https://platform.openai.com/docs/api-reference/files
 *
 * The Files API is used to upload documents that can be used with features like Assistants and Fine-tuning.
 *
 * @author Christian Tzolov
 */
public class FileApi {

	public static final String DEFAULT_BASE_URL = "https://api.openai.com";

	private final RestClient rest;
	private final Consumer<HttpHeaders> multipartContentHeaders;

	private final String openAiToken;
	private final ResponseErrorHandler responseErrorHandler;

	/**
	 * Create new FileApi instance.
	 * @param openAiToken Your OpenAPI api-key.
	 */
	public FileApi(String openAiToken) {
		this(DEFAULT_BASE_URL, openAiToken, RestClient.builder());
	}

	/**
	 * Create new FileApi instance.
	 * @param baseUrl the api base url.
	 * @param openAiToken Your OpenAPI api-key.
	 * @param restClientBuilder the {@link RestClient.Builder} to use.
	 */
	public FileApi(String baseUrl, String openAiToken, RestClient.Builder restClientBuilder) {
		this.openAiToken = openAiToken;

		this.multipartContentHeaders = headers -> {
			headers.setBearerAuth(openAiToken);
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		};

		this.responseErrorHandler = new OpenAiResponseErrorHandler();

		this.rest = restClientBuilder
				.baseUrl(baseUrl)
				.defaultHeaders(headers -> {
					headers.setBearerAuth(openAiToken);
					headers.setContentType(MediaType.APPLICATION_JSON);
				})
				.build();
	}

	/**
	 * Returns a list of files that belong to the given purpose.
	 *
	 * @param purpose Only return files with the given purpose.
	 * @return Returns a list of {@link Data.File}s object
	 */
	public DataList<Data.File> listFiles(File.Purpose purpose) {

		return this.rest.get()
				.uri("/v1/files?purpose={purpose}", purpose.getText())
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	/**
	 * Upload a file that can be used across various endpoints. The size of all the files uploaded by one organization
	 * can be up to 100 GB.
	 *
	 * The size of individual files can be a maximum of 512 MB. See the Assistants Tools guide to learn more about the
	 * types of files supported. The Fine-tuning API only supports .jsonl files.
	 *
	 * @param file The File object (not file name) to be uploaded.
	 * @param purpose The intended purpose of the uploaded file. Use "fine-tune" for Fine-tuning and "assistants" for
	 * Assistants and Messages.
	 *
	 * @return The uploaded {@link Data.File} object.
	 */
	public Data.File uploadFile(Resource file, File.Purpose purpose) {

		MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
		multipartBody.add("purpose", purpose.getText());
		multipartBody.add("file", file);

		return this.rest.post()
				.uri("/v1/files")
				.headers(this.multipartContentHeaders)
				.body(multipartBody)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.File.class);
	}

	/**
	 * Deletes a file by ID.
	 *
	 * @param fileId The ID of the file to use for this request.
	 * @return Return the file deletion status.
	 */
	public Data.DeletionStatus deleteFile(String fileId) {
		return this.rest.delete()
				.uri("/v1/files/{file_id}", fileId)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.DeletionStatus.class);
	}

	/**
	 * Retrieve the {@link Data.File} object matching the specified ID.
	 *
	 * @param fileId The ID of the file to use for this request.
	 * @return Return the {@link Data.File}.
	 */
	public Data.File retrieveFile(String fileId) {
		return this.rest.get()
				.uri("/v1/files/{file_id}", fileId)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.File.class);
	}

	/**
	 * Returns the contents of the specified file.
	 *
	 * @param fileId The ID of the file to use for this request.
	 * @return Returns the byte array content of the file.
	 */
	public byte[] retrieveFileContent(String fileId) {
		return this.rest.get()
				.uri("/v1/files/{file_id}/content", fileId)
				.headers(headers -> {
					headers.setBearerAuth(this.openAiToken);
				})
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(byte[].class);
	}

	/**
	 * Exception throw if FileApi error is detected.
	 */
	public static class FileApiException extends RuntimeException {
		public FileApiException(String message) {
			super(message);
		};
	}
}
