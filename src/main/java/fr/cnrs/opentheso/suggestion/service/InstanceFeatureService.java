package fr.cnrs.opentheso.suggestion.service;

import fr.cnrs.opentheso.suggestion.model.InstanceFeatures;
import fr.cnrs.opentheso.suggestion.persistence.InstanceFeatureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class InstanceFeatureService {

    private final InstanceFeatureRepository repository;
    private final AtomicReference<InstanceFeatures> cache = new AtomicReference<>();

    public InstanceFeatureService(InstanceFeatureRepository repository) {
        this.repository = repository;
    }

    public InstanceFeatures current() {
        InstanceFeatures cached = cache.get();
        if (cached != null) {
            return cached;
        }
        try {
            InstanceFeatures loaded = repository.find().orElseGet(InstanceFeatures::defaults);
            cache.compareAndSet(null, loaded);
            InstanceFeatures settled = cache.get();
            return settled != null ? settled : loaded;
        } catch (DataAccessException ex) {
            log.warn("Réglages d'instance indisponibles, affichage par défaut conservé", ex);
            return InstanceFeatures.defaults();
        }
    }

    public void save(InstanceFeatures features) {
        InstanceFeatures safe = features == null ? InstanceFeatures.defaults() : features;
        repository.save(safe);
        cache.set(safe);
    }
}
