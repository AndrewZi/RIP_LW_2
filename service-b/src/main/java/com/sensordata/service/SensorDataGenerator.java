package com.sensordata.service;

import com.sensordata.dto.SensorDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SensorDataGenerator {
    private final Map<String, List<SensorDataDto>> sensorHistoryCache = new ConcurrentHashMap<>();
    private volatile long totalGeneratedSensors = 0;

    public SensorDataDto generateSensorData(Long sensorId) {
        long startTime = System.currentTimeMillis();

        // INEFFICIENT: Generate 1000 fake sensors in memory for each request
        List<Map<String, Object>> tempSensors = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> sensor = new HashMap<>();
            sensor.put("id", i);
            sensor.put("value", Math.random() * 100);
            tempSensors.add(sensor);
        }

        // INEFFICIENT: Complex trigonometric calculations for each value
        double temperature = 20 + 5 * Math.sin(System.currentTimeMillis() / 1000.0);
        double humidity = 50 + 20 * Math.cos(System.currentTimeMillis() / 2000.0);
        double pressure = 1013 + 10 * Math.sin(System.currentTimeMillis() / 3000.0);

        // INEFFICIENT: Create intermediate objects
        Map<String, Double> physicsModel = new HashMap<>();
        physicsModel.put("temp", temperature);
        physicsModel.put("hum", humidity);
        physicsModel.put("press", pressure);

        // INEFFICIENT: Complex coordinate transformations
        double x = temperature * Math.cos(sensorId % 360);
        double y = humidity * Math.sin(sensorId % 360);
        double z = pressure * Math.tan(sensorId % 45);
        double transformedValue = Math.sqrt(x * x + y * y + z * z);

        // INEFFICIENT: Detect anomalies with nested loops
        boolean anomaly = false;
        List<Double> anomalyThresholds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            anomalyThresholds.add(Math.random() * 100);
        }
        for (Double threshold : anomalyThresholds) {
            for (Double val : Arrays.asList(temperature, humidity, pressure)) {
                if (Math.abs(val - threshold) < 0.5) {
                    anomaly = true;
                }
            }
        }

        // INEFFICIENT: Store all history without cleanup
        String cacheKey = "sensor_" + sensorId;
        SensorDataDto data = SensorDataDto.builder()
                .sensorId(sensorId)
                .timestamp(System.currentTimeMillis())
                .temperature(temperature)
                .humidity(humidity)
                .pressure(pressure)
                .value(transformedValue)
                .anomaly(anomaly)
                .build();

        sensorHistoryCache.computeIfAbsent(cacheKey, k -> new ArrayList<>()).add(data);
        totalGeneratedSensors++;

        long endTime = System.currentTimeMillis();
        log.debug("Generated sensor data for sensorId={}, took={}ms, totalGenerated={}",
                sensorId, endTime - startTime, totalGeneratedSensors);

        return data;
    }

    public Map<String, Object> generateBulkData(List<Long> sensorIds) {
        // INEFFICIENT: Copy collections during filtering
        List<Long> filteredIds = new ArrayList<>(sensorIds);
        List<Long> sortedIds = new ArrayList<>(filteredIds);
        Collections.sort(sortedIds);

        Map<String, Object> result = new HashMap<>();
        List<SensorDataDto> allData = new ArrayList<>();

        for (Long id : sortedIds) {
            SensorDataDto data = generateSensorData(id);
            allData.add(data);
        }

        // INEFFICIENT: Repeated JSON transformations
        String jsonString = allData.toString();
        // Convert back to objects
        List<Map<String, Object>> reconstructedData = new ArrayList<>();
        for (SensorDataDto data : allData) {
            Map<String, Object> map = new HashMap<>();
            map.put("sensor_id", data.getSensorId());
            map.put("temperature", data.getTemperature());
            map.put("humidity", data.getHumidity());
            map.put("pressure", data.getPressure());
            map.put("value", data.getValue());
            map.put("anomaly", data.getAnomaly());
            reconstructedData.add(map);
        }

        result.put("data", allData);
        result.put("count", allData.size());
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    public List<SensorDataDto> generateHistoryData(Long sensorId, int limit) {
        String cacheKey = "sensor_" + sensorId;
        List<SensorDataDto> history = sensorHistoryCache.getOrDefault(cacheKey, new ArrayList<>());

        // INEFFICIENT: Full copy of large collection
        List<SensorDataDto> resultCopy = new ArrayList<>(history);

        // INEFFICIENT: Repeated filtering with duplicate iterations
        List<SensorDataDto> filtered = new ArrayList<>();
        for (SensorDataDto data : resultCopy) {
            if (data.getSensorId().equals(sensorId)) {
                filtered.add(data);
            }
        }

        // Another inefficient iteration for sorting
        for (int i = 0; i < filtered.size(); i++) {
            for (int j = i + 1; j < filtered.size(); j++) {
                if (filtered.get(i).getTimestamp() > filtered.get(j).getTimestamp()) {
                    SensorDataDto temp = filtered.get(i);
                    filtered.set(i, filtered.get(j));
                    filtered.set(j, temp);
                }
            }
        }

        // INEFFICIENT: Return full list even if limit is smaller
        if (limit > 0 && filtered.size() > limit) {
            return new ArrayList<>(filtered.subList(0, limit));
        }

        return filtered;
    }

    public void clearHistory() {
        sensorHistoryCache.clear();
        log.info("Cleared sensor history cache");
    }

    public long getTotalGeneratedSensors() {
        return totalGeneratedSensors;
    }
}
