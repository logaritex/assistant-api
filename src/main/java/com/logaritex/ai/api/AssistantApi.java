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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.Data.DataList;
import com.logaritex.ai.api.Data.ListRequest;
import com.logaritex.ai.api.Data.ResponseError;
import com.logaritex.ai.api.Data.RunRequest;
import com.logaritex.ai.api.Data.RunThreadRequest;
import com.logaritex.ai.api.Data.ThreadRequest;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 *
 * @author Christian Tzolov
 */
public class AssistantApi {

	private static final String HTTPS_API_OPENAI_COM = "https://api.openai.com";

	private final RestClient rest;
	private final Consumer<HttpHeaders> jsonContentHeaders;
	private final String openAiToken;

	private final Function<String, String> url;

	public AssistantApi(String openAiToken) {
		this(openAiToken, HTTPS_API_OPENAI_COM);
	}

	public AssistantApi(String openAiToken, String baseUrl) {
		this.rest = RestClient.create();
		this.openAiToken = openAiToken;
		this.jsonContentHeaders = headers -> {
			headers.set("OpenAI-Beta", "assistants=v1");
			headers.setBearerAuth(this.openAiToken);
			headers.setContentType(MediaType.APPLICATION_JSON);
		};
		this.url = suffix -> baseUrl + suffix;
	}

	private String url(String resource) {
		return this.url.apply(resource);
	}

