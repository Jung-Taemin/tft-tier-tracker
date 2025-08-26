package me.jungtaemin.tfttiertracker.tft.repository;

import me.jungtaemin.tfttiertracker.tft.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySummonerName(String summonerName);
}
