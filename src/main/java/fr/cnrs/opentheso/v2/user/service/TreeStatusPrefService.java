package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.persistence.UserTreeStatusPrefEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserTreeStatusPrefRepository;
import fr.cnrs.opentheso.v2.user.model.TreeStatusIds;
import fr.cnrs.opentheso.v2.user.model.TreeStatusPref;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TreeStatusPrefService {

    private final UserTreeStatusPrefRepository repository;

    @Transactional(readOnly = true)
    public TreeStatusPref getPref(int userId) {
        List<UserTreeStatusPrefEntity> rows = repository.findByUserId(userId);
        if (rows.isEmpty()) {
            return TreeStatusPref.defaults();
        }
        Set<String> selected = new LinkedHashSet<>();
        for (UserTreeStatusPrefEntity row : rows) {
            if (row.isSelected() && TreeStatusIds.ALL.contains(row.getStatusId())) {
                selected.add(row.getStatusId());
            }
        }
        return new TreeStatusPref(Set.copyOf(selected));
    }

    @Transactional
    public TreeStatusPref savePref(int userId, Iterable<String> selected) {
        Set<String> normalized = TreeStatusIds.normalizeSelected(selected);
        repository.deleteByUserId(userId);
        List<UserTreeStatusPrefEntity> rows = new ArrayList<>();
        for (String statusId : TreeStatusIds.ALL) {
            rows.add(UserTreeStatusPrefEntity.builder()
                    .userId(userId)
                    .statusId(statusId)
                    .selected(normalized.contains(statusId))
                    .build());
        }
        repository.saveAll(rows);
        return new TreeStatusPref(normalized);
    }

    @Transactional
    public TreeStatusPref resetPref(int userId) {
        repository.deleteByUserId(userId);
        return TreeStatusPref.defaults();
    }
}
