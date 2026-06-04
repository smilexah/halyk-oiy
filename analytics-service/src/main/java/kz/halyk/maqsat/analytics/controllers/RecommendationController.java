package kz.halyk.maqsat.analytics.controllers;

import jakarta.transaction.Transactional;
import java.util.List;
import kz.halyk.maqsat.analytics.dto.req.RecommendationBatch;
import kz.halyk.maqsat.analytics.dto.res.RecommendationDto;
import kz.halyk.maqsat.analytics.mappers.RecommendationMapper;
import kz.halyk.maqsat.analytics.repositories.RecommendationRepository;
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
    private final RecommendationMapper recommendationMapper;

    @GetMapping("/api/analytics/recommendations/{userId}")
    public List<RecommendationDto> get(@PathVariable String userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(recommendationMapper::toDto).toList();
    }

    @PostMapping("/api/analytics/internal/recommendations")
    @Transactional
    public ResponseEntity<Void> writeBatch(@RequestBody RecommendationBatch batch) {
        repo.deleteByUserIdAndPeriod(batch.userId(), batch.period());
        batch.items().forEach(it ->
                repo.save(recommendationMapper.toEntity(it, batch.userId(), batch.period())));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
