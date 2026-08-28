package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.persistence.UserConceptBlockPrefEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserConceptBlockPrefRepository;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockIds;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConceptBlockLayoutService {

    private final UserConceptBlockPrefRepository repository;

    @Transactional(readOnly = true)
    public ConceptBlockLayout getLayout(int userId) {
        List<UserConceptBlockPrefEntity> rows = repository.findByUserIdOrderByPositionAsc(userId);
        if (rows.isEmpty()) {
            return ConceptBlockLayout.defaults();
        }
        List<String> storedOrder = rows.stream().map(UserConceptBlockPrefEntity::getBlockId).toList();
        Set<String> storedCollapsed = new LinkedHashSet<>();
        for (UserConceptBlockPrefEntity row : rows) {
            if (row.isCollapsed()) {
                storedCollapsed.add(row.getBlockId());
            }
        }
        return normalize(storedOrder, storedCollapsed);
    }

    @Transactional
    public ConceptBlockLayout saveLayout(int userId, List<String> order, List<String> collapsed) {
        ConceptBlockLayout layout = normalize(order, collapsed);
        repository.deleteByUserId(userId);
        int position = 0;
        List<UserConceptBlockPrefEntity> rows = new ArrayList<>();
        for (String blockId : layout.order()) {
            rows.add(UserConceptBlockPrefEntity.builder()
                    .userId(userId)
                    .blockId(blockId)
                    .position(position++)
                    .collapsed(layout.collapsed().contains(blockId))
                    .build());
        }
        repository.saveAll(rows);
        return layout;
    }

    @Transactional
    public ConceptBlockLayout resetLayout(int userId) {
        repository.deleteByUserId(userId);
        return ConceptBlockLayout.defaults();
    }

    public ConceptBlockLayout normalize(List<String> order, Iterable<String> collapsed) {
        LinkedHashSet<String> normalizedOrder = new LinkedHashSet<>();
        if (order != null) {
            for (String id : order) {
                if (id != null && ConceptBlockIds.ALL.contains(id)) {
                    normalizedOrder.add(id);
                }
            }
        }
        for (String id : ConceptBlockIds.DEFAULT_ORDER) {
            normalizedOrder.add(id);
        }
        Set<String> normalizedCollapsed = new LinkedHashSet<>();
        if (collapsed != null) {
            for (String id : collapsed) {
                if (id != null && ConceptBlockIds.ALL.contains(id)) {
                    normalizedCollapsed.add(id);
                }
            }
        }
        return new ConceptBlockLayout(List.copyOf(normalizedOrder), Set.copyOf(normalizedCollapsed));
    }
}
