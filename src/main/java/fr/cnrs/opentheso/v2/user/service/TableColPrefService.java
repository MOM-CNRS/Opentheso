package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.persistence.UserTableColPrefEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserTableColPrefRepository;
import fr.cnrs.opentheso.v2.user.model.TableColIds;
import fr.cnrs.opentheso.v2.user.model.TableColPref;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TableColPrefService {

    private final UserTableColPrefRepository repository;

    @Transactional(readOnly = true)
    public TableColPref getPref(int userId) {
        List<UserTableColPrefEntity> rows = repository.findByUserId(userId);
        if (rows.isEmpty()) {
            return TableColPref.defaults();
        }
        Set<String> selected = new LinkedHashSet<>();
        for (UserTableColPrefEntity row : rows) {
            if (row.isSelected() && TableColIds.ALL.contains(row.getColId())) {
                selected.add(row.getColId());
            }
        }
        return new TableColPref(Set.copyOf(selected));
    }

    @Transactional
    public TableColPref savePref(int userId, Iterable<String> selected) {
        Set<String> normalized = TableColIds.normalizeSelected(selected);
        repository.deleteByUserId(userId);
        List<UserTableColPrefEntity> rows = new ArrayList<>();
        for (String colId : TableColIds.ALL) {
            rows.add(UserTableColPrefEntity.builder()
                    .userId(userId)
                    .colId(colId)
                    .selected(normalized.contains(colId))
                    .build());
        }
        repository.saveAll(rows);
        return new TableColPref(normalized);
    }

    @Transactional
    public TableColPref resetPref(int userId) {
        repository.deleteByUserId(userId);
        return TableColPref.defaults();
    }
}
