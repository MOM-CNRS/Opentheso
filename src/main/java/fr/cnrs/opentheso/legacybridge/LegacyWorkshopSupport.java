package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.bean.importexport.ImportFileBean;
import fr.cnrs.opentheso.bean.toolbox.atelier.AtelierThesBean;
import fr.cnrs.opentheso.v2.toolbox.session.WorkshopLegacySupport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class LegacyWorkshopSupport implements WorkshopLegacySupport {

    private final ObjectProvider<ImportFileBean> importFileBeanProvider;
    private final ObjectProvider<AtelierThesBean> atelierThesBeanProvider;

    public LegacyWorkshopSupport(
            ObjectProvider<ImportFileBean> importFileBeanProvider,
            ObjectProvider<AtelierThesBean> atelierThesBeanProvider) {
        this.importFileBeanProvider = importFileBeanProvider;
        this.atelierThesBeanProvider = atelierThesBeanProvider;
    }

    @Override
    public void initAtelier() {
        atelierThesBeanProvider.getObject().init();
    }

    @Override
    public void initBulkImport() {
        importFileBeanProvider.getObject().init();
    }
}
