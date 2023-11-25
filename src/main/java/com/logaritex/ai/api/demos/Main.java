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

import java.io.IOException;

import com.logaritex.ai.api.Data;
import com.logaritex.ai.api.FileApi;
import com.logaritex.ai.api.Data.DataList;
import com.logaritex.ai.api.Data.File;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Files are used to upload documents that can be used with features like Assistants and Fine-tuning.
 *
 * https://platform.openai.com/docs/api-reference
 * @author Christian Tzolov
 */
public class Main {


	public static void main(String[] args) throws IOException {

		FileApi fileApi = new FileApi(System.getenv("OPENAI_API_KEY"));

		DataList<File> files = fileApi.listFiles(File.Purpose.ASSISTANTS);

		System.out.println(files);

		Resource content = new DefaultResourceLoader().getResource("classpath:/text.txt");

		Data.File file = fileApi.uploadFile(content, File.Purpose.ASSISTANTS);

		System.out.println(file);

		System.out.println(fileApi.retrieveFile(file.id()));

		System.out.println(new String(fileApi.retrieveFileContent(file.id())));

		fileApi.listFiles(File.Purpose.ASSISTANTS).data().stream().forEach(f -> fileApi.deleteFile(f.id()));

		System.out.println(fileApi.listFiles(File.Purpose.ASSISTANTS).data().size());

	}

}
