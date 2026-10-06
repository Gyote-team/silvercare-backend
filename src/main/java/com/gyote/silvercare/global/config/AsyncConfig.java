package com.gyote.silvercare.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** @Async 메서드가 비동기로 실행되도록 켜는 설정입니다. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
