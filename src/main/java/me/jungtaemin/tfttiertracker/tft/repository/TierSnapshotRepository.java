package me.jungtaemin.tfttiertracker.tft.repository;

import me.jungtaemin.tfttiertracker.tft.domain.TierSnapshot;
import me.jungtaemin.tfttiertracker.tft.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TierSnapshotRepository extends JpaRepository<TierSnapshot, Long> {
    List<TierSnapshot> findTop1ByUserOrderByCheckedAtDesc(User user);
}
