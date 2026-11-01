package com.seroter.demo.weather;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WeatherDashboardController {

    private final WeatherApiService weatherApiService;

    public WeatherDashboardController(WeatherApiService weatherApiService) {
        this.weatherApiService = weatherApiService;
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "city", required = false) String city, Model model) {
        WeatherDashboardData weather = weatherApiService.fetchWeather(city);
        model.addAttribute("weather", weather);
        model.addAttribute("city", weather.city());
        return "index";
    }
}
