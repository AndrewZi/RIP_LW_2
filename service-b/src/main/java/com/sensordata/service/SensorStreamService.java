package com.sensordata.service;

import com.sensordata.dto.SensorDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorStreamService {
    private final SensorDataGenerator sensorDataGenerator;

    public Flux<SensorDataDto> streamSensorData(Long sensorId, Integer limit) {
        log.info("Starting sensor stream for sensorId={}, limit={}", sensorId, limit);

        return Flux.interval(Duration.ofSeconds(1))
                .doOnNext(tick -> log.debug("Emitting sensor data at tick={}", tick))
                .map(tick -> sensorDataGenerator.generateSensorData(sensorId))
                .take(limit != null && limit > 0 ? limit : 10)
                .doOnComplete(() -> log.info("Sensor stream completed for sensorId={}", sensorId))
                .doOnCancel(() -> log.warn("Sensor stream cancelled for sensorId={}", sensorId))
                .doOnError(error -> log.error("Error in sensor stream for sensorId={}: {}", sensorId, error.getMessage(), error))
                .onErrorResume(error -> {
                    log.error("Recovering from error in sensor stream: {}", error.getMessage());
                    return Flux.empty();
                });
    }

    public Flux<SensorDataDto> streamMultipleSensors(Integer sensorCount, Integer limit) {
        log.info("Starting multi-sensor stream with sensorCount={}, limit={}", sensorCount, limit);

        int count = sensorCount != null && sensorCount > 0 ? sensorCount : 5;
        int totalLimit = limit != null && limit > 0 ? limit : 20;

        return Flux.range(1, count)
                .flatMap(sensorId -> streamSensorData((long) sensorId, totalLimit / count))
                .doOnComplete(() -> log.info("Multi-sensor stream completed"))
                .doOnCancel(() -> log.warn("Multi-sensor stream cancelled"))
                .doOnError(error -> log.error("Error in multi-sensor stream: {}", error.getMessage()));
    }
}
