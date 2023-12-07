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

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.Data.ResponseError;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * Error handler assigned to all REST call to detect and report API call errors.
 *
 * @author Christian Tzolov
 */
public class OpenAiResponseErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return response.getStatusCode().isError();
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		if (response.getStatusCode().isError()) {
			throw new RuntimeException(String.format("%s - %s", response.getStatusCode().value(),
					new ObjectMapper().readValue(response.getBody(), ResponseError.class)));
		}
	}
}
