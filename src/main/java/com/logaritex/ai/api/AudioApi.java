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

import java.io.FileOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.logaritex.ai.api.AudioApi.SpeechRequest.ResponseFormat;
import com.logaritex.ai.api.AudioApi.SpeechRequest.Voice;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Turn audio into text or text into audio. https://beta.openai.com/docs/api-reference/audio
 *
 * @author Christian Tzolov
 */
public class AudioApi {

	private static final String DEFAULT_BASE_URL = "https://api.openai.com";

	private final ResponseErrorHandler responseErrorHandler;
	private final RestClient restClient;

	/**
	 * Create an new audio api.
	 *
	 * @param openAiToken OpenAI apiKey.
	 */
	public AudioApi(String openAiToken) {
		this(DEFAULT_BASE_URL, openAiToken, RestClient.builder());
	}

	/**
	 * Create an new chat completion api.
	 *
	 * @param baseUrl api base URL.
	 * @param openAiToken OpenAI apiKey.
	 * @param restClientBuilder RestClient builder.
	 */
	public AudioApi(String baseUrl, String openAiToken, RestClient.Builder restClientBuilder) {

		this.responseErrorHandler = new OpenAiResponseErrorHandler();

		this.restClient = restClientBuilder
				.baseUrl(baseUrl)
				.defaultHeaders(headers -> {
					headers.setBearerAuth(openAiToken);
				})
				.build();
	}

	/**
	 * Request to generates audio from the input text.
	 *
	 * @param model The model to use for generating the audio. One of the available TTS models: tts-1 or tts-1-hd.
	 * @param input The input text to synthesize. Must be at most 4096 tokens long.
	 * @param voice The voice to use for synthesis. One of the available voices for the chosen model: 'alloy', 'echo',
	 * 'fable', 'onyx', 'nova', and 'shimmer'.
	 * @param response_format The format to audio in. Supported formats are mp3, opus, aac, and flac. Defaults to mp3.
	 * @param speed The speed of the voice synthesis. The acceptable range is from 0.0 (slowest) to 1.0 (fastest).
	 */
	@JsonInclude(Include.NON_NULL)
	public record SpeechRequest(
			@JsonProperty("model") String model,
			@JsonProperty("input") String input,
			@JsonProperty("voice") Voice voice,
			@JsonProperty("response_format") ResponseFormat response_format,
			@JsonProperty("speed") Float speed) {

		/**
		 * Create a new speech request with speed defaulting to 1.0f.
		 *
		 * @param model The model to use for generating the audio.
		 * @param input The input text to synthesize.
		 * @param voice The voice to use for synthesis.
		 *
		 */
		public SpeechRequest(String model, String input, Voice voice) {
			this(model, input, voice, ResponseFormat.mp3, 1.0f);
		}

		/**
		 * The voice to use for synthesis.
		 */
		public enum Voice {
			/**
			 * alloy voice
			 */
			alloy,
			/**
			 * echo voice
			 */
			echo,
			/**
			 * fable voice
			 */
			fable,
			/**
			 * onyx voice
			 */
			onyx,
			/**
			 * nova voice
			 */
			nova,
			/**
			 * shimmer voice
			 */
			shimmer
		}

		/**
		 * The format to audio in. Supported formats are mp3, opus, aac, and flac. Defaults to mp3.
		 */
		public enum ResponseFormat {
			/**
			 * mp3 audio format.
			 */
			mp3,
			/**
			 * opus audio format.
			 */
			opus,
			/**
			 * aac audio format.
			 */
			aac,
			/**
			 * flac audio format.
			 */
			flac
		}
	}

	/**
	 * Request to transcribe an audio file to text.
	 *
	 * @param file The audio file to transcribe. Must be a valid audio file type.
	 * @param model ID of the model to use. Only whisper-1 is currently available.
	 * @param language The language of the input audio. Supplying the input language in ISO-639-1 format will improve
	 * accuracy and latency.
	 * @param prompt An optional text to guide the model's style or continue a previous audio segment. The prompt should
	 * match the audio language.
	 * @param response_format The format of the transcript output, in one of these options: json, text, srt,
	 * verbose_json, or vtt. Defaults to json.
	 * @param temperature The sampling temperature, between 0 and 1. Higher values like 0.8 will make the output more
	 * random, while lower values like 0.2 will make it more focused and deterministic. If set to 0, the model will use
	 * log probability to automatically increase the temperature until certain thresholds are hit.
	 */
	@JsonInclude(Include.NON_NULL)
	public record TranscriptionRequest(
			@JsonProperty("file") byte[] file,
			@JsonProperty("model") String model,
			@JsonProperty("language") String language,
			@JsonProperty("prompt") String prompt,
			@JsonProperty("response_format") TranscriptionResponseFormat response_format,
			@JsonProperty("temperature") Float temperature) {

		/**
		 * Create a new transcription request with model set to 'whisper-1', prompt and temperature defaulting to null
		 * and 0.8f respectively.
		 * @param file The audio file to transcribe. Must be a valid audio file type.
		 * @param language The language of the input audio.
		 * @param response_format The format of the transcript output.
		 */
		public TranscriptionRequest(byte[] file, String language, TranscriptionResponseFormat response_format) {
			this(file, "whisper-1", language, null, response_format, 0.8f);
		}
	}

