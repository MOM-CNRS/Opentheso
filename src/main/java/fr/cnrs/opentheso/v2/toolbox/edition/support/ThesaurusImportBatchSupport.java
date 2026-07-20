package fr.cnrs.opentheso.v2.toolbox.edition.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Batches thesaurus import writes to keep transactions short and the persistence context small.
 */
@Component
@RequiredArgsConstructor
public class ThesaurusImportBatchSupport {

    public static final int CONCEPT_BATCH_SIZE = 250;

    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    public TransactionTemplate newTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    public <T> T inTransaction(Supplier<T> work) {
        return newTemplate().execute(status -> work.get());
    }

    public void inTransaction(Runnable work) {
        newTemplate().executeWithoutResult(status -> work.run());
    }

    public <T> int forEachBatched(List<T> items, BiConsumer<List<T>, Integer> batchConsumer) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int processed = 0;
        for (int i = 0; i < items.size(); i += CONCEPT_BATCH_SIZE) {
            int end = Math.min(i + CONCEPT_BATCH_SIZE, items.size());
            List<T> batch = items.subList(i, end);
            inTransaction(() -> {
                batchConsumer.accept(batch, batch.size());
                entityManager.flush();
                entityManager.clear();
            });
            processed += batch.size();
        }
        return processed;
    }

    public void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
