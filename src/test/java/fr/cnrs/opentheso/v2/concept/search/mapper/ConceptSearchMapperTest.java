package fr.cnrs.opentheso.v2.concept.search.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptSearchMapperTest {

    @Test
    void isDeprecatedStatus_delegatesToPolicy() {
        assertTrue(ConceptSearchMapper.isDeprecatedStatus("dep"));
    }
}
