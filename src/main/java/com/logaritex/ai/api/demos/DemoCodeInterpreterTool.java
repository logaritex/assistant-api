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
import com.logaritex.ai.api.Data.Assistant;
import com.logaritex.ai.api.Data.Message;
import com.logaritex.ai.api.Data.Run;
import com.logaritex.ai.api.Data.ThreadRequest;

/**
 * On of the most important parts of Assistant is the ability to leverage tools to perform Actions autonomously.
 *
 * The "Code Interpreter" expands the Assistant's capabilities to include: accurate math, processing files, data
 * analysis, generating images...
 *
 * https://youtu.be/pq34V_V5j18?t=1734
 *
 * @author Christian Tzolov
 */
public class DemoCodeInterpreterTool {


	public static void main(String[] args) throws InterruptedException {

		FileApi fileApi = new FileApi(System.getenv("OPENAI_API_KEY"));
		AssistantApi assistantApi = new AssistantApi(System.getenv("OPENAI_API_KEY"));


		Assistant assistant = assistantApi.createAssistant(new Data.RequestBody(
				"gpt-4-1106-preview",
				"Personal finance genius",
				"",
				"You help users with their personal finance questions.",
				List.of(new Data.Tool(Data.Tool.Type.code_interpreter)),
				List.of(), Map.of()));

		//
		// Threads - represents a session between your user and your application.
		//

		// Create an empty thread.
		Data.Thread thread = assistantApi.createThread(new ThreadRequest<>(List.of(), Map.of()));

		// Add user message to the thread.
		// Role is from the user because the user types this message and the content is their question.
		Data.Message<String> message = assistantApi.createMessage(
				new Data.MessageRequest(Data.Role.user,
						"Generate a chart showing which day of the week I spend the most money?"),
				thread.id());

		//
		// Runs - Represents an execution run on a thread.
		//
		Data.Run run = assistantApi.createRun(thread.id(), new Data.RunRequest(assistant.id()));

		System.out.println("Run status: " + run.status());

		// Wait until the completed
		while (run.status() != Run.Status.completed) {
			java.lang.Thread.sleep(500);
			run = assistantApi.retrieveRun(thread.id(), run.id());
		}

		// Get all messages associated with the thread.
		Data.DataList<Data.Message<String>> messages = assistantApi.listMessages(new Data.ListRequest(),
				thread.id());

		System.out.println("Size: " + messages.data().size());
		System.out.println("Thread Messages: " + messages.data());

		List<Message<String>> assistantMessages = messages.data().stream().filter(m -> m.role() == Data.Role.assistant)
				.toList();
		// assistantMessages.forEach(m -> System.out.println(m.content()));
		System.out.println(assistantMessages);
		// Benefits of Assistant API:
		// 1. Don't have to store messages in my own DB.
		// 2. OpenAi handles truncating messages to fit the context window for me.
		// 3. The model output is generated even if I'm disconnected from the API (e.g. the Run async).
		// 4. I can always get the messages later as they remain saved to the thread.

	}

}
