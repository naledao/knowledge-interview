package com.example.backend.knowledge.search.interfaces.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.knowledge.search.application.HotSearchKeywordService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HotSearchKeywordControllerTests {
    private HotSearchKeywordService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(HotSearchKeywordService.class);
        mvc = MockMvcBuilders.standaloneSetup(new HotSearchKeywordController(service)).build();
    }

    @Test
    void returnsKeywordArrayWithDefaultLimit() throws Exception {
        when(service.listHotKeywords(5)).thenReturn(List.of("Vue", "Redis"));
        mvc.perform(get("/api/search/hot-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Vue"));
    }

    @Test
    void mapsInvalidLimitToBadRequest() throws Exception {
        when(service.listHotKeywords(0)).thenThrow(new IllegalArgumentException("热门词数量必须在 1 到 20 之间"));
        mvc.perform(get("/api/search/hot-keywords").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("热门词数量必须在 1 到 20 之间"));
    }
}
