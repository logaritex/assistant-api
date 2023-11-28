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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.ai.api.Data.Content.ImageFile;
import com.logaritex.ai.api.Data.Content.Text;
import com.logaritex.ai.api.Data.Content.Type;
import com.logaritex.ai.api.Data.ListRequest.Order;
import com.logaritex.ai.api.Data.Run.RequiredAction;
import com.logaritex.ai.api.Data.Run.RequiredAction.SubmitToolOutputs;
import com.logaritex.ai.api.Data.Run.RunError;
import com.logaritex.ai.api.Data.Run.Status;
import com.logaritex.ai.api.Data.RunStep.Error;
import com.logaritex.ai.api.Data.RunStep.RunStepStatus;
import com.logaritex.ai.api.Data.RunStep.RunStepType;
import com.logaritex.ai.api.Data.ToolOutputs.ToolOutput;

/**
 * the {@link Data} defines all domain objects such as request, responses, enum used by the {@link AssistantApi} and
 * {@link FileApi} clients.
 *
 * @author Christian Tzolov
 */
public class Data {

	/**
	 * Represents an Assistant tool.
	 */
	public static class Tool {

		public enum Type {
			code_interpreter, retrieval, function
		}

		private Type type;

		Tool() {
		}

		public Tool(Type type) {
			this.type = type;
		}

