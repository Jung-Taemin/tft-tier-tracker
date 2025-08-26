package me.jungtaemin.tfttiertracker.tft.application;

import lombok.RequiredArgsConstructor;
import me.jungtaemin.tfttiertracker.infra.riot.RiotTftClient;
import me.jungtaemin.tfttiertracker.tft.api.dto.UserResponse;
import me.jungtaemin.tfttiertracker.tft.domain.TierSnapshot;
import me.jungtaemin.tfttiertracker.tft.domain.User;
import me.jungtaemin.tfttiertracker.tft.repository.TierSnapshotRepository;
import me.jungtaemin.tfttiertracker.tft.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TierService {
    private final UserRepository userRepo;
    private final TierSnapshotRepository snapshotRepo;
    private final RiotTftClient riot;

    public Long registerUser(String summonerName, String region) {
        User user = userRepo.findBySummonerName(summonerName)
                .orElseGet(() -> userRepo.save(User.builder()
                        .summonerName(summonerName)
                        .region(region == null ? "kr" : region)
                        .build()));

        var summoner = riot.getSummonerByName(user.getSummonerName()).block();
        if (summoner == null || summoner.get("id") == null)
            throw new IllegalArgumentException("소환사 정보를 찾을 수 없음: " + summonerName);

        userRepo.save(User.builder()
                .id(user.getId())
                .summonerName(user.getSummonerName())
                .region(user.getRegion())
                .puuid((String) summoner.get("puuid"))
                .summonerId((String) summoner.get("id"))
                .build());
        return user.getId();
    }

    public int refreshAll() {
        int updated = 0;
        for (User u : userRepo.findAll()) {
            if (u.getSummonerId() == null) continue;
            var leagues = riot.getLeagueBySummonerId(u.getSummonerId()).blockOptional().orElse(List.of());
            var ranked = leagues.stream().filter(m -> "RANKED_TFT".equals(m.get("queueType"))).findFirst().orElse(null);
            if (ranked != null) {
                snapshotRepo.save(TierSnapshot.builder()
                        .user(u)
                        .queueType((String) ranked.get("queueType"))
                        .tier((String) ranked.get("tier"))
                        .rankDivision((String) ranked.get("rank"))
                        .leaguePoints(((Number) ranked.getOrDefault("leaguePoints", 0)).intValue())
                        .wins(((Number) ranked.getOrDefault("wins", 0)).intValue())
                        .losses(((Number) ranked.getOrDefault("losses", 0)).intValue())
                        .checkedAt(LocalDateTime.now())
                        .build());
                updated++;
            }
        }
        return updated;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> currentTier() {
        return userRepo.findAll().stream().map(u -> {
            var latest = snapshotRepo.findTop1ByUserOrderByCheckedAtDesc(u);
            String tier = null, rank = null; Integer lp = null;
            if (!latest.isEmpty()) {
                var s = latest.get(0);
                tier = s.getTier(); rank = s.getRankDivision(); lp = s.getLeaguePoints();
            }
            return new UserResponse(u.getId(), u.getSummonerName(), tier, rank, lp);
        }).toList();
    }
}
