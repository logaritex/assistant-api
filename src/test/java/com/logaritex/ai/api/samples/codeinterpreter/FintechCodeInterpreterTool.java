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

package com.logaritex.ai.api.samples.codeinterpreter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.Data;
import com.logaritex.ai.api.Data.Assistant;
import com.logaritex.ai.api.Data.Message;
import com.logaritex.ai.api.Data.Run;
import com.logaritex.ai.api.Data.ThreadRequest;
import com.logaritex.ai.api.FileApi;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;

/**
 * WIP On of the most important parts of Assistant is the ability to leverage tools to perform Actions autonomously.
 *
 * The "Code Interpreter" expands the Assistant's capabilities to include: accurate math, processing files, data
 * analysis, generating images...
 *
 * https://youtu.be/pq34V_V5j18?t=1734
 *
 * @author Christian Tzolov
 */
public class FintechCodeInterpreterTool {

	public static void main(String[] args) throws InterruptedException, IOException {

		var resourceLoader = new DefaultResourceLoader();
		FileApi fileApi = new FileApi(System.getenv("OPENAI_API_KEY"));

		Data.File file = fileApi.uploadFile(
				resourceLoader.getResource("classpath:/MSFT.csv"),
				Data.File.Purpose.ASSISTANTS);

		AssistantApi assistantApi = new AssistantApi(System.getenv("OPENAI_API_KEY"));

		Assistant assistant = assistantApi.createAssistant(new Data.AssistantRequestBody(
				"gpt-4-1106-preview",
				"Personal finance genius",
				"",
				"You help users with finance and stock exchange questions.",
				List.of(new Data.Tool(Data.Tool.Type.code_interpreter)),
				List.of(file.id()),
				Map.of()));

		//
		// Threads - represents a session between your user and your application.
		//

		// Create an empty thread.
		Data.Thread thread = assistantApi.createThread(new ThreadRequest(List.of(), Map.of()));

		// Add user message to the thread.
		// Role is from the user because the user types this message and the content is their question.
		assistantApi.createMessage(
				new Data.MessageRequest(Data.Role.user,
						"Use the attached file to generate a chart showing the MSFT stock value changing over time."),
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
		Data.DataList<Data.Message> messageList = assistantApi.listMessages(new Data.ListRequest(),
				thread.id());

		System.out.println("Size: " + messageList.data().size());
		System.out.println("Thread Messages: " + messageList.data());

		List<Message> assistantMessages = messageList.data().stream().filter(m -> m.role() == Data.Role.assistant)
				.toList();
		// assistantMessages.forEach(m -> System.out.println(m.content()));
		System.out.println(assistantMessages);

		Data.Message lastAssistantMessage = assistantApi.retrieveMessage(thread.id(), messageList.first_id());

		String chartImageId = lastAssistantMessage.content().stream()
				.filter(c -> c.type() == Data.Content.Type.image_file).findFirst()
				.get().image_file()
				.file_id();

		byte[] fileContent = fileApi.retrieveFileContent(chartImageId);

		var fos = new FileOutputStream("msft-chart.png");

		StreamUtils.copy(fileContent, fos);

		fos.close();
	}

}
