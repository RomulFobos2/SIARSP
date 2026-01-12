package com.mai.siarsp.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduleTask {

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
        };

    }

    // Запускаем метод при старте программы и затем каждый день в полночь
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanUpDirectories() {
    }

}