	/**
	 * Create an assistant with a model and instructions.
	 *
	 * @param requestBody {@link Data.RequestBody} to specify required assistant.
	 * @return Returns an {@link Data.Assistant} object.
	 */
	public Data.Assistant createAssistant(Data.RequestBody requestBody) {

		Assert.notNull(requestBody, "The request body can not be null.");

		return this.rest.post()
				.uri(url("/v1/assistants"))
				.headers(this.jsonContentHeaders)
				.body(requestBody)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Assistant.class);
	}

	/**
	 * Retrieves an assistant by ID.
	 *
	 * @param assistantId The ID of the assistant to retrieve.
	 * @return Returns the {@link Data.Assistant} object matching the specified ID.
	 */
	public Data.Assistant retrieveAssistant(String assistantId) {

		Assert.hasText(assistantId, "Assistant ID can not be empty.");

		return this.rest.get()
				.uri(url("/v1/assistants/{assistant_id}"), assistantId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Assistant.class);
	}

	/**
	 * Modifies an assistant.
	 *
	 * @param requestBody {@link Data.RequestBody} with assistant modifications.
	 * @param assistantId The ID of the assistant to modify.
	 * @return Returns the modified {@link Data.Assistant} object.
	 */
	public Data.Assistant modifyAssistant(Data.RequestBody requestBody, String assistantId) {

		Assert.notNull(requestBody, "The request body can not be null.");
		Assert.hasText(assistantId, "Assistant ID can not be empty.");

		return this.rest.post()
				.uri(url("/v1/assistants/{assistant_id}"), assistantId)
				.headers(this.jsonContentHeaders)
				.body(requestBody)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Assistant.class);
	}

	/**
	 * Returns a list of assistants.
	 *
	 * @param listRequest List query parameters.
	 * @return Returns a list of {@link Data.Assistant} objects.
	 */
	public DataList<Data.Assistant> listAssistants(ListRequest listRequest) {

		return this.rest.get()
				.uri(url("/v1/assistants?order={order}&limit={limit}&before={before}&after={after}"),
						listRequest.order(), listRequest.limit(), listRequest.before(), listRequest.after())
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	/**
	 * Delete an assistant.
	 *
	 * @param assistantId The ID of the assistant to delete.
	 * @return Returns the deletion status.
	 */
	public Data.DeletionStatus deleteAssistant(String assistantId) {

		Assert.hasText(assistantId, "Assistant ID can not be empty.");

		return this.rest.delete()
				.uri(url("/v1/assistants/{assistant_id}"), assistantId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.DeletionStatus.class);
	}

	/**
	 * Create an assistant file by attaching a {@link Data.File} to an {@link Data.Assistant}.
	 *
	 * @param assistantId The ID of the assistant for which to create a File.
	 * @param fileId A {@link Data.File} ID (with purpose="assistants") that the assistant should use. Useful for tools
	 * like retrieval and code_interpreter that can access files.
	 * @return Returns an assistant file object.
	 */
	public Data.File createAssistantFile(String assistantId, String fileId) {
		Assert.hasText(assistantId, "Assistant ID can not be empty.");
		Assert.hasText(fileId, "File ID can not be empty.");

		return this.rest.post()
				.uri(url("/v1/assistants/{assistant_id}/files"), assistantId)
				.headers(this.jsonContentHeaders)
				.body(Map.of("file_id", fileId))
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.File.class);
	}

	/**
	 * Retrieves an AssistantFile.
	 * @param assistantId The ID of the assistant who the file belongs to.
	 * @param fileId The ID of the file we're getting.
	 * @return Returns the assistant file object matching the specified ID.
	 */
	public Data.File retrieveAssistantFile(String assistantId, String fileId) {
		Assert.hasText(assistantId, "Assistant ID can not be empty.");
		Assert.hasText(fileId, "File ID can not be empty.");

		return this.rest.get()
				.uri(url("/v1/assistants/{assistant_id}/files/{file_id}"), assistantId, fileId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.File.class);
	}

	/**
	 * Delete an assistant file.
	 *
	 * @param assistantId The ID of the assistant that the file belongs to.
	 * @param fileId The ID of the file to delete.
	 * @return Returns deletion status
	 */
	public Data.DeletionStatus deleteAssistantFile(String assistantId, String fileId) {
		Assert.hasText(assistantId, "Assistant ID can not be empty.");
		Assert.hasText(fileId, "File ID can not be empty.");

		return this.rest.delete()
				.uri(url("/v1/assistants/{assistant_id}/files/{file_id}"), assistantId, fileId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.DeletionStatus.class);
	}

	/**
	 * Returns a list of assistant files.
	 *
	 * @param assistantId The ID of the assistant the file belongs to.
	 * @return Returns a list of assistant file objects.
	 */
	public DataList<Data.File> listAssistantFiles(String assistantId) {
		Assert.hasText(assistantId, "Assistant ID can not be empty.");
		return this.rest.get()
				.uri(url("/v1/assistants/{assistant_id}/files"),
						assistantId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	// Threads

	/**
	 * Create threads that assistants can interact with.
	 *
	 * @param <T>
	 * @param createRequest
	 * @return Returns a thread object.
	 */
	public <T> Data.Thread createThread(ThreadRequest<T> createRequest) {
		Assert.notNull(createRequest, "Thread request can not be null.");

		return this.rest.post()
				.uri(url("/v1/threads"))
				.headers(this.jsonContentHeaders)
				.body(createRequest)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Thread.class);
	}

	/**
	 * Retrieves a thread.
	 *
	 * @param threadId The ID of the thread to retrieve.
	 * @return Returns the thread object matching the specified ID.
	 */
	public Data.Thread retrieveThread(String threadId) {
		Assert.hasText(threadId, "threadId can not be empty.");

		return this.rest.get()
				.uri(url("/v1/threads/{thread_id}"), threadId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Thread.class);
	}

	/**
	 * Modifies a thread.
	 *
	 * @param <T>
	 * @param modifyRequest modify {@link Data.ThreadRequest} body
	 * @param threadId The ID of the thread to modify. Only the metadata can be modified.
	 * @return Returns tTe modified thread object matching the specified ID.
	 */
	public <T> Data.Thread modifyThread(ThreadRequest<T> modifyRequest, String threadId) {
		Assert.notNull(modifyRequest, "Thread request can not be null.");
		Assert.hasText(threadId, "threadId can not be empty.");

		return this.rest.post()
				.uri(url("/v1/threads/{thread_id}"), threadId)
				.headers(this.jsonContentHeaders)
				.body(modifyRequest)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Thread.class);
	}

	public DataList<Data.Thread> listThreads(ListRequest listRequest) {
		Assert.notNull(listRequest, "The listRequest can not be null");

		return this.rest.get()
				.uri(url("/v1/threads?order={order}&limit={limit}&before={before}&after={after}"),
						listRequest.order(), listRequest.limit(), listRequest.before(), listRequest.after())
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	/**
	 * Delete a thread.
	 *
	 * @param threadId The ID of the thread to delete.
	 * @return Returns the deletion status
	 */
	public Data.DeletionStatus deleteThread(String threadId) {
		Assert.hasText(threadId, "threadId can not be empty.");
		return this.rest.delete()
				.uri(url("/v1/threads/{thread_id}"), threadId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.DeletionStatus.class);
	}

	// Messages
	// https://platform.openai.com/docs/api-reference/messages

	/**
	 * Create a message.
	 *
	 * @param <T>
	 * @param messageRequest
	 * @param threadId The ID of the {@link Data.Thread} to create a message for.
	 * @return Returns a {@link Data.Message} object.
	 */
	public <T> Data.Message<T> createMessage(Data.MessageRequest messageRequest, String threadId) {
		Assert.hasText(threadId, "threadId can not be empty.");
		return this.rest.post()
				.uri(url("/v1/threads/{thread_id}/messages"), threadId)
				.headers(this.jsonContentHeaders)
				.body(messageRequest)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	/**
	 * Retrieve a message.
	 *
	 * @param <T>
	 * @param threadId The ID of the thread to which this message belongs.
	 * @param messageId The ID of the message to retrieve.
	 * @return Returns the message object matching the specified ID.
	 */
	public <T> Data.Message<T> retrieveMessage(String threadId, String messageId) {
		Assert.hasText(threadId, "threadId can not be empty.");
		Assert.hasText(messageId, "messageId can not be empty.");

		return this.rest.get()
				.uri(url("/v1/threads/{thread_id}/messages/{message_id}"), threadId, messageId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	public <T> Data.Message<T> modifyMessage(Data.MessageRequest messageRequest, String threadId,
			String messageId) {
		Assert.notNull(messageRequest, "The 'messageRequest' can not be null");
		Assert.hasText(threadId, "The 'threadId' can not be empty.");
		Assert.hasText(messageId, "The 'messageId' can not be empty.");

		return this.rest.post()
				.uri(url("/v1/threads/{thread_id}/messages/{message_id}"), threadId, messageId)
				.headers(this.jsonContentHeaders)
				.body(messageRequest)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	public <T> DataList<Data.Message<T>> listMessages(ListRequest listRequest, String threadId) {
		Assert.notNull(listRequest, "The listRequest can not be null");
		Assert.hasText(threadId, "The threadId can not be empty.");

		return this.rest.get()
				.uri(url("/v1/threads/{thread_id}/messages?order={order}&limit={limit}&before={before}&after={after}"),
						threadId, listRequest.order(), listRequest.limit(), listRequest.before(), listRequest.after())
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	//
	// Runs - Represents an execution run on a thread.
	//
	/**
	 * Crate a new {@link Data.Run} object.
	 *
	 * @param threadId The ID of the thread to run.
	 * @param runRequest {@link Data.RunRequest} object containing the ID of the assistant to use to execute this run.
	 * @return Returns a {@link Data.Run} object.
	 */
	public Data.Run createRun(String threadId, RunRequest runRequest) {
		Assert.hasText(threadId, "The threadId can not be empty.");
		Assert.notNull(runRequest, "The runRequest can not be null.");

		return this.rest.post()
				.uri(url("/v1/threads/{thread_id}/runs"), threadId)
				.headers(this.jsonContentHeaders)
				.body(runRequest)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Run.class);
	}

	/**
	 * Retrieves a run.
	 *
	 * @param threadId The ID of the thread that was run.
	 * @param runId The ID of the run to retrieve.
	 * @return Returns the run object matching the specified ID.
	 */
	public Data.Run retrieveRun(String threadId, String runId) {
		Assert.hasText(threadId, "The threadId can not be empty.");
		Assert.hasText(runId, "The runId threadId can not be empty.");
		return this.rest.get()
				.uri(url("/v1/threads/{thread_id}/runs/{run_id}"), threadId, runId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Run.class);
	}

	/**
	 * Modifies a {@link Data.Run} object.
	 *
	 * @param threadId The ID of the thread that was run.
	 * @param runId The ID of the run to modify.
	 * @param metadata Metadata to modify.
	 * @return Returns modified run object matching the specified ID.
	 */
	public Data.Run modifyRun(String threadId, String runId, Map<String, String> metadata) {
		Assert.hasText(threadId, "The threadId can not be empty.");
		Assert.hasText(runId, "The runId can not be empty.");
		Assert.notNull(metadata, "The metadata can not be null.");

		return this.rest.post()
				.uri(url("/v1/threads/{thread_id}/runs/{run_id}"), threadId, runId)
				.headers(this.jsonContentHeaders)
				.body(metadata)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Run.class);
	}

	/**
	 * Returns a list of runs belonging to a thread.
	 *
	 * @param threadId The ID of the thread the run belongs to.
	 * @param listRequest
	 * @return Returns a list of run objects.
	 */
	public DataList<Data.Run> listRuns(String threadId, ListRequest listRequest) {
		Assert.hasText(threadId, "The threadId can not be empty.");
		Assert.notNull(listRequest, "The listRequest can not be null.");

		return this.rest.get()
				.uri(url("/v1/threads/{thread_id}/runs?order={order}&limit={limit}&before={before}&after={after}"),
						threadId, listRequest.order(), listRequest.limit(), listRequest.before(), listRequest.after())
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});

	}

	/**
	 * When a run has the 'status: "requires_action"' and 'required_action.type' is 'submit_tool_outputs', this endpoint
	 * can be used to submit the outputs from the tool calls once they're all completed. All outputs must be submitted
	 * in a single request.
	 *
	 * @param threadId The ID of the {@link Data.Thread} to which this run belongs.
	 * @param runId The ID of the {@link Data.Run} that requires the tool output submission.
	 * @return The modified {@link Data.Run} object matching the specified ID.
	 */
	public Data.Run submitToolOutputsToRun(String threadId, String runId, List<Data.ToolOutput> requestBody) {
		Assert.hasText(threadId, "The threadId can not be empty.");
		Assert.hasText(runId, "The runId can not be empty.");
		Assert.notNull(requestBody, "The requestBody can not be null.");

		return this.rest.post()
				.uri(url("/v1/threads/{thread_id}/runs/{run_id}/submit_tool_outputs"), threadId, runId)
				.headers(this.jsonContentHeaders)
				.body(requestBody)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Run.class);
	}

	/**
	 * Cancels a run that is in_progress.
	 *
	 * @param threadId The ID of the thread to which this run belongs.
	 * @param runId Required
	 * @return The modified {@link Data.Run} object matching the specified ID.
	 */
	public Data.Run cancelRun(String threadId, String runId) {
		Assert.hasText(threadId, "The threadId can not be empty.");
		Assert.hasText(runId, "The runId can not be empty.");

		return this.rest.post()
				.uri(url("/v1/threads/{thread_id}/runs/{run_id}/cancel"), threadId, runId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Run.class);
	}

	/**
	 * Create a thread and run it in one request.
	 *
	 * @param <T>
	 * @param runThreadRequest
	 * @return Returns a {@link Data.Run} object.
	 */
	public <T> Data.Run createThreadAndRun(RunThreadRequest<T> runThreadRequest) {
		Assert.notNull(runThreadRequest, "The runThreadRequest can not be null.");

		return this.rest.post()
				.uri(url("/v1/threads/runs"))
				.headers(this.jsonContentHeaders)
				.body(runThreadRequest)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.Run.class);
	}

	// Run Step

	/**
	 * Retrieves a {@link Data.RunStep}.
	 *
	 * @param threadId The ID of the thread to which the run and run step belongs.
	 * @param runId The ID of the run to which the run step belongs.
	 * @param runId The ID of the run step to retrieve.
	 * @return The {@link Data.RunStep} object matching the specified ID.
	 */
	public Data.RunStep retrieveRunStep(String threadId, String runId, String stepId) {
		Assert.hasText(threadId, "The threadId can not be empty.");
		Assert.hasText(runId, "The runId threadId can not be empty.");
		Assert.hasText(stepId, "The stepId threadId can not be empty.");

		return this.rest.get()
				.uri(url("/v1/threads/{thread_id}/runs/{run_id}/steps/{step_id}"), threadId, runId, stepId)
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.RunStep.class);
	}

	/**
	 * Returns a list of run steps belonging to a run.
	 *
	 * @param threadId The ID of the thread the run and run steps belong to.
	 * @param runId The ID of the run the run steps belong to.
	 * @param listRequest Query parameters
	 * @return A list of {@link Data.RunStep} objects.
	 */
	public DataList<Data.RunStep> listRunSteps(String threadId, String runId, ListRequest listRequest) {
		Assert.hasText(threadId, "The threadId can not be empty.");
		Assert.hasText(runId, "The runId can not be empty.");
		Assert.notNull(listRequest, "The listRequest can not be null.");

		return this.rest.get()
				.uri(url(
						"/v1/threads/{thread_id}/runs/{run_id}/steps?order={order}&limit={limit}&before={before}&after={after}"),
						threadId, runId, listRequest.order(), listRequest.limit(), listRequest.before(),
						listRequest.after())
				.headers(this.jsonContentHeaders)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	// Common helpers

	final ResponseErrorHandler responseErrorHandler = new ResponseErrorHandler() {
		@Override
		public boolean hasError(ClientHttpResponse response) throws IOException {
			if (response.getStatusCode().isError()) {
				throw new RuntimeException(String.format("%s - %s", response.getStatusCode().value(),
						new ObjectMapper().readValue(response.getBody(), ResponseError.class)));
			}
			return true;
		}

		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
			if (response.getStatusCode().isError()) {
				throw new RuntimeException(String.format("%s - %s", response.getStatusCode().value(),
						new ObjectMapper().readValue(response.getBody(), ResponseError.class)));
			}
		}
	};
}
