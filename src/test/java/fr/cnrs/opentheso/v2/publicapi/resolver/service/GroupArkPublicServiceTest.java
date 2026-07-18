package fr.cnrs.opentheso.v2.publicapi.resolver.service;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupArkPublicServiceTest {

    @Mock
    private ConceptGroupRepository conceptGroupRepository;

    private GroupArkPublicService service;

    @BeforeEach
    void setUp() {
        service = new GroupArkPublicService(conceptGroupRepository);
    }

    @Test
    void resolveGroupByArk_returnsResolvedGroup() {
        when(conceptGroupRepository.findThesaurusIdByArkId("naan/ark1")).thenReturn("TH1");
        when(conceptGroupRepository.findAllByIdThesaurusAndIdArk("TH1", "naan/ark1"))
                .thenReturn(Optional.of(ConceptGroup.builder().idGroup("G1").build()));

        var response = service.resolveGroupByArk("naan", "ark1");

        assertEquals("TH1", response.thesaurusId());
        assertEquals("G1", response.groupId());
    }

    @Test
    void resolveGroupByArk_throwsWhenThesaurusNotFound() {
        when(conceptGroupRepository.findThesaurusIdByArkId("naan/ark9")).thenReturn(null);

        assertThrows(PublicResourceNotFoundException.class, () -> service.resolveGroupByArk("naan", "ark9"));
    }

    @Test
    void resolveGroupByArk_throwsWhenGroupNotFound() {
        when(conceptGroupRepository.findThesaurusIdByArkId("naan/ark2")).thenReturn("TH1");
        when(conceptGroupRepository.findAllByIdThesaurusAndIdArk("TH1", "naan/ark2")).thenReturn(Optional.empty());

        assertThrows(PublicResourceNotFoundException.class, () -> service.resolveGroupByArk("naan", "ark2"));
    }
}
