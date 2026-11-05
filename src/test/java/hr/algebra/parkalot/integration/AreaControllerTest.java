package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.helper.AreaHelper;
import hr.algebra.parkalot.models.enums.Zone;
import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.services.db.AreaService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AreaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AreaService areaService;

    private Area testArea;

    @BeforeEach
    void setUp() {
        testArea = AreaHelper.getAreaWithId();
    }

    // --------------------------------------------------
    // GET /rest/area
    // --------------------------------------------------

    @Test
    @WithMockUser(roles = "USER")
    void getAllAreas_whenAuthenticated_returnsAreas() throws Exception {
        when(areaService.findAll()).thenReturn(List.of(testArea));

        mockMvc.perform(get("/rest/area"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Area1"))
                .andExpect(jsonPath("$[0].zone").exists())
                .andExpect(jsonPath("$[0].coordinates").isArray());

        verify(areaService).findAll();
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void getAllAreas_whenOfficer_returnsAreas() throws Exception {
        when(areaService.findAll()).thenReturn(List.of(testArea));

        mockMvc.perform(get("/rest/area"))
                .andExpect(status().isOk());

        verify(areaService).findAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAreas_whenAdmin_returnsAreas() throws Exception {
        when(areaService.findAll()).thenReturn(List.of(testArea));

        mockMvc.perform(get("/rest/area"))
                .andExpect(status().isOk());

        verify(areaService).findAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllAreas_whenNoAreasExist_returnsEmptyList() throws Exception {
        when(areaService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/rest/area"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --------------------------------------------------
    // GET /rest/area/whereami
    // --------------------------------------------------

    @Test
    @WithMockUser(roles = "USER")
    void getAreaForCoordinates_whenAreaExists_returnsArea() throws Exception {
        when(areaService.getCurrentArea(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(testArea));

        String requestBody = """
                {
                    "latitude": 45.8150,
                    "longitude": 15.9819
                }
                """;

        mockMvc.perform(get("/rest/area/whereami")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Area1"))
                .andExpect(jsonPath("$.coordinates").isArray());

        verify(areaService).getCurrentArea(45.8150, 15.9819);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAreaForCoordinates_whenAreaDoesNotExist_returnsNotFound()
            throws Exception {

        when(areaService.getCurrentArea(anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());

        String requestBody = """
                {
                    "latitude": 45.8150,
                    "longitude": 15.9819
                }
                """;

        mockMvc.perform(get("/rest/area/whereami")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());

        verify(areaService).getCurrentArea(45.8150, 15.9819);
    }

    // --------------------------------------------------
    // POST /rest/area
    // --------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createArea_whenValidRequest_savesArea() throws Exception {
        when(areaService.save(any(Area.class)))
                .thenAnswer(invocation -> {
                    Area area = invocation.getArgument(0);
                    area.setId(2L);
                    return area;
                });

        String requestBody = """
                {
                    "name": "New Area",
                    "zone": "ZONE_1",
                    "coordinates": [
                        {
                            "latitude": 45.8100,
                            "longitude": 15.9700
                        },
                        {
                            "latitude": 45.8200,
                            "longitude": 15.9700
                        },
                        {
                            "latitude": 45.8200,
                            "longitude": 15.9800
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/rest/area")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(areaService).save(argThat(area ->
                area.getId().equals(2L)
                        && area.getName().equals("New Area")
                        && area.getZone() == Zone.ZONE_1
                        && area.getAreaPolygon() != null
        ));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createArea_whenUserIsNotAdmin_returnsForbidden() throws Exception {
        String requestBody = """
                {
                    "name": "New Area",
                    "zone": "ZONE_1",
                    "coordinates": [
                        {"latitude": 45.8100, "longitude": 15.9700},
                        {"latitude": 45.8200, "longitude": 15.9700},
                        {"latitude": 45.8200, "longitude": 15.9800}
                    ]
                }
                """;

        mockMvc.perform(post("/rest/area")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verify(areaService, never()).save(any(Area.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createArea_whenCoordinatesAreInsufficient_returnsBadRequest()
            throws Exception {

        String requestBody = """
                {
                    "name": "Invalid Area",
                    "zone": "ZONE_1",
                    "coordinates": [
                        {"latitude": 45.8100, "longitude": 15.9700},
                        {"latitude": 45.8200, "longitude": 15.9700}
                    ]
                }
                """;

        mockMvc.perform(post("/rest/area")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(areaService, never()).save(any(Area.class));
    }

    // --------------------------------------------------
    // DELETE /rest/area/{id}
    // --------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteArea_whenAreaExists_deletesArea() throws Exception {
        when(areaService.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/rest/area/{id}", 1L)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(areaService).existsById(1L);
        verify(areaService).deleteById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteArea_whenAreaDoesNotExist_returnsNotFound()
            throws Exception {

        when(areaService.existsById(99L)).thenReturn(false);

        mockMvc.perform(delete("/rest/area/{id}", 99L)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(areaService).existsById(99L);
        verify(areaService, never()).deleteById(anyLong());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteArea_whenUserIsNotAdmin_returnsForbidden()
            throws Exception {

        mockMvc.perform(delete("/rest/area/{id}", 1L)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(areaService, never()).existsById(anyLong());
        verify(areaService, never()).deleteById(anyLong());
    }
}