		public Type getType() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}
	}

	/**
	 * Function calling Assistant Tool
	 */
	public static class FunctionTool extends Data.Tool {

		/**
		 * Tool function definition
		 */
		public static class Function {

			/**
			 * A description of what the function does, used by the model to choose when and how to call the function.
			 */
			private final String description;

			/**
			 * The name of the function to be called. Must be a-z, A-Z, 0-9, or contain underscores and dashes, with a
			 * maximum length of 64.
			 */
			private final String name;

			/**
			 * The parameters the functions accepts, described as a JSON Schema object. See the guide for examples, and
			 * the JSON Schema reference for documentation about the format. To describe a function that accepts no
			 * parameters, provide the value {"type": "object", "properties": {}}.
			 */
			private final Map<String, Object> parameters;

			/**
			 * Create tool function definition.
			 *
			 * @param description tool function description.
			 * @param name tool function name.
			 * @param parametersJsonSchema tool function schema as json.
			 */
			public Function(String description, String name, String parametersJsonSchema) {
				this(description, name, parseSchema(parametersJsonSchema));
			}

			private static Map<String, Object> parseSchema(String parametersJsonSchema) {
				try {
					return new ObjectMapper().readValue(parametersJsonSchema,
							new TypeReference<Map<String, Object>>() {
							});
				}
				catch (Exception e) {
					throw new RuntimeException("Failed to parse schema: " + parametersJsonSchema, e);
				}
			}

			/**
			 * Create tool function definition.
			 *
			 * @param description tool function description.
			 * @param name tool function name.
			 * @param parameters tool function schema as map.
			 */
			public Function(String description, String name, Map<String, Object> parameters) {
				this.description = description;
				this.name = name;
				this.parameters = parameters;
			}

			/**
			 * @return function's description.
			 */
			public String getDescription() {
				return description;
			}

			/**
			 * @return function's name.
			 */
			public String getName() {
				return name;
			}

			/**
			 * @return function's parameters.
			 */
			public Map<String, Object> getParameters() {
				return parameters;
			}
		}

		private final Function function;

		/**
		 * Create function tool.
		 * @param function function definition.
		 */
		public FunctionTool(Function function) {
			super(Tool.Type.function);
			this.function = function;
		}

		/**
		 * @return Returns Tool's function definition.
		 */
		public Function getFunction() {
			return function;
		}
	}

	/**
	 * Represents an assistant that can call the model and use tools.
	 *
	 * @param id The identifier, which can be referenced in API endpoints.
	 * @param object The object type, which is always 'assistant'.
	 * @param created_at The Unix timestamp (in seconds) for when the assistant was created.
	 * @param name The name of the assistant. The maximum length is 256 characters.
	 * @param description The description of the assistant. The maximum length is 512 characters.
	 * @param model ID of the model to use.
	 * @param instructions The system instructions that the assistant uses. The maximum length is 32768 characters.
	 * @param tools A list of tool enabled on the assistant. There can be a maximum of 128 tools per assistant. Tools
	 * can be of types 'code_interpreter', 'retrieval', or 'function'.
	 * @param file_ids A list of {@link Data.File} IDs attached to this assistant. There can be a maximum of 20 files
	 * attached to the assistant. Files are ordered by their creation date in ascending order.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	public record Assistant(String id, String object, Long created_at, String name, String description, String model,
			String instructions, List<Tool> tools, List<String> file_ids, Map<String, String> metadata) {
	}

	/**
	 * Assistant creation request.
	 *
	 * @param model ID of the model to use.
	 * @param name The name of the assistant. The maximum length is 256 characters.
	 * @param description The description of the assistant. The maximum length is 512 characters.
	 * @param instructions The system instructions that the assistant uses. The maximum length is 32768 characters.
	 * @param tools A list of tool enabled on the assistant. There can be a maximum of 128 tools per assistant. Tools
	 * can be of types code_interpreter, retrieval, or function.
	 * @param file_ids A list of file IDs attached to this assistant. There can be a maximum of 20 files attached to the
	 * assistant. Files are ordered by their creation date in ascending order.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	@JsonInclude(Include.NON_NULL)
	public record AssistantRequestBody(String model, String name, String description, String instructions,
			List<Tool> tools,
			List<String> file_ids, Map<String, String> metadata) {

		/**
		 * Assistant creation request.
		 * @param model ID of the model to use.
		 * @param instructions The system instructions that the assistant uses. The maximum length is 32768 characters.
		 */
		public AssistantRequestBody(String model, String instructions) {
			this(model, null, null, instructions, List.of(), List.of(), Map.of());
		}
	}

	/**
	 * The File object represents a document that has been uploaded to OpenAI.
	 *
	 * @param id The file identifier, which can be referenced in the API endpoints.
	 * @param bytes The size of the file, in bytes.
	 * @param created_at The Unix timestamp (in seconds) for when the file was created.
	 * @param filename The name of the file.
	 * @param object The object type, which is always file.
	 * @param purpose The intended purpose of the file. Supported values are fine-tune, fine-tune-results, assistants,
	 * and assistants_output.
	 * @param status file status.
	 * @param status_details status details.
	 */
	public record File(String id, Integer bytes, Long created_at, String filename, String object, String purpose,
			String status, String status_details) {

		/**
		 * File purpose.
		 */
		public enum Purpose {

			/**
			 * File to be used for fine tunning.
			 */
			FINE_TUNE("fine-tune"),
			/**
			 * File to be used by the Assistant.
			 */
			ASSISTANTS("assistants");

			private final String text;

			/**
			 * @return purpose as string.
			 */
			public String getText() {
				return text;
			}

			Purpose(String text) {
				this.text = text;
			}

		};

	}

	/**
	 * Deletion status.
	 *
	 * @param id status id
	 * @param object should be
	 * @param deleted true if successful adn false otherwise.
	 */
	public record DeletionStatus(String id, String object, Boolean deleted) {
	}

	/**
	 * Query request.
	 */
	public record ListRequest(Order order, Integer limit, String before, String after) {

		/**
		 * Query order
		 */
		public enum Order {
			/**
			 * Ascendant order.
			 */
			asc,
			/**
			 * Descendant order.
			 */
			desc
		}

		/**
		 * Query request.
		 */
		public ListRequest() {
			this(Order.desc, 20, "", "");
		}

		/**
		 * Query request.
		 * @param order query order.
		 */
		public ListRequest(Order order) {
			this(order, 20, "", "");
		}

		/**
		 * Query request.
		 * @param order query order.
		 * @param limit max response size.
		 */
		public ListRequest(Order order, int limit) {
			this(order, limit, "", "");
		}
	}

	/**
	 * Common list wrapper for API's list responses.
	 *
	 * @param <T> Type of the entity in the data list.
	 * @param object Must have value "list".
	 * @param data List of entities.
	 * @param first_id The ID of the first entity in the data list.
	 * @param last_id The ID of the last entity in the data list.
	 * @param has_mode ???
	 */
	public record DataList<T>(String object, List<T> data, String first_id, String last_id,
			boolean has_mode) {
	}

	/**
	 * Thread that assistants can interact with. Represents a thread that contains messages:
	 * https://platform.openai.com/docs/api-reference/threads/object .
	 *
	 * @param id The identifier, which can be referenced in API endpoints.
	 * @param object The object type, which is always `thread`.
	 * @param created_at The Unix timestamp (in seconds) for when the thread was created.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	public record Thread(String id, String object, Long created_at, Map<String, String> metadata) {
	}

	/**
	 * Thread creation request body.
	 *
	 * @param messages A list of {@link Data.Message}s to start the thread with.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	public record ThreadRequest(List<Message> messages, Map<String, String> metadata) {

		public ThreadRequest() {
			this(List.of(), Map.of());
		}
	}

	/**
	 * Role of the message producer entity.
	 */
	public enum Role {
		/**
		 * Use message role.
		 */
		user,
		/**
		 * Assistant message role.
		 */
		assistant
	}

	/**
	 * Represents a message within a thread.
	 *
	 * @param id The identifier, which can be referenced in API endpoints.
	 * @param object The object type, which is always 'thread.message'.
	 * @param created_at The Unix timestamp (in seconds) for when the message was created.
	 * @param thread_id The {@link Thread} ID that this message belongs to.
	 * @param role The entity that produced the message. One of 'user' or 'assistant'.
	 * @param content The content of the message in array of text and/or images.
	 * @param assistant_id If applicable, the ID of the {@link Assistant} that authored this message.
	 * @param run_id If applicable, the ID of the run associated with the authoring of this message.
	 * @param file_ids A list of {@link File} IDs that the assistant should use. Useful for tools like retrieval and
	 * code_interpreter that can access files. A maximum of 10 files can be attached to a message.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	public record Message(String id, String object, Long created_at, String thread_id, Role role, List<Content> content,
			String assistant_id, String run_id, List<String> file_ids, Map<String, String> metadata) {
	}

	/**
	 * Message content. Can hold either text or image types. Mutually exclusive.
	 *
	 * @param type should be set to 'text' or 'image'.
	 * @param text text message content when the type is 'text' and null otherwise.
	 * @param image_file image file id when the type is 'image' and null otherwise.
	 */
	public record Content(Type type, Text text, ImageFile image_file) {

		/**
		 * Message content type.
		 */
		public enum Type {
			/**
			 * Message content is of Text type.
			 */
			text,
			/**
			 * Message content is of type Image.
			 */
			image_file;
		}

		/**
		 * Content's text.
		 * @param value Content's string text value. Can contain placeholders to be replaced with annotation values.
		 * @param annotations Content's annotations.
		 */
		public record Text(String value, List<Map<String, Object>> annotations) {
		}

		/**
		 * Image file reference.
		 *
		 * @param file_id File id.
		 */
		public record ImageFile(String file_id) {
		}
	}

	/**
	 * Message creation request.
	 *
	 * @param role The entity that produced the message. One of 'user' or 'assistant'.
	 * @param file_ids A list of {@link File} IDs that the assistant should use. Useful for tools like retrieval and
	 * code_interpreter that can access files. A maximum of 10 files can be attached to a message.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	@JsonInclude(Include.NON_NULL)
	public record MessageRequest(Role role, String content, List<String> file_ids, Map<String, String> metadata) {

		/**
		 * Message creation request.
		 * @param role The entity that produced the message. One of 'user' or 'assistant'.
		 * @param content Message text content.
		 */
		public MessageRequest(Role role, String content) {
			this(role, content, null, null);
		}

	}

	/**
	 *
	 * Represents an execution run on a {@link Data.Thread}.
	 *
	 * @param id The identifier, which can be referenced in API endpoints.
	 * @param object The object type, which is always 'thread.run'.
	 * @param created_at The Unix timestamp (in seconds) for when the run was created.
	 * @param thread_id The ID of the thread that was executed on as a part of this run.
	 * @param assistant_id The ID of the assistant used for execution of this run.
	 * @param status The status of the run, which can be either 'queued', 'in_progress', 'requires_action',
	 * 'cancelling', 'cancelled', 'failed', 'completed', or 'expired'.
	 * @param required_action Details on the action required to continue the run. Will be null if no action is required.
	 * @param last_error The last error associated with this run. Will be null if there are no errors.
	 * @param expires_at The Unix timestamp (in seconds) for when the run will expire.
	 * @param started_at The Unix timestamp (in seconds) for when the run was started.
	 * @param cancelled_at The Unix timestamp (in seconds) for when the run was cancelled.
	 * @param failed_at The Unix timestamp (in seconds) for when the run failed.
	 * @param completed_at completed_at
	 * @param model The model that the assistant used for this run.
	 * @param instructions The instructions that the assistant used for this run.
	 * @param tools The list of tools that the assistant used for this run.
	 * @param file_ids The list of File IDs the assistant used for this run.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	public record Run(String id, String object, Long created_at, String thread_id, String assistant_id,
			Status status, RequiredAction required_action, RunError last_error, Long expires_at, Long started_at,
			Long cancelled_at, Long failed_at, Long completed_at, String model, String instructions, List<Tool> tools,
			List<String> file_ids, Map<String, String> metadata) {

		/**
		 * Run status.
		 */
		public enum Status {
			queued, in_progress, requires_action, cancelling, cancelled, failed, completed, expired
		};

		/**
		 * Details on the action required to continue the run.
		 *
		 * @param type For now, this is always 'submit_tool_outputs'.
		 * @param submit_tool_outputs Details on the tool outputs needed for this run to continue.
		 */
		public record RequiredAction(String type, SubmitToolOutputs submit_tool_outputs) {

			/**
			 * Details on the tool outputs needed for this run to continue.
			 *
			 * @param tool_calls A list of the relevant tool calls.
			 */
			public record SubmitToolOutputs(List<ToolCall> tool_calls) {

			}

			/**
			 * The relevant tool call.
			 *
			 * @param id The ID of the tool call. This ID must be referenced when you submit the tool outputs in using
			 * the Submit tool outputs to run endpoint.
			 * @param type The type of tool call the output is required for. For now, this is always function.
			 * @param function The function definition.
			 */
			public record ToolCall(String id, String type, Function function) {

			}

			/**
			 * The function definition.
			 *
			 * @param name The name of the function.
			 * @param arguments The arguments that the model expects you to pass to the function.
			 */
			public record Function(String name, String arguments) {

			}
		}

		/**
		 *
		 * @param code One of server_error or rate_limit_exceeded.
		 * @param message A human-readable description of the error.
		 */
		public record RunError(String code, String message) {
		}
	}

	/**
	 * Run Request body.
	 *
	 * @param assistant_id The ID of the assistant to use to execute this run.
	 * @param model The ID of the Model to be used to execute this run. If a value is provided here, it will override
	 * the model associated with the assistant. If not, the model associated with the assistant will be used.
	 * @param instructions Override the default system message of the assistant. This is useful for modifying the
	 * behavior on a per-run basis.
	 * @param tools Override the tools the assistant can use for this run. This is useful for modifying the behavior on
	 * a per-run basis.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	@JsonInclude(Include.NON_NULL)
	public record RunRequest(String assistant_id, String model, String instructions, List<Data.Tool> tools,
			Map<String, String> metadata) {
		public RunRequest(String assistant_id) {
			this(assistant_id, null, null, null, null);
		}
	}

	/**
	 * Run Request body.
	 *
	 * @param assistant_id The ID of the assistant to use to execute this run.
	 * @param thread {@link ThreadRequest} object containing messages and metadata to start the Thread with.
	 * @param model The ID of the Model to be used to execute this run. If a value is provided here, it will override
	 * the model associated with the assistant. If not, the model associated with the assistant will be used.
	 * @param instructions Override the default system message of the assistant. This is useful for modifying the
	 * behavior on a per-run basis.
	 * @param tools Override the tools the assistant can use for this run. This is useful for modifying the behavior on
	 * a per-run basis.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	@JsonInclude(Include.NON_NULL)
	public record RunThreadRequest(String assistant_id, ThreadRequest thread, String model, String instructions,
			List<Data.Tool> tools, Map<String, String> metadata) {

		public RunThreadRequest(String assistant_id) {
			this(assistant_id, null, null, null, null, null);
		}

	}

	/**
	 * Represents a step in execution of a {@link Data.Run}. It provides a detailed list of steps the Assistant took as
	 * part of a Run. An Assistant can call tools or create Messages during it’s run. Examining Run Steps allows you to
	 * introspect how the Assistant is getting to it’s final results.
	 *
	 * @param id The identifier of the run step, which can be referenced in API endpoints.
	 * @param object The object type, which is always 'thread.run.step'.
	 * @param created_at The Unix timestamp (in seconds) for when the run step was created.
	 * @param assistant_id The ID of the {@link Data.Assistant} associated with the run step.
	 * @param thread_id The ID of the {@link Data.Thread} that was run.
	 * @param run_id The ID of the {@link Data.Run} that this run step is a part of.
	 * @param type The type of run step, which can be either 'message_creation' or 'tool_calls'.
	 * @param status The status of the run step, which can be either 'in_progress', 'cancelled', 'failed', 'completed',
	 * or 'expired'.
	 * @param step_details The details of the run step. (TODO)
	 * @param last_error The last error associated with this run step. Will be null if there are no errors.
	 * @param expired_at The Unix timestamp (in seconds) for when the run step expired. A step is considered expired if
	 * the parent run is expired.
	 * @param cancelled_at The Unix timestamp (in seconds) for when the run step was cancelled.
	 * @param failed_at The Unix timestamp (in seconds) for when the run step failed.
	 * @param completed_at The Unix timestamp (in seconds) for when the run step completed.
	 * @param metadata Set of 16 key-value pairs that can be attached to an object. This can be useful for storing
	 * additional information about the object in a structured format. Keys can be a maximum of 64 characters long and
	 * values can be a maximum of 512 characters long.
	 */
	public record RunStep(String id, String object, Long created_at, String assistant_id, String thread_id,
			String run_id, RunStepType type, RunStepStatus status, Map<String, Object> step_details, Error last_error,
			Long expired_at, Long cancelled_at, Long failed_at, Long completed_at, Map<String, String> metadata) {

		public enum RunStepType {
			message_creation, tool_calls
		}

		public enum RunStepStatus {
			in_progress, cancelled, failed, completed, expired
		}

		/**
		 * Run Step error.
		 *
		 * @param code One of 'server_error' or 'rate_limit_exceeded'.
		 * @param message A human-readable description of the error.
		 */
		public record Error(String code, String message) {
		}

	}

	/**
	 * Holder for a list of tool outputs
	 *
	 * @param tool_outputs List of tool outputs.
	 */
	public record ToolOutputs(List<ToolOutput> tool_outputs) {
		/**
		 * Tool for which the outputs are being submitted.
		 *
		 * @param tool_call_id The ID of the tool call in the 'required_action' object within the run object the output
		 * is being submitted for.
		 * @param output The output of the tool call to be submitted to continue the run.
		 */
		public record ToolOutput(String tool_call_id, String output) {

		}
	}

	/**
	 * API error response.
	 */
	public record ResponseError(Error error) {
		private record Error(String message, String type, String param, String code) {
		}
	}

}
