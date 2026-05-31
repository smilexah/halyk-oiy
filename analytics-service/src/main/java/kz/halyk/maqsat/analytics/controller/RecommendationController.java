package kz.halyk.maqsat.analytics.controller;

import jakarta.transaction.Transactional;
import java.util.List;
import kz.halyk.maqsat.analytics.domain.Recommendation;
import kz.halyk.maqsat.analytics.dto.RecommendationBatch;
import kz.halyk.maqsat.analytics.dto.RecommendationDto;
import kz.halyk.maqsat.analytics.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationRepository repo;

    /** Public read endpoint — used by the mobile app. */
    @GetMapping("/api/analytics/recommendations/{userId}")
    public List<RecommendationDto> get(@PathVariable String userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(RecommendationDto::from).toList();
    }

    /** Internal write endpoint — used by recommendation-service to persist a batch.
     *  Secured by the permitAll rule on /api/analytics/internal/**.
     *  Replaces all recommendations for (userId, period) atomically. */
    @PostMapping("/api/analytics/internal/recommendations")
    @Transactional
    public ResponseEntity<Void> writeBatch(@RequestBody RecommendationBatch batch) {
        repo.deleteByUserIdAndPeriod(batch.userId(), batch.period());
        batch.items().forEach(it -> {
            Recommendation r = new Recommendation();
            r.setUserId(batch.userId());
            r.setPeriod(batch.period());
            r.setOfferId(it.offerId());
            r.setPartner(it.partner());
            r.setScore(it.score());
            r.setAudienceTags(it.audienceTags());
            r.setRationale(it.rationale());
            repo.save(r);
        });
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
