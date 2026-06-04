package kz.halyk.maqsat.alser.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import kz.halyk.maqsat.alser.entities.DeviceOffer;

public class DeviceOfferRepositoryImpl implements DeviceOfferRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<DeviceOffer> findActiveMatchingAudience(String[] tags) {
        StringBuilder arrayLiteral = new StringBuilder("ARRAY[");
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) arrayLiteral.append(',');
            arrayLiteral.append('\'').append(tags[i].replace("'", "''")).append('\'');
        }
        arrayLiteral.append(']');
        String sql = "SELECT * FROM device_offer WHERE audience_tags ?| " + arrayLiteral + " AND valid_until > now()";
        return em.createNativeQuery(sql, DeviceOffer.class).getResultList();
    }
}
