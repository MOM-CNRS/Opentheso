package fr.cnrs.opentheso.v2.user.api;

import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.UserSearchRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDirectoryApiControllerTest {

    @Mock
    private UserCommandRepository userCommandRepository;

    private UserDirectoryApiController controller;

    @BeforeEach
    void setUp() {
        controller = new UserDirectoryApiController(userCommandRepository);
    }

    @Test
    void search_blankQueryListsUsersAlphabetically() {
        when(userCommandRepository.searchDirectory("", UserDirectoryApiController.DIRECTORY_LIMIT))
                .thenReturn(List.of(
                        new UserSearchRow(2, "a.costa", "a@example.org"),
                        new UserSearchRow(1, "c.roussel", "c@example.org")
                ));

        var result = controller.search("  ");

        assertEquals(List.of("a.costa", "c.roussel"), result.stream().map(item -> item.username()).toList());
        verify(userCommandRepository).searchDirectory("", UserDirectoryApiController.DIRECTORY_LIMIT);
    }

    @Test
    void search_filtersByQueryWithoutExposingEmail() {
        when(userCommandRepository.searchDirectory("rou", UserDirectoryApiController.DIRECTORY_LIMIT))
                .thenReturn(List.of(new UserSearchRow(1, "c.roussel", "secret@example.org")));

        var result = controller.search("rou");

        assertEquals(1, result.size());
        assertEquals("c.roussel", result.get(0).username());
    }
}
