package me.jungtaemin.tfttiertracker.tft.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    private User user;

    private String queueType;
    private String tier;
    private String rankDivision;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;

    private LocalDateTime checkedAt;
}
