package fr.cnrs.opentheso.v2.user.api;

import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.user.api.dto.UserDirectoryItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v2/api", "/v2-preview/api"})
public class UserDirectoryApiController {

    static final int DIRECTORY_LIMIT = 200;

    private final UserCommandRepository userCommandRepository;

    public UserDirectoryApiController(UserCommandRepository userCommandRepository) {
        this.userCommandRepository = userCommandRepository;
    }

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserDirectoryItem> search(@RequestParam(required = false) String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() > 80) {
            query = query.substring(0, 80);
        }
        return userCommandRepository.searchDirectory(query, DIRECTORY_LIMIT).stream()
                .filter(row -> row.username() != null && !row.username().isBlank())
                .map(row -> new UserDirectoryItem(row.username()))
                .toList();
    }
}
