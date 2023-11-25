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

package com.logaritex.ai.api.demos;

import java.util.List;
import java.util.Map;

import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.Data;
import com.logaritex.ai.api.FileApi;
import com.logaritex.ai.api.Data.Message;
import com.logaritex.ai.api.Data.Run;

import org.springframework.core.io.DefaultResourceLoader;

/**
 * Retrieval Tool - expends the knowledge of your assistant: https://youtu.be/pq34V_V5j18?t=2014
 *
 * @author Christian Tzolov
 */
public class DemoRetrialTool {

	public static void main(String[] args) throws InterruptedException {

		var resourceLoader = new DefaultResourceLoader();

		FileApi fileApi = new FileApi(System.getenv("OPENAI_API_KEY"));

		// Upload the Spring Boot docs (pdf file).
		Data.File file = fileApi.uploadFile(
				resourceLoader.getResource("classpath:/spring-boot-reference.pdf"),
				Data.File.Purpose.ASSISTANTS);

		AssistantApi assistantApi = new AssistantApi(System.getenv("OPENAI_API_KEY"));

		Data.Assistant assistant = assistantApi.createAssistant(new Data.RequestBody(
				"gpt-4-1106-preview",
				"SpringBoot Wizard",
				"",
				"It's a helpful assistant, use the attached docs to answer questions about the SpringBoot.",
				List.of(new Data.Tool(Data.Tool.Type.retrieval)),
				List.of(file.id()), // Assign the PDF file to the assistant!!!
				Map.of()));

		// Create an empty Thread (represents a session between your user and your application).
		Data.Thread thread = assistantApi.createThread(new Data.ThreadRequest<>());

		// Add a new user Message to the Thread.
		assistantApi.createMessage(
				new Data.MessageRequest(Data.Role.user, "How to use Spring Integration?"),
				thread.id());

		// Crate a new Run - represents an execution run on a Thread with an Assistant.
		Data.Run run = assistantApi.createRun(
				thread.id(), // run this thread,
				new Data.RunRequest(assistant.id())); // with this assistant.

		// Run is asynchronous. Wait until it completes.
		while (assistantApi.retrieveRun(thread.id(), run.id()).status() != Run.Status.completed) {
			java.lang.Thread.sleep(500);
		}

		// Retrieve thread's messages. Result contains all 'assistant' and 'user' messages.
		Data.DataList<Data.Message<String>> messages = assistantApi.listMessages(
				new Data.ListRequest(),
				thread.id());

		System.out.println("Message count: " + messages.data().size());
		System.out.println("Thread messages: " + messages.data());

		// Extract only the assistant messages.
		List<Message<String>> assistantMessages = messages.data().stream().filter(m -> m.role() == Data.Role.assistant)
				.toList();

		System.out.println(assistantMessages);
	}

}
