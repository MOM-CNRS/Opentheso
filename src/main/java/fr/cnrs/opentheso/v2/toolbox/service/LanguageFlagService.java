package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.mapper.ToolboxMapper;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageFlag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LanguageFlagService {

    private final EditionQueryRepository editionQueryRepository;
    private final LanguageRepository languageRepository;

    @Transactional(readOnly = true)
    public List<LanguageFlag> listAll() {
        return editionQueryRepository.findAllLanguages().stream()
                .map(ToolboxMapper::toLanguageFlag)
                .toList();
    }

    @Transactional
    public void updateCountryCode(String iso6391, String countryCode) {
        if (StringUtils.isBlank(iso6391)) {
            throw new InvalidToolboxDataException("La langue est obligatoire");
        }
        var language = languageRepository.findByIso6391(iso6391)
                .orElseThrow(() -> new InvalidToolboxDataException("Langue introuvable"));
        language.setCodePays(StringUtils.trimToEmpty(countryCode));
        languageRepository.save(language);
    }
}
