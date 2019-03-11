package com.ogb.fes;


import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;


@EnableWebMvc
@Configuration
public class FesSpringConfiguration extends WebMvcConfigurerAdapter {

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	    converters.add(stringConverter());
	    converters.add(mappingJackson2HttpMessageConverter());
	    super.configureMessageConverters(converters);
	}

	@Bean
	public StringHttpMessageConverter stringConverter() {
	    final StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
	    stringConverter.setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON));
	    return stringConverter;
	}

	@Bean
	public GenericHttpMessageConverter<Object> mappingJackson2HttpMessageConverter() {
	   
	    return new MappingJackson2HttpMessageConverter(new ObjectMapper());
	}
}

