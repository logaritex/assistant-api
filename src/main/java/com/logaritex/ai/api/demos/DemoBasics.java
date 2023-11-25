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
import com.logaritex.ai.api.Data.Run;
import com.logaritex.ai.api.Data.TextContent;
import com.logaritex.ai.api.Data.ThreadRequest;

/**
 * https://youtu.be/pq34V_V5j18?t=1494
 *
 * @author Christian Tzolov
 */
public class DemoBasics {

	// Benefits of Assistant API:
	//
	// 1. Don't have to store messages in my own DB.
	// 2. OpenAi handles truncating messages to fit the context window for me.
	// 3. The model output is generated even if I'm disconnected from the API (e.g. the Run async).
	// 4. I can always get the messages later as they remain saved to the thread.

	public static void main(String[] args) throws InterruptedException {

		// Connect to the assistant API.
		// https://platform.openai.com/docs/assistants/overview
		AssistantApi assistantApi = new AssistantApi(System.getenv("OPENAI_API_KEY"));

		// Crate and assistant.
		Data.Assistant assistant = assistantApi.createAssistant(new Data.RequestBody(
				"gpt-4-1106-preview",
				"You are a expert in geography, be helpful and concise."));

		//
		// Threads - represents a session between your user and your application.
		//

		// Create an empty thread.
		Data.Thread thread = assistantApi.createThread(new ThreadRequest<>(List.of(), Map.of()));

		// Add user message to the thread.
		// Role is from the user because the user types this message and the content is their question.
		assistantApi.createMessage(
				new Data.MessageRequest(Data.Role.user, "What is the capital of France?"),
				thread.id());

		//
		// Runs - Represents an execution run on a thread.
		//

		// Create run to assign the thread with our geography assistant.
		// P.S. The same thread can be associated (e.g. run) with multiple, different assistants.
		Data.Run run = assistantApi.createRun(thread.id(), new Data.RunRequest(assistant.id()));

		System.out.println("Run status: " + run.status()); // expected to be 'queued'

		// Wait until the Run completes, e.g. the answer is received.
		while (assistantApi.retrieveRun(thread.id(), run.id()).status() != Run.Status.completed) {
			java.lang.Thread.sleep(500);
		}

		// Get all messages associated with the thread.
		Data.DataList<Data.Message<Data.TextContent>> messages = assistantApi.listMessages(new Data.ListRequest(),
				thread.id());

		System.out.println("Size: " + messages.data().size());
		System.out.println("Thread Messages: " + messages.data());

		// Thread contains both user's and assistant's messages. Filter out only the assistant one.
		List<Data.Message<Data.TextContent>> assistantMessages = messages.data().stream()
				.filter(msg -> msg.role() == Data.Role.assistant).toList();

		System.out.println(assistantMessages);

		List<List<TextContent>> list = assistantMessages.stream().map(msg -> msg.content()).toList();
		System.out.println(list);
	}

}
