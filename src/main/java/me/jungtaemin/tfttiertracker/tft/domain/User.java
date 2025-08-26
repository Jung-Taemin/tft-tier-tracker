package me.jungtaemin.tfttiertracker.tft.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String summonerName;

    @Column(length=100)
    private String puuid;

    @Column(length=100)
    private String summonerId;

    @Column(length=20)
    private String region;
}
