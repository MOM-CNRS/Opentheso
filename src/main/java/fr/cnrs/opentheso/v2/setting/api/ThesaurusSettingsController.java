package fr.cnrs.opentheso.v2.setting.api;

import fr.cnrs.opentheso.v2.setting.api.dto.CreateCorpusRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.CorpusResponse;
import fr.cnrs.opentheso.v2.setting.api.dto.ThesaurusIdentifierSettingsResponse;
import fr.cnrs.opentheso.v2.setting.api.dto.ThesaurusPreferencesResponse;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateCorpusRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateThesaurusIdentifierSettingsRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateThesaurusPreferencesRequest;
import fr.cnrs.opentheso.v2.setting.api.mapper.SettingApiMapper;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/openapi/v2/thesauri/{thesaurusId}/settings")
@RequiredArgsConstructor
@Tag(name = "Paramètres thésaurus", description = "Gestion des préférences, identifiants et corpus d'un thésaurus (v2)")
@SecurityRequirement(name = "ApiKeyAuth")
public class ThesaurusSettingsController {

    private final SettingAuthSupport settingAuthSupport;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ThesaurusCorpusService thesaurusCorpusService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @GetMapping(value = "/preferences", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lire les préférences", description = "Retourne les préférences générales du thésaurus.")
    public ThesaurusPreferencesResponse getPreferences(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId
    ) {
        int userId = settingAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        settingAuthSupport.requireThesaurusManager(userId, thesaurusId);
        return SettingApiMapper.toPreferencesResponse(
                thesaurusPreferenceService.loadPreferences(thesaurusId, defaultWorkLanguage)
        );
    }

    @PutMapping(value = "/preferences", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mettre à jour les préférences", description = "Met à jour les préférences générales du thésaurus.")
    public ThesaurusPreferencesResponse updatePreferences(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @Valid @RequestBody UpdateThesaurusPreferencesRequest request
    ) {
        int userId = settingAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        settingAuthSupport.requireThesaurusManager(userId, thesaurusId);
        var current = thesaurusPreferenceService.loadPreferences(thesaurusId, defaultWorkLanguage);
        var merged = SettingApiMapper.mergePreferencesUpdate(current, request);
        return SettingApiMapper.toPreferencesResponse(
                thesaurusPreferenceService.savePreferences(thesaurusId, merged, defaultWorkLanguage)
        );
    }

    @GetMapping(value = "/identifiers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lire les paramètres d'identifiants", description = "Retourne la configuration des serveurs d'identifiants.")
    public ThesaurusIdentifierSettingsResponse getIdentifiers(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId
    ) {
        int userId = settingAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        settingAuthSupport.requireThesaurusManager(userId, thesaurusId);
        return SettingApiMapper.toIdentifierResponse(
                thesaurusPreferenceService.loadPreferences(thesaurusId, defaultWorkLanguage)
        );
    }

    @PutMapping(value = "/identifiers", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mettre à jour les identifiants", description = "Met à jour la configuration des serveurs d'identifiants.")
    public ThesaurusIdentifierSettingsResponse updateIdentifiers(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @Valid @RequestBody UpdateThesaurusIdentifierSettingsRequest request
    ) {
        int userId = settingAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        settingAuthSupport.requireThesaurusManager(userId, thesaurusId);
        var current = thesaurusPreferenceService.loadPreferences(thesaurusId, defaultWorkLanguage);
        var merged = SettingApiMapper.mergeIdentifierUpdate(current, request);
        return SettingApiMapper.toIdentifierResponse(
                thesaurusPreferenceService.saveIdentifierSettings(
                        thesaurusId,
                        merged,
                        request.apiKeyOpenArk(),
                        defaultWorkLanguage
                )
        );
    }

    @GetMapping(value = "/corpus", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister les corpus", description = "Retourne les corpus liés au thésaurus.")
    public List<CorpusResponse> listCorpus(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId
    ) {
        int userId = settingAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        settingAuthSupport.requireThesaurusManager(userId, thesaurusId);
        return thesaurusCorpusService.listCorpus(thesaurusId).stream()
                .map(SettingApiMapper::toCorpusResponse)
                .toList();
    }

    @PostMapping(value = "/corpus", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Créer un corpus", description = "Ajoute un corpus au thésaurus.")
    public CorpusResponse createCorpus(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @Valid @RequestBody CreateCorpusRequest request
    ) {
        int userId = settingAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        settingAuthSupport.requireThesaurusManager(userId, thesaurusId);
        return SettingApiMapper.toCorpusResponse(
                thesaurusCorpusService.createCorpus(thesaurusId, SettingApiMapper.toCorpusModel(request))
        );
    }

    @PutMapping(value = "/corpus/{name}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier un corpus", description = "Met à jour un corpus existant.")
    public CorpusResponse updateCorpus(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String name,
            @Valid @RequestBody UpdateCorpusRequest request
    ) {
        int userId = settingAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        settingAuthSupport.requireThesaurusManager(userId, thesaurusId);
        return SettingApiMapper.toCorpusResponse(
                thesaurusCorpusService.updateCorpus(thesaurusId, name, SettingApiMapper.toCorpusModel(request))
        );
    }

    @DeleteMapping("/corpus/{name}")
    @Operation(summary = "Supprimer un corpus", description = "Supprime un corpus du thésaurus.")
    public ResponseEntity<Void> deleteCorpus(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String name
    ) {
        int userId = settingAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        settingAuthSupport.requireThesaurusManager(userId, thesaurusId);
        thesaurusCorpusService.deleteCorpus(thesaurusId, name);
        return ResponseEntity.noContent().build();
    }
}
