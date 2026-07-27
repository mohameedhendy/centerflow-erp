package com.centerflow.academic;

import com.centerflow.academic.branch.domain.Branch;
import com.centerflow.academic.branch.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BranchApiTests {

    private static final String BRANCHES_URL =
            "/api/v1/academic/branches";

    private final MockMvc mockMvc;
    private final BranchRepository branchRepository;

    @Autowired
    BranchApiTests(
            MockMvc mockMvc,
            BranchRepository branchRepository
    ) {
        this.mockMvc = mockMvc;
        this.branchRepository = branchRepository;
    }

    @Test
    void createsBranchAndNormalizesValues()
            throws Exception {

        mockMvc.perform(
                        post(BRANCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "zag-01",
                                          "name": " Zagazig Main Branch ",
                                          "phone": "+20 100 000 0000",
                                          "email": "ZAG@CENTERFLOW.COM",
                                          "address": "Zagazig City Center",
                                          "city": "Zagazig"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("ZAG-01")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Zagazig Main Branch"
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "zag@centerflow.com"
                                )
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                );

        Branch branch = branchRepository
                .findByCode("ZAG-01")
                .orElseThrow();

        assertThat(branch.getEmail())
                .isEqualTo("zag@centerflow.com");

        assertThat(branch.isActive()).isTrue();
    }

    @Test
    void rejectsDuplicateCodeIgnoringCase()
            throws Exception {

        createBranch(
                "cairo-01",
                "Cairo Main Branch",
                "Cairo"
        );

        mockMvc.perform(
                        post(BRANCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "CAIRO-01",
                                          "name": "Another Cairo Branch",
                                          "city": "Cairo"
                                        }
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status").value(409)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "A branch already exists with code: CAIRO-01"
                                )
                );
    }

    @Test
    void updatesBranchAndChangesItsStatus()
            throws Exception {

        UUID branchId = createBranch(
                "MANS-01",
                "Mansoura Branch",
                "Mansoura"
        );

        mockMvc.perform(
                        put(
                                BRANCHES_URL
                                        + "/"
                                        + branchId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Mansoura Main Branch",
                                          "phone": "+20 101 111 1111",
                                          "email": "mansoura@centerflow.com",
                                          "address": "Central Mansoura",
                                          "city": "Mansoura"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("MANS-01")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Mansoura Main Branch"
                                )
                );

        mockMvc.perform(
                        patch(
                                BRANCHES_URL
                                        + "/"
                                        + branchId
                                        + "/status"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "active": false
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.active")
                                .value(false)
                );

        Branch branch = branchRepository
                .findById(branchId)
                .orElseThrow();

        assertThat(branch.isActive()).isFalse();
    }

    @Test
    void searchesAndFiltersBranchesInDatabase()
            throws Exception {

        createBranch(
                "CAIRO-01",
                "Cairo Main Branch",
                "Cairo"
        );

        createBranch(
                "CAIRO-02",
                "Cairo East Branch",
                "Cairo"
        );

        UUID inactiveBranchId = createBranch(
                "ALEX-01",
                "Alexandria Branch",
                "Alexandria"
        );

        mockMvc.perform(
                        patch(
                                BRANCHES_URL
                                        + "/"
                                        + inactiveBranchId
                                        + "/status"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "active": false
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(BRANCHES_URL)
                                .queryParam(
                                        "search",
                                        "cairo"
                                )
                                .queryParam(
                                        "city",
                                        "CAIRO"
                                )
                                .queryParam(
                                        "active",
                                        "true"
                                )
                                .queryParam("page", "0")
                                .queryParam("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.page").value(0)
                )
                .andExpect(
                        jsonPath("$.size").value(10)
                );
    }

    @Test
    void returnsNotFoundForMissingBranch()
            throws Exception {

        UUID missingId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                BRANCHES_URL
                                        + "/"
                                        + missingId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status").value(404)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Branch was not found: "
                                                + missingId
                                )
                );
    }

    @Test
    void rejectsInvalidBranchRequest()
            throws Exception {

        mockMvc.perform(
                        post(BRANCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "invalid code",
                                          "name": "",
                                          "email": "invalid-email"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.code"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.name"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.email"
                        ).exists()
                );
    }

    private UUID createBranch(
            String code,
            String name,
            String city
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(BRANCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "%s",
                                          "name": "%s",
                                          "city": "%s"
                                        }
                                        """.formatted(
                                                code,
                                                name,
                                                city
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        String location =
                result.getResponse()
                        .getContentAsString();

        String idValue = location
                .replaceAll(
                        ".*\"id\":\"([^\"]+)\".*",
                        "$1"
                );

        return UUID.fromString(idValue);
    }
}