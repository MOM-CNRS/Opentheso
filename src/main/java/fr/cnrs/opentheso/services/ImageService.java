package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.ImageExterne;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@AllArgsConstructor
public class ImageService {

    private final ImagesRepository imagesRepository;


    public List<NodeImage> getAllExternalImages(String idThesaurus, String idConcept) {

        var result = imagesRepository.findAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        return CollectionUtils.isNotEmpty(result)
                ? result.stream().map(element -> new NodeImage(element.getId(), element.getIdConcept(),
                        element.getIdThesaurus(), element.getImageName(), element.getImageCreator(),
                        element.getImageCopyright(), element.getExternalUri(), ""))
                .toList()
                : List.of();
    }

    public List<NodeImage> findAllByIdConceptAndIdThesaurus(String idConcept, String idThesaurus) {

        var result = imagesRepository.findAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);

        return result.stream().map(element -> NodeImage.builder()
                    .idConcept(element.getIdConcept())
                    .idThesaurus(element.getIdThesaurus())
                    .imageName(element.getImageName())
                    .copyRight(element.getImageCopyright())
                    .uri(element.getExternalUri())
                    .build()).
                toList();
    }

    public void deleteImages(String idThesaurus, String idConcept, String url) {
        if (StringUtils.isEmpty(url)) {
            imagesRepository.deleteAllByIdThesaurusAndIdConcept(idThesaurus, idConcept);
        } else {
            imagesRepository.deleteByIdThesaurusAndIdConceptAndExternalUri(idThesaurus, idConcept, url);
        }
    }

    public void updateExternalImage(String idConcept, String idThesaurus, NodeImage nodeImage) {

        var image = ImageExterne.builder()
                .id(nodeImage.getId())
                .imageCreator(nodeImage.getCreator())
                .externalUri(nodeImage.getUri())
                .imageName(nodeImage.getImageName())
                .imageCopyright(nodeImage.getCopyRight())
                .idThesaurus(idThesaurus)
                .idConcept(idConcept)
                .build();

        imagesRepository.save(image);
    }

    public ImageExterne addExternalImage(String idConcept, String idThesaurus, String imageName, String copyRight,
                                 String uri, String creator, int idUser) {

        return imagesRepository.save(ImageExterne.builder()
                .imageCreator(creator)
                .idUser(idUser)
                .idConcept(idConcept)
                .idThesaurus(idThesaurus)
                .imageName(StringUtils.isEmpty(imageName) ? "" : imageName)
                .imageCopyright(copyRight)
                .externalUri(uri.trim())
                .build());
    }

    public void deleteImagesByThesaurus(String idThesaurus) {

        log.debug("Suppression de toutes les images présentes dans le thésaurus id {}", idThesaurus);
        imagesRepository.deleteByIdThesaurus(idThesaurus);
    }

    public void updateThesaurusId(String oldIdThesaurus, String newIdThesaurus) {

        log.debug("Mise à jour du thésaurus id de toutes les images présentes dans le thésaurus id {}", oldIdThesaurus);
        imagesRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }
}
