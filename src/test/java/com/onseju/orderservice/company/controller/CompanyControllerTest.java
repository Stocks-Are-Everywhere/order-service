package com.onseju.orderservice.company.controller;

import com.onseju.orderservice.company.controller.response.CompanySearchResponse;
import com.onseju.orderservice.company.service.CompanyService;
import com.onseju.orderservice.global.jwt.JwtUtil;
import com.onseju.orderservice.global.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompanyController.class)
@AutoConfigureMockMvc(addFilters = false)
class CompanyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CompanyService companyService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	UserDetailsServiceImpl userDetailsServiceImpl;

	@Nested
	@DisplayName("회사 검색 기능 테스트")
	class searchCompany {

		@Test
		@DisplayName("회사 검색 정상 케이스 테스트")
		void 회사검색_정상케이스() throws Exception {
			String query = "삼성";
			List<CompanySearchResponse> mockCompanies = Arrays.asList(
					new CompanySearchResponse("삼성전자", "005930", "KOSPI", "주권", "삼성전자"),
					new CompanySearchResponse("삼성물산", "028260", "KOSPI", "주권", "삼성물산")
			);

			when(companyService.searchCompanies(query)).thenReturn(mockCompanies);

			mockMvc.perform(get("/api/companies/search")
							.param("query", query))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(2)))
					.andExpect(jsonPath("$[0].isuNm").value("삼성전자"))
					.andExpect(jsonPath("$[0].isuSrtCd").value("005930"))
					.andExpect(jsonPath("$[1].isuNm").value("삼성물산"))
					.andExpect(jsonPath("$[1].isuSrtCd").value("028260"));

			verify(companyService).searchCompanies(query);
		}

		@Test
		@DisplayName("회사 검색 결과 없음 테스트")
		void 회사검색_결과없음() throws Exception {
			String query = "존재하지않는회사";
			when(companyService.searchCompanies(query)).thenReturn(Collections.emptyList());

			mockMvc.perform(get("/api/companies/search")
							.param("query", query))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(0)));

			verify(companyService).searchCompanies(query);
		}

		@Test
		@DisplayName("회사 검색 데이터 정합성 검증 테스트")
		void TC20_1_2_데이터정합성검증() throws Exception {
			String query = "삼성전자";
			CompanySearchResponse mockCompany = new CompanySearchResponse("삼성전자", "005930", "KOSPI", "주권", "삼성전자");
			when(companyService.searchCompanies(query)).thenReturn(Collections.singletonList(mockCompany));

			mockMvc.perform(get("/api/companies/search")
							.param("query", query))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$[0].isuNm").value("삼성전자"))
					.andExpect(jsonPath("$[0].isuSrtCd").value("005930"))
					.andExpect(jsonPath("$[0].isuAbbrv").value("KOSPI"))
					.andExpect(jsonPath("$[0].isEngNm").value("주권"))
					.andExpect(jsonPath("$[0].kindstkcertTpNm").value("삼성전자"));

			verify(companyService).searchCompanies(query);
		}

		@Test
		@DisplayName("검색어가 공백일 경우 비어있는 리스트값을 반환한다.")
		void 비어있는_검색어() throws Exception {
			String query = "";
			when(companyService.searchCompanies(query)).thenReturn(Collections.emptyList());

			mockMvc.perform(get("/api/companies/search")
							.param("query", query))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(0)));
		}
	}
}
