package kz.halyk.maqsat.recommendation.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kz.halyk.maqsat.recommendation.client.AnalyticsClient;
import kz.halyk.maqsat.recommendation.client.AnalyticsWriteClient;
import kz.halyk.maqsat.recommendation.client.AlserClient;
import kz.halyk.maqsat.recommendation.client.DeviceOfferDto;
import kz.halyk.maqsat.recommendation.client.GoalDto;
import kz.halyk.maqsat.recommendation.client.GoalsClient;
import kz.halyk.maqsat.recommendation.client.HalykTravelClient;
import kz.halyk.maqsat.recommendation.client.OpenAiClient;
import kz.halyk.maqsat.recommendation.client.TravelOfferDto;
import kz.halyk.maqsat.recommendation.client.UserMetricsDto;
import kz.halyk.maqsat.recommendation.config.OpenAiProperties;
import kz.halyk.maqsat.recommendation.dto.res.MatchedOffer;
import kz.halyk.maqsat.recommendation.dto.res.RecommendationResult;
import kz.halyk.maqsat.recommendation.services.AudienceTagger;
import kz.halyk.maqsat.recommendation.services.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final AnalyticsClient analytics;
    private final GoalsClient goals;
    private final AlserClient alser;
    private final HalykTravelClient travel;
    private final AnalyticsWriteClient writer;
    private final AudienceTagger tagger;
    private final OpenAiProperties props;
    private final OpenAiClient openAi;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;

    @Override
    public RecommendationResult recommend(String userId, String period) {
        UserMetricsDto metrics = analytics.fetchMetrics(userId, period);
        List<GoalDto> userGoals = goals.fetchGoals(userId);
        Set<String> tags = tagger.tag(metrics, userGoals);
        List<DeviceOfferDto> alserOffers = alser.fetchOffers(tags);
        List<TravelOfferDto> travelOffers = travel.fetchOffers(tags);

        List<MatchedOffer> matched = props.enabled()
                ? rankWithLlm(userId, period, metrics, userGoals, tags, alserOffers, travelOffers)
                : rankByOverlap(tags, alserOffers, travelOffers);

        matched = matched.stream().limit(5).toList();
        writer.write(userId, period, matched);
        for (var m : matched) {
            meterRegistry.counter("maqsat.recommendations", "partner", m.partner()).increment();
        }
        return new RecommendationResult(userId, period, matched, Instant.now());
    }

    List<MatchedOffer> rankByOverlap(Set<String> userTags,
                                     List<DeviceOfferDto> alserOffers,
                                     List<TravelOfferDto> travelOffers) {
        List<MatchedOffer> out = new ArrayList<>();

        for (var o : alserOffers) {
            List<String> offerTags = o.audienceTags() != null ? o.audienceTags() : Collections.emptyList();
            int overlap = (int) offerTags.stream().filter(userTags::contains).count();
            BigDecimal score = userTags.isEmpty() ? BigDecimal.ZERO
                    : new BigDecimal(overlap).divide(new BigDecimal(userTags.size()), 3, RoundingMode.HALF_UP);
            out.add(new MatchedOffer(o.id().toString(), "ALSER", score, offerTags,
                    "overlap=" + overlap + " tags"));
        }

        for (var o : travelOffers) {
            List<String> offerTags = o.audienceTags() != null ? o.audienceTags() : Collections.emptyList();
            int overlap = (int) offerTags.stream().filter(userTags::contains).count();
            BigDecimal score = userTags.isEmpty() ? BigDecimal.ZERO
                    : new BigDecimal(overlap).divide(new BigDecimal(userTags.size()), 3, RoundingMode.HALF_UP);
            out.add(new MatchedOffer(o.id().toString(), "HALYK_TRAVEL", score, offerTags,
                    "overlap=" + overlap + " tags"));
        }

        out.sort(Comparator.comparing(MatchedOffer::score).reversed());
        return out;
    }

    private List<MatchedOffer> rankWithLlm(String userId, String period,
                                            UserMetricsDto metrics,
                                            List<GoalDto> userGoals,
                                            Set<String> tags,
                                            List<DeviceOfferDto> alserOffers,
                                            List<TravelOfferDto> travelOffers) {
        String system = """
                You are an offer-matching ML model emulated as an LLM.
                Given a user's audience tags and partner offers (alser devices, halyk-travel destinations),
                output STRICT JSON {"matches":[{"offerId":"...","partner":"ALSER|HALYK_TRAVEL","score":0.0..1.0,"rationale":"..."}]}.
                Sort by score descending. Keep at most 5 entries. No markdown.
                """;
        try {
            String userPrompt = mapper.writeValueAsString(Map.of(
                    "userId", userId,
                    "period", period,
                    "tags", tags,
                    "metrics", metrics != null ? metrics : Map.of(),
                    "goals", userGoals,
                    "alserOffers", alserOffers,
                    "travelOffers", travelOffers));
            String raw = openAi.complete(system, List.of(new OpenAiClient.ChatMessage("user", userPrompt)));
            JsonNode root = mapper.readTree(raw);
            List<MatchedOffer> out = new ArrayList<>();
            for (JsonNode m : root.path("matches")) {
                String offerId = m.path("offerId").asText();
                String partner = m.path("partner").asText();
                BigDecimal score = new BigDecimal(m.path("score").asText("0"));
                String rationale = m.path("rationale").asText("");
                List<String> tagList = findAudienceTags(offerId, alserOffers, travelOffers);
                out.add(new MatchedOffer(offerId, partner, score, tagList, rationale));
            }
            meterRegistry.counter("maqsat.openai.calls", "service", "recommendation", "outcome", "ok").increment();
            return out;
        } catch (Exception e) {
            meterRegistry.counter("maqsat.openai.calls", "service", "recommendation", "outcome", "error").increment();
            log.warn("LLM rank failed, falling back to overlap: {}", e.getMessage());
            return rankByOverlap(tags, alserOffers, travelOffers);
        }
    }

    private List<String> findAudienceTags(String offerId,
                                           List<DeviceOfferDto> alserOffers,
                                           List<TravelOfferDto> travelOffers) {
        for (var o : alserOffers) {
            if (o.id() != null && o.id().toString().equals(offerId)) {
                return o.audienceTags() != null ? o.audienceTags() : Collections.emptyList();
            }
        }
        for (var o : travelOffers) {
            if (o.id() != null && o.id().toString().equals(offerId)) {
                return o.audienceTags() != null ? o.audienceTags() : Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