	/**
	 * The format of the transcript output, in one of these options: json, text, srt, verbose_json, or vtt. Defaults to
	 * json.
	 */
	public enum TranscriptionResponseFormat {

		/**
		 * json format.
		 */
		json,
		/**
		 * text format.
		 */
		text,
		/**
		 * SubRip Subtitle format (srt).
		 */
		srt,
		/**
		 * verbose_json format.
		 */
		verbose_json,
		/**
		 * Web Video Text Tracks format (vtt).
		 */
		vtt;
	}

	/**
	 * Request to translate an audio file to English.
	 *
	 * @param file The audio file object (not file name) to translate, in one of these formats: flac, mp3, mp4, mpeg,
	 * mpga, m4a, ogg, wav, or webm.
	 * @param model ID of the model to use. Only whisper-1 is currently available.
	 * @param prompt An optional text to guide the model's style or continue a previous audio segment. The prompt should
	 * be in English.
	 * @param response_format The format of the transcript output, in one of these options: json, text, srt,
	 * verbose_json, or vtt.
	 * @param temperature The sampling temperature, between 0 and 1. Higher values like 0.8 will make the output more
	 * random, while lower values like 0.2 will make it more focused and deterministic. If set to 0, the model will use
	 * log probability to automatically increase the temperature until certain thresholds are hit.
	 */
	@JsonInclude(Include.NON_NULL)
	public record TranslationRequest(
			@JsonProperty("file") byte[] file,
			@JsonProperty("model") String model,
			@JsonProperty("prompt") String prompt,
			@JsonProperty("response_format") TranscriptionResponseFormat response_format,
			@JsonProperty("temperature") Float temperature) {

		/**
		 * Create a new translation request with model set to 'whisper-1', prompt defaulting to null and temperature set
		 * to 0.8f.
		 * @param file The audio file to translate.
		 * @param response_format The format of the translation output.
		 */
		public TranslationRequest(byte[] file, TranscriptionResponseFormat response_format) {
			this(file, "whisper-1", null, response_format, 0.8f);
		}
	}

	/**
	 * Request to generates audio from the input text.
	 * @param requestBody The request body.
	 * @return The audio file in bytes. You can use the {@link #saveToFile(byte[], String)} to save the audio to a file.
	 */
	public byte[] createSpeech(SpeechRequest requestBody) {
		return this.restClient.post()
				.uri("/v1/audio/speech")
				.body(requestBody)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(byte[].class);
	}

	/**
	 * Saves the audio binary into a file.
	 * @param audioContent The audio as byte array.
	 * @param fileName The file name to save the audio to.
	 */
	public static void saveToFile(byte[] audioContent, String fileName) {

		try {
			var fos = new FileOutputStream(fileName);
			StreamUtils.copy(audioContent, fos);
			fos.close();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	};

	/**
	 * Transcribes audio into the input language.
	 *
	 * @param requestBody The request body.
	 * @return The transcribed text.
	 */
	public String createTranscription(TranscriptionRequest requestBody) {

		MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
		multipartBody.add("file", new ByteArrayResource(requestBody.file()) {
			@Override
			public String getFilename() {
				return "audio.webm";
			}
		});
		multipartBody.add("model", requestBody.model());
		multipartBody.add("language", requestBody.language());
		multipartBody.add("prompt", requestBody.prompt());
		multipartBody.add("response_format", requestBody.response_format().name());
		multipartBody.add("temperature", requestBody.temperature());

		return this.restClient.post()
				.uri("/v1/audio/transcriptions")
				.body(multipartBody)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(String.class);
	}

	/**
	 * Translates audio into English.
	 *
	 * @param requestBody The request body.
	 * @return The transcribed text.
	 */
	public String createTranslation(TranslationRequest requestBody) {

		MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
		multipartBody.add("file", new ByteArrayResource(requestBody.file()) {
			@Override
			public String getFilename() {
				return "audio.webm";
			}
		});
		multipartBody.add("model", requestBody.model());
		multipartBody.add("prompt", requestBody.prompt());
		multipartBody.add("response_format", requestBody.response_format().name());
		multipartBody.add("temperature", requestBody.temperature());

		return this.restClient.post()
				.uri("/v1/audio/translations")
				.body(multipartBody)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(String.class);
	}

	/**
	 * Main method to test the audio api.
	 * @param args The command line arguments.
	 * @throws IOException In case of IO errors.
	 */
	public static void main(String[] args) throws IOException {

		var audioApi = new AudioApi(System.getenv("OPENAI_API_KEY"));

		byte[] mp3 = audioApi
				.createSpeech(new SpeechRequest("tts-1", "Spring AI rocks!", SpeechRequest.Voice.echo));

		System.out.println(mp3.length);

		saveToFile(mp3, "test.mp3");

		var text = audioApi.createTranscription(
				new TranscriptionRequest(mp3, "en", TranscriptionResponseFormat.text));

		System.out.println(text);

		text = audioApi.createTranslation(
				new TranslationRequest(mp3, TranscriptionResponseFormat.text));

		System.out.println(text);

	}
}
