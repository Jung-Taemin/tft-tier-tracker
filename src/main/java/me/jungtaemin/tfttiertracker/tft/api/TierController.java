package me.jungtaemin.tfttiertracker.tft.api;

import lombok.RequiredArgsConstructor;
import me.jungtaemin.tfttiertracker.infra.riot.RiotTftClient;
import me.jungtaemin.tfttiertracker.tft.api.dto.CreateUserRequest;
import me.jungtaemin.tfttiertracker.tft.api.dto.UserResponse;
import me.jungtaemin.tfttiertracker.tft.domain.TierSnapshot;
import me.jungtaemin.tfttiertracker.tft.domain.User;
import me.jungtaemin.tfttiertracker.tft.repository.TierSnapshotRepository;
import me.jungtaemin.tfttiertracker.tft.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TierController {

    private final UserRepository userRepo;
    private final TierSnapshotRepository snapshotRepo;
    private final RiotTftClient riot;

    @PostMapping("/users")
    @Transactional
    public Map<String, Object> createUser(@RequestBody CreateUserRequest req) {
        User user = userRepo.findBySummonerName(req.summonerName())
                .orElseGet(() -> userRepo.save(
                        User.builder()
                                .summonerName(req.summonerName())
                                .region(Optional.ofNullable(req.region()).orElse("kr"))
                                .build()
                ));

        // Summoner 정보 동기화
        Map<String, Object> summoner = riot.getSummonerByName(user.getSummonerName()).block();
        if (summoner == null || summoner.get("id") == null) {
            throw new IllegalArgumentException("소환사 정보를 찾을 수 없습니다: " + user.getSummonerName());
        }

        user = User.builder()
                .id(user.getId())
                .summonerName(user.getSummonerName())
                .region(user.getRegion())
                .puuid((String) summoner.get("puuid"))
                .summonerId((String) summoner.get("id"))
                .build();
        userRepo.save(user);

        return Map.of("id", user.getId(), "summonerName", user.getSummonerName());
    }

    @GetMapping("/tier/current")
    @Transactional(readOnly = true)
    public List<UserResponse> currentTier() {
        List<User> users = userRepo.findAll();
        List<UserResponse> result = new ArrayList<>();

        for (User u : users) {
            // 최근 스냅샷 1건 (없으면 실시간 조회)
            List<TierSnapshot> latest = snapshotRepo.findTop1ByUserOrderByCheckedAtDesc(u);
            String tier = null, rank = null; Integer lp = null;
            if (!latest.isEmpty()) {
                TierSnapshot s = latest.get(0);
                tier = s.getTier(); rank = s.getRankDivision(); lp = s.getLeaguePoints();
            }
            result.add(new UserResponse(u.getId(), u.getSummonerName(), tier, rank, lp));
        }
        return result;
    }

    @PostMapping("/tier/refresh")
    @Transactional
    public Map<String, Object> refreshNow() {
        List<User> users = userRepo.findAll();
        int updated = 0;

        for (User u : users) {
            if (u.getSummonerId() == null) continue;
            List<Map<String, Object>> leagues = riot.getLeagueBySummonerId(u.getSummonerId()).blockOptional().orElse(List.of());
            // TFT 랭크만 찾기
            Map<String, Object> ranked = leagues.stream()
                    .filter(m -> "RANKED_TFT".equals(m.get("queueType")))
                    .findFirst().orElse(null);

            if (ranked != null) {
                TierSnapshot s = TierSnapshot.builder()
                        .user(u)
                        .queueType((String) ranked.get("queueType"))
                        .tier((String) ranked.get("tier"))
                        .rankDivision((String) ranked.get("rank"))
                        .leaguePoints(((Number) ranked.getOrDefault("leaguePoints", 0)).intValue())
                        .wins(((Number) ranked.getOrDefault("wins", 0)).intValue())
                        .losses(((Number) ranked.getOrDefault("losses", 0)).intValue())
                        .checkedAt(LocalDateTime.now())
                        .build();
                snapshotRepo.save(s);
                updated++;
            }
        }
        return Map.of("updated", updated, "at", LocalDateTime.now().toString());
    }
}