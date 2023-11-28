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

package com.logaritex.ai.api.samples.retrieval;

import java.util.List;
import java.util.Map;

import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.Data;
import com.logaritex.ai.api.FileApi;
import com.logaritex.ai.api.Data.Message;
import com.logaritex.ai.api.Data.Run;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;

/**
 * Knowledge Retrieval Tool - expends the knowledge of the Assistant: https://youtu.be/pq34V_V5j18?t=2014
 *
 * This demo creates an Assistant instructed as a SpringBoot expert that can answer related question. The Retrieval Tool
 * is enabled to augment the Assistant with knowledge from outside its model, such as proprietary product information or
 * documents (e.g. the Spring Boot pdf docs). Once a doc files are uploaded and passed to the Assistant, OpenAI
 * automatically chunks the documents, index and store the embeddings, and implement vector search to retrieve relevant
 * content to answer user queries.
 *
 * The Retrieval Tool is likely a OpenAI, built-in, RAG solution with quite limited configuration options at the moment:
 * "Retrieval currently optimizes for quality by adding all relevant content to the context of model calls. We plan to
 * introduce other retrieval strategies to enable developers to choose a different tradeoff between retrieval quality
 * and model usage cost."
 *
 * @author Christian Tzolov
 */
public class KnowledgeRetrievalAssistant {

	private static final Log logger = LogFactory.getLog(KnowledgeRetrievalAssistant.class);

	public static void main(String[] args) throws InterruptedException {

		var resourceLoader = new DefaultResourceLoader();

		FileApi fileApi = new FileApi(System.getenv("OPENAI_API_KEY"));

		logger.info("1. Upload the Spring Boot (classpath:/spring-boot-reference.pdf) file.");
		// 1. Upload the Spring Boot docs - pdf file.
		Data.File file = fileApi.uploadFile(
				resourceLoader.getResource("classpath:/spring-boot-reference.pdf"),
				Data.File.Purpose.ASSISTANTS);

		AssistantApi assistantApi = new AssistantApi(System.getenv("OPENAI_API_KEY"));

		logger.info("2. Create assistant with the pdf file assigned.");
		// 2. Create assistant with the pdf file assigned.
		Data.Assistant assistant = assistantApi.createAssistant(new Data.AssistantRequestBody(
				"gpt-4-1106-preview",
				"SpringBoot Wizard",
				"",
				"You are Spring Boot expert, use the attached docs to answer questions about the SpringBoot.",
				List.of(new Data.Tool(Data.Tool.Type.retrieval)), // enable the knowledge retrieval tool (aka RAG).
				List.of(file.id()), // Assign the PDF file to the assistant!!!
				Map.of()));

		logger.info(" 3. Create an empty Thread (represents a session between your user and your application).");
		// 3. Create an empty Thread (represents a session between your user and your application).
		Data.Thread thread = assistantApi.createThread(new Data.ThreadRequest());

		logger.info(" 4. Add a new user Message to the Thread.");
		// 4. Add a new user Message to the Thread.
		assistantApi.createMessage(
				new Data.MessageRequest(Data.Role.user, "How to use Spring RestClient?"), // user question.
				thread.id());

		logger.info("5. Start a new Run - representing the execution of a Thread with an Assistant.");
		// 5. Start a new Run - representing the execution of a Thread with an Assistant.
		Data.Run run = assistantApi.createRun(
				thread.id(), // run this thread,
				new Data.RunRequest(assistant.id())); // with this assistant.

		logger.info("5.1. Wait until the run completes.");
		// 5.1. Wait until the run completes.
		while (assistantApi.retrieveRun(thread.id(), run.id()).status() != Run.Status.completed) {
			java.lang.Thread.sleep(500);
		}

		logger.info("6. Retrieve thread's messages. Result contains all 'assistant' and 'user' messages.");
		// 6. Retrieve thread's messages. Result contains all 'assistant' and 'user' messages.
		Data.DataList<Data.Message> messages = assistantApi.listMessages(
				new Data.ListRequest(),
				thread.id());

		logger.info("Message count: " + messages.data().size());
		// System.out.println("Thread messages: " + messages.data());

		logger.info("7. Extract only the assistant messages.");
		// 7. Extract only the assistant messages.
		List<Message> assistantMessages = messages.data().stream().filter(m -> m.role() == Data.Role.assistant)
				.toList();

		System.out.println(assistantMessages);

		logger.info(" 8. Delete the demo resources.");
		// 8. Delete the demo resources.
		// Comment out the deletion if you want to reuse the Assistant and Files in
		// https://platform.openai.com/assistants and https://platform.openai.com/files
		// fileApi.deleteFile(file.id());
		// assistantApi.deleteThread(thread.id());
		// assistantApi.deleteAssistant(assistant.id());
	}

}
