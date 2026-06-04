package kz.halyk.maqsat.travel.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import kz.halyk.maqsat.travel.entities.TravelOffer;
import org.springframework.stereotype.Repository;

@Repository
public class TravelOfferNativeRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<TravelOffer> findActiveMatchingAudience(String[] tags) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(tags[i].replace("\"", "\\\""));
        }
        sb.append("}");
        return em.createNativeQuery(
                        "SELECT * FROM travel_offer WHERE audience_tags ?| CAST(:tags AS text[]) AND valid_until > now()",
                        TravelOffer.class)
                .setParameter("tags", sb.toString())
                .getResultList();
    }
}
