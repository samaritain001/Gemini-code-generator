package com.seroter.demo.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WeatherApiService {

    private final RestClient restClient;
    private final String apiKey;
    private final String defaultCity;

    public WeatherApiService(@Value("${weather.api.key:}") String apiKey,
                            @Value("${weather.default-city:Paris}") String defaultCity) {
        this.apiKey = apiKey;
        this.defaultCity = defaultCity;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openweathermap.org/data/2.5")
                .build();
    }

    public WeatherDashboardData fetchWeather(String requestedCity) {
        String city = (requestedCity == null || requestedCity.isBlank()) ? defaultCity : requestedCity.trim();

        if (apiKey == null || apiKey.isBlank()) {
            return fallbackWeather(city, "Using demo data because no OpenWeatherMap API key was configured.");
        }

        try {
            WeatherCurrentResponse currentResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weather")
                            .queryParam("q", city)
                            .queryParam("units", "metric")
                            .queryParam("appid", apiKey)
                            .build())
                    .retrieve()
                    .body(WeatherCurrentResponse.class);

            if (currentResponse == null) {
                return fallbackWeather(city, "No data returned for the requested city. Showing demo values instead.");
            }

            WeatherForecastResponse forecastResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("q", city)
                            .queryParam("units", "metric")
                            .queryParam("cnt", 40)
                            .queryParam("appid", apiKey)
                            .build())
                    .retrieve()
                    .body(WeatherForecastResponse.class);

            return mapToDashboard(city, currentResponse, forecastResponse);
        } catch (Exception ex) {
            return fallbackWeather(city, "API request failed. Showing demo weather while the service is unavailable.");
        }
    }

    private WeatherDashboardData mapToDashboard(String city,
                                                WeatherCurrentResponse current,
                                                WeatherForecastResponse forecast) {
        String condition = "Clear";
        String icon = "01d";
        if (current.weather() != null && current.weather().length > 0) {
            condition = current.weather()[0].main();
            icon = current.weather()[0].icon();
        }

        List<WeatherForecastPoint> forecastPoints = List.of();
        if (forecast != null && forecast.list() != null) {
            forecastPoints = forecast.list().stream()
                    .limit(5)
                    .map(item -> {
                        String itemCondition = item.weather() != null && item.weather().length > 0 ? item.weather()[0].main() : "Clear";
                        String itemIcon = item.weather() != null && item.weather().length > 0 ? item.weather()[0].icon() : "01d";
                        LocalDateTime dateTime = Instant.ofEpochSecond(item.dt())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();
                        return new WeatherForecastPoint(
                                dateTime.toLocalDate(),
                                dateTime.format(DateTimeFormatter.ofPattern("EEE")),
                                itemCondition,
                                itemIcon,
                                item.main() != null ? item.main().temp() : 0.0
                        );
                    })
                    .toList();
        }

        return new WeatherDashboardData(
                city,
                condition,
                icon,
                current.main() != null ? current.main().temp() : 0.0,
                current.main() != null ? current.main().feelsLike() : 0.0,
                current.main() != null ? current.main().humidity() : 0,
                current.wind() != null ? current.wind().speed() : 0.0,
                current.sys() != null ? current.sys().sunrise() : 0L,
                current.sys() != null ? current.sys().sunset() : 0L,
                forecastPoints,
                "Live weather from OpenWeatherMap"
        );
    }

    private WeatherDashboardData fallbackWeather(String city, String statusMessage) {
        return new WeatherDashboardData(
                city,
                "Clear",
                "01d",
                24.0,
                25.1,
                52,
                8.4,
                System.currentTimeMillis() / 1000L - 3600,
                System.currentTimeMillis() / 1000L + 3600,
                List.of(
                        new WeatherForecastPoint(LocalDate.now().plusDays(1), "Mon", "Clouds", "02d", 23.0),
                        new WeatherForecastPoint(LocalDate.now().plusDays(2), "Tue", "Rain", "10d", 20.0),
                        new WeatherForecastPoint(LocalDate.now().plusDays(3), "Wed", "Sun", "01d", 26.0),
                        new WeatherForecastPoint(LocalDate.now().plusDays(4), "Thu", "Clouds", "03d", 22.0),
                        new WeatherForecastPoint(LocalDate.now().plusDays(5), "Fri", "Rain", "10d", 19.0)
                ),
                statusMessage
        );
    }
}

record WeatherDashboardData(String city,
                           String condition,
                           String icon,
                           double temperature,
                           double feelsLike,
                           int humidity,
                           double windSpeed,
                           long sunrise,
                           long sunset,
                           List<WeatherForecastPoint> forecast,
                           String statusMessage) {}

record WeatherForecastPoint(LocalDate date, String dayLabel, String condition, String icon, double temperature) {}

record WeatherCurrentResponse(WeatherMain main, WeatherWind wind, WeatherSystem sys, WeatherCondition[] weather, String name) {}

record WeatherForecastResponse(List<WeatherForecastItem> list) {}

record WeatherForecastItem(long dt, WeatherMain main, WeatherCondition[] weather) {}

record WeatherMain(double temp, double feelsLike, int humidity) {
    public double temp() { return temp; }
    public double feelsLike() { return feelsLike; }
    public int humidity() { return humidity; }
}

record WeatherWind(double speed) {}

record WeatherSystem(long sunrise, long sunset) {}

record WeatherCondition(String main, String description, String icon) {}
