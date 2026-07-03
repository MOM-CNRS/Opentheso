package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.v2.candidat.model.CandidatImportLanguage;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatLanguageService {

    private final EditionQueryRepository editionQueryRepository;

    @Transactional(readOnly = true)
    public List<CandidatImportLanguage> listAllLanguages() {
        return editionQueryRepository.findAllLanguages().stream()
                .map(row -> new CandidatImportLanguage(row.code(), row.frenchName(), row.englishName()))
                .toList();
    }
}
