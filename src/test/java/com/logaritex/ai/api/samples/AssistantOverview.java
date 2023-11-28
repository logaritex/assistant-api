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

package com.logaritex.ai.api.samples;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.Data;
import com.logaritex.ai.api.Data.AssistantRequestBody;
import com.logaritex.ai.api.Data.DataList;
import com.logaritex.ai.api.Data.ListRequest;
import com.logaritex.ai.api.Data.Message;
import com.logaritex.ai.api.Data.MessageRequest;
import com.logaritex.ai.api.Data.Role;
import com.logaritex.ai.api.Data.Run;
import com.logaritex.ai.api.Data.RunRequest;
import com.logaritex.ai.api.Data.ThreadRequest;
import com.logaritex.ai.api.Data.Tool;

/**
 * Java implementation of the Assistant Overview example: https://platform.openai.com/docs/assistants/overview
 *
 * @author Christian Tzolov
 */
public class AssistantOverview {

	public static void main(String[] args) throws InterruptedException {

		// 1. Connect to the Assistant API.
		AssistantApi assistantApi = new AssistantApi(System.getenv("OPENAI_API_KEY"));

		// 2. Create a Math Tutor assistant.
		Data.Assistant assistant = assistantApi.createAssistant(new AssistantRequestBody(
				"gpt-4-1106-preview", // model
				"Math Tutor", // name
				"", // description
				"You are a personal math tutor. Write and run code to answer math questions.", // instructions
				List.of(new Tool(Tool.Type.code_interpreter)), // tools
				List.of(), // file ids
				Map.of())); // metadata

		// 3. Create an empty Thread.
		Data.Thread thread = assistantApi.createThread(new ThreadRequest());

		// 4. Add a Message to a Thread
		assistantApi.createMessage(
				new MessageRequest(Role.user, "I need to solve the equation `3x + 11 = 14`. Can you help me?"),
				thread.id()); // Thread id to add the message to.

		// 5. Run the Assistant
		Data.Run run = assistantApi.createRun(
				thread.id(), // run this thread,
				new RunRequest(assistant.id())); // with this assistant.

		// 6. Check the Run status.
		while (assistantApi.retrieveRun(thread.id(), run.id()).status() != Run.Status.completed) {
			java.lang.Thread.sleep(500);
		}

		// 7. Display the Assistant's Response
		DataList<Message> messages = assistantApi.listMessages(
				new ListRequest(), thread.id());

		// 7.1 Filter out the assistant messages only.
		List<Message> assistantMessages = messages.data().stream()
				.filter(msg -> msg.role() == Role.assistant).toList();

		System.out.println(assistantMessages
				.stream()
				.map(AssistantOverview::toJson)
				.collect(Collectors.joining("\n")));

		// 8. Delete the demo resources.
		// Comment out the deletion if you want to reuse the Assistant in
		// https://platform.openai.com/assistants
		// assistantApi.deleteThread(thread.id());
		// assistantApi.deleteAssistant(assistant.id());
	}

	private static String toJson(Object message) {
		try {
			return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(message);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
