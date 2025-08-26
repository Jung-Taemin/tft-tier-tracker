package me.jungtaemin.tfttiertracker.tft.api.dto;

public record UserResponse(
        Long id, String summonerName, String tier, String rank, Integer lp
) {}
