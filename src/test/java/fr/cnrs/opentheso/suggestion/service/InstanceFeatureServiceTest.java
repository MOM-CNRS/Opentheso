package fr.cnrs.opentheso.suggestion.service;

import fr.cnrs.opentheso.suggestion.model.InstanceFeatures;
import fr.cnrs.opentheso.suggestion.persistence.InstanceFeatureRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstanceFeatureServiceTest {

    @Test
    void currentReadsTheRowOnce() {
        InstanceFeatureRepository repository = mock(InstanceFeatureRepository.class);
        when(repository.find()).thenReturn(Optional.of(new InstanceFeatures(false, true, false, true)));
        InstanceFeatureService service = new InstanceFeatureService(repository);

        InstanceFeatures first = service.current();
        InstanceFeatures second = service.current();

        assertFalse(first.suggestionEnabled());
        assertFalse(first.restApiVisible());
        assertTrue(second.graphApiVisible());
        verify(repository, times(1)).find();
    }

    @Test
    void missingRowUsesDefaults() {
        InstanceFeatureRepository repository = mock(InstanceFeatureRepository.class);
        when(repository.find()).thenReturn(Optional.empty());
        InstanceFeatureService service = new InstanceFeatureService(repository);

        InstanceFeatures features = service.current();

        assertTrue(features.suggestionEnabled());
        assertTrue(features.languageVisible());
        assertTrue(features.restApiVisible());
        assertTrue(features.graphApiVisible());
    }

    @Test
    void saveReplacesTheCache() {
        InstanceFeatureRepository repository = mock(InstanceFeatureRepository.class);
        when(repository.find()).thenReturn(Optional.of(InstanceFeatures.defaults()));
        InstanceFeatureService service = new InstanceFeatureService(repository);
        service.current();

        service.save(new InstanceFeatures(false, false, true, false));

        assertFalse(service.current().suggestionEnabled());
        assertFalse(service.current().languageVisible());
        assertTrue(service.current().restApiVisible());
        assertFalse(service.current().graphApiVisible());
        verify(repository).save(new InstanceFeatures(false, false, true, false));
        verify(repository, times(1)).find();
    }

    @Test
    void databaseFailureDoesNotStick() {
        InstanceFeatureRepository repository = mock(InstanceFeatureRepository.class);
        when(repository.find()).thenThrow(new DataAccessResourceFailureException("down"));
        InstanceFeatureService service = new InstanceFeatureService(repository);

        assertTrue(service.current().suggestionEnabled());
        assertTrue(service.current().languageVisible());
        verify(repository, times(2)).find();
    }
}
