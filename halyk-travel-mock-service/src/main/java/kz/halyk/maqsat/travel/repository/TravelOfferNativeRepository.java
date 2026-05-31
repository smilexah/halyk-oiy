package kz.halyk.maqsat.travel.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kz.halyk.maqsat.travel.domain.TravelOffer;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Provides native Postgres-specific queries that cannot be expressed in JPQL or Spring Data
 * due to Postgres-specific operators (e.g., {@code ?|} for jsonb array containment).
 */
@Repository
public class TravelOfferNativeRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Finds active travel offers whose audience_tags array contains any of the given tags.
     * Uses the Postgres {@code ?|} jsonb operator which requires a native query.
     *
     * @param tags array of audience tag strings to match against
     * @return list of matching TravelOffer entities with valid_until in the future
     */
    @SuppressWarnings("unchecked")
    public List<TravelOffer> findActiveMatchingAudience(String[] tags) {
        // Build the text array literal for Postgres
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(tags[i].replace("\"", "\\\""));
        }
        sb.append("}");
        String tagsLiteral = sb.toString();

        return em.createNativeQuery(
                        "SELECT * FROM travel_offer WHERE audience_tags ?| CAST(:tags AS text[]) AND valid_until > now()",
                        TravelOffer.class)
                .setParameter("tags", tagsLiteral)
                .getResultList();
    }
}
