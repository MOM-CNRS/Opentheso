package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.mapper.SettingMapper;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.shared.persistence.CorpusLinkEntity;
import fr.cnrs.opentheso.v2.shared.repository.CorpusLinkJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThesaurusCorpusService {

    private final CorpusLinkJpaRepository corpusLinkJpaRepository;

    @Transactional(readOnly = true)
    public List<ThesaurusCorpus> listCorpus(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        return corpusLinkJpaRepository.findAllByIdThesaurusOrderBySortAsc(thesaurusId).stream()
                .map(SettingMapper::toCorpus)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveCorpus(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return corpusLinkJpaRepository.findAllByIdThesaurusOrderBySortAsc(thesaurusId).stream()
                .anyMatch(CorpusLinkEntity::isActive);
    }

    @Transactional
    public ThesaurusCorpus createCorpus(String thesaurusId, ThesaurusCorpus corpus) {
        validateCorpus(corpus);
        if (corpusLinkJpaRepository.findByIdThesaurusAndCorpusName(thesaurusId, corpus.corpusName()).isPresent()) {
            throw new InvalidSettingDataException("Ce corpus existe déjà.");
        }
        CorpusLinkEntity saved = corpusLinkJpaRepository.save(SettingMapper.toCorpusEntity(thesaurusId, corpus));
        log.info("Corpus {} créé pour le thésaurus {}", corpus.corpusName(), thesaurusId);
        return SettingMapper.toCorpus(saved);
    }

    @Transactional
    public ThesaurusCorpus updateCorpus(String thesaurusId, String currentName, ThesaurusCorpus corpus) {
        validateCorpus(corpus);
        CorpusLinkEntity existing = corpusLinkJpaRepository.findByIdThesaurusAndCorpusName(thesaurusId, currentName)
                .orElseThrow(() -> new InvalidSettingDataException("Corpus introuvable : " + currentName));

        if (!currentName.equalsIgnoreCase(corpus.corpusName())
                && corpusLinkJpaRepository.findByIdThesaurusAndCorpusName(thesaurusId, corpus.corpusName()).isPresent()) {
            throw new InvalidSettingDataException("Ce corpus existe déjà.");
        }

        if (!currentName.equals(corpus.corpusName())) {
            corpusLinkJpaRepository.updateCorpusName(corpus.corpusName(), currentName, thesaurusId);
            existing = corpusLinkJpaRepository.findByIdThesaurusAndCorpusName(thesaurusId, corpus.corpusName())
                    .orElseThrow(() -> new InvalidSettingDataException("Corpus introuvable après renommage."));
        }

        SettingMapper.applyCorpus(existing, corpus);
        CorpusLinkEntity saved = corpusLinkJpaRepository.save(existing);
        log.info("Corpus {} mis à jour pour le thésaurus {}", corpus.corpusName(), thesaurusId);
        return SettingMapper.toCorpus(saved);
    }

    @Transactional
    public void deleteCorpus(String thesaurusId, String corpusName) {
        if (corpusLinkJpaRepository.findByIdThesaurusAndCorpusName(thesaurusId, corpusName).isEmpty()) {
            throw new InvalidSettingDataException("Corpus introuvable : " + corpusName);
        }
        corpusLinkJpaRepository.deleteByIdThesaurusAndCorpusName(thesaurusId, corpusName);
        log.info("Corpus {} supprimé pour le thésaurus {}", corpusName, thesaurusId);
    }

    private void validateCorpus(ThesaurusCorpus corpus) {
        if (corpus == null || StringUtils.isBlank(corpus.corpusName())) {
            throw new InvalidSettingDataException("Le nom du corpus est obligatoire.");
        }
        if (StringUtils.isBlank(corpus.uriLink())) {
            throw new InvalidSettingDataException("L'URI du lien est obligatoire.");
        }
        if (!corpus.onlyUriLink() && StringUtils.isBlank(corpus.uriCount())) {
            throw new InvalidSettingDataException("L'URI pour le comptage est obligatoire.");
        }
    }
}
