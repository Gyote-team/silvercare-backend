package com.gyote.silvercare.medical_document.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** 로컬 개발 저장소의 문서·렌더링 페이지를 브라우저에서 표시한다. */
@Configuration
public class LocalDocumentResourceConfig implements WebMvcConfigurer {

    private final String resourceLocation;

    public LocalDocumentResourceConfig(
            @Value("${silvercare.storage.local-root:./data/object-storage}") String localRoot
    ) {
        this.resourceLocation = Path.of(localRoot).toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/local-documents/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(0);
    }
}
