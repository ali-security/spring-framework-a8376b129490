/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.accept;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A test fixture with a test subclass of AbstractMappingContentNegotiationStrategy.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
class MappingContentNegotiationStrategyTests {

	@Test
	void resolveMediaTypes() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("json", mapping);

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

		assertThat(mediaTypes).hasSize(1);
		assertThat(mediaTypes.get(0).toString()).isEqualTo("application/json");
	}

	@Test
	void resolveMediaTypesNoMatch() throws Exception {
		Map<String, MediaType> mapping = null;
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("blah", mapping);

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

		assertThat(mediaTypes).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
	}

	@Test
	void resolveMediaTypesNoKey() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy(null, mapping);

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

		assertThat(mediaTypes).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
	}

	@Test
	void resolveMediaTypesHandleNoMatch() throws Exception {
		Map<String, MediaType> mapping = null;
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("xml", mapping);

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

		assertThat(mediaTypes).hasSize(1);
		assertThat(mediaTypes.get(0).toString()).isEqualTo("application/xml");
	}

	@Test
	void resolveMediaTypesHandleNoMatchDoesNotAddKeyWithDot() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("not.xml", mapping);

		assertThat(strategy.resolveMediaTypes(null)).containsExactly(MediaType.APPLICATION_XML);
		assertThat(strategy.getMediaTypes()).containsOnlyKeys("json");
		assertThat(strategy.getAllFileExtensions()).containsExactly("json");
		assertThat(strategy.resolveFileExtensions(MediaType.APPLICATION_XML)).isEmpty();
	}

	@Test
	void resolveMediaTypesHandleNoMatchAddsLowerCaseKeyOnce() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("XML", mapping);

		assertThat(strategy.resolveMediaTypes(null)).containsExactly(MediaType.APPLICATION_XML);
		assertThat(strategy.resolveMediaTypeKey(null, "Xml")).containsExactly(MediaType.APPLICATION_XML);
		assertThat(strategy.getMediaTypes()).containsOnlyKeys("json", "xml");
		assertThat(strategy.getAllFileExtensions()).containsExactly("json", "xml");
		assertThat(strategy.resolveFileExtensions(MediaType.APPLICATION_XML)).containsExactly("xml");
	}

	@Test
	void resolveMediaTypesViaMediaTypeFactoryDoesNotGrowMappings() throws Exception {
		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(mapping);
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		NativeWebRequest webRequest = new ServletWebRequest(servletRequest);

		for (int i = 0; i < 100; i++) {
			servletRequest.setParameter("format", "a" + i + ".json");
			assertThat(strategy.resolveMediaTypes(webRequest)).containsExactly(MediaType.APPLICATION_JSON);
			servletRequest.setParameter("format", "a" + i + ".xml");
			assertThat(strategy.resolveMediaTypes(webRequest)).containsExactly(MediaType.APPLICATION_XML);
		}
		for (String key : Arrays.asList("XML", "Xml", "xMl", "xmL", "xml")) {
			servletRequest.setParameter("format", key);
			assertThat(strategy.resolveMediaTypes(webRequest)).containsExactly(MediaType.APPLICATION_XML);
		}

		assertThat(strategy.getMediaTypes()).containsOnlyKeys("json", "xml");
		assertThat(strategy.getAllFileExtensions()).containsExactly("json", "xml");
		assertThat(strategy.resolveFileExtensions(MediaType.APPLICATION_JSON)).containsExactly("json");
		assertThat(strategy.resolveFileExtensions(MediaType.APPLICATION_XML)).containsExactly("xml");
	}


	private static class TestMappingContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

		private final String extension;

		public TestMappingContentNegotiationStrategy(String extension, Map<String, MediaType> mapping) {
			super(mapping);
			this.extension = extension;
		}

		@Override
		protected String getMediaTypeKey(NativeWebRequest request) {
			return this.extension;
		}

		@Override
		protected MediaType handleNoMatch(NativeWebRequest request, String mappingKey) {
			// Like MediaTypeFactory, resolve by the last (case-insensitive) extension only
			String key = mappingKey.toLowerCase(Locale.ROOT);
			return (key.equals("xml") || key.endsWith(".xml") ? MediaType.APPLICATION_XML : null);
		}
	}

}
