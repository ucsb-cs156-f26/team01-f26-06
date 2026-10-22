package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = RecommendationRequestController.class)
@Import(TestConfig.class)
public class RecommendationRequestControllerTests extends ControllerTestCase {

  @MockitoBean RecommendationRequestRepository recommendationRequestRepository;

  @MockitoBean UserRepository userRepository;

  // Authorization tests for /api/RecommendationRequest/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/RecommendationRequest/all")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/RecommendationRequest/all")).andExpect(status().is(200));
  }

  // Authorization tests for /api/RecommendationRequest/post

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/RecommendationRequest/post")
                .param("requesterEmail", "student@ucsb.edu")
                .param("professorEmail", "prof@ucsb.edu")
                .param("explanation", "grad school")
                .param("dateRequested", "2022-01-03T00:00:00")
                .param("dateNeeded", "2022-02-03T00:00:00")
                .param("done", "false")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/RecommendationRequest/post")
                .param("requesterEmail", "student@ucsb.edu")
                .param("professorEmail", "prof@ucsb.edu")
                .param("explanation", "grad school")
                .param("dateRequested", "2022-01-03T00:00:00")
                .param("dateNeeded", "2022-02-03T00:00:00")
                .param("done", "false")
                .with(csrf()))
        .andExpect(status().is(403)); // only admins can post
  }

  // Tests with mocks for database actions

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_recommendation_requests() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-02-03T00:00:00");

    RecommendationRequest request1 =
        RecommendationRequest.builder()
            .requesterEmail("student1@ucsb.edu")
            .professorEmail("prof1@ucsb.edu")
            .explanation("grad school")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(false)
            .build();

    RecommendationRequest request2 =
        RecommendationRequest.builder()
            .requesterEmail("student2@ucsb.edu")
            .professorEmail("prof2@ucsb.edu")
            .explanation("internship")
            .dateRequested(ldt2)
            .dateNeeded(ldt2.plusMonths(1))
            .done(true)
            .build();

    ArrayList<RecommendationRequest> expectedRequests = new ArrayList<>();
    expectedRequests.addAll(Arrays.asList(request1, request2));

    when(recommendationRequestRepository.findAll()).thenReturn(expectedRequests);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/RecommendationRequest/all"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedRequests);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_recommendation_request() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-02-03T00:00:00");

    RecommendationRequest request1 =
        RecommendationRequest.builder()
            .requesterEmail("student@ucsb.edu")
            .professorEmail("prof@ucsb.edu")
            .explanation("grad school")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(true)
            .build();

    when(recommendationRequestRepository.save(request1)).thenReturn(request1);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/RecommendationRequest/post")
                    .param("requesterEmail", "student@ucsb.edu")
                    .param("professorEmail", "prof@ucsb.edu")
                    .param("explanation", "grad school")
                    .param("dateRequested", "2022-01-03T00:00:00")
                    .param("dateNeeded", "2022-02-03T00:00:00")
                    .param("done", "true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1)).save(request1);
    String expectedJson = mapper.writeValueAsString(request1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // Tests for GET /api/RecommendationRequest?id=...

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc.perform(get("/api/RecommendationRequest").param("id", "7")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("student@ucsb.edu")
            .professorEmail("professor@ucsb.edu")
            .explanation("Need recommendation for graduate school")
            .dateRequested(LocalDateTime.parse("2026-10-20T12:00:00"))
            .dateNeeded(LocalDateTime.parse("2026-11-20T12:00:00"))
            .done(false)
            .build();

    when(recommendationRequestRepository.findById(eq(7L)))
        .thenReturn(Optional.of(recommendationRequest));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/RecommendationRequest").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(recommendationRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(recommendationRequestRepository.findById(eq(7L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/RecommendationRequest").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("RecommendationRequest with id 7 not found", json.get("message"));
  }

  // Tests for PUT /api/RecommendationRequest?id=...

  @Test
  public void logged_out_users_cannot_put() throws Exception {
    mockMvc
        .perform(
            put("/api/RecommendationRequest")
                .param("id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_users_cannot_put() throws Exception {
    mockMvc
        .perform(
            put("/api/RecommendationRequest")
                .param("id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_recommendationrequest() throws Exception {
    // arrange

    RecommendationRequest original =
        RecommendationRequest.builder()
            .requesterEmail("student@ucsb.edu")
            .professorEmail("professor@ucsb.edu")
            .explanation("Need recommendation for graduate school")
            .dateRequested(LocalDateTime.parse("2026-10-20T12:00:00"))
            .dateNeeded(LocalDateTime.parse("2026-11-20T12:00:00"))
            .done(false)
            .build();

    RecommendationRequest edited =
        RecommendationRequest.builder()
            .requesterEmail("other.student@ucsb.edu")
            .professorEmail("other.professor@ucsb.edu")
            .explanation("Need recommendation for a summer internship")
            .dateRequested(LocalDateTime.parse("2026-10-21T08:30:00"))
            .dateNeeded(LocalDateTime.parse("2026-12-01T17:00:00"))
            .done(true)
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(recommendationRequestRepository.findById(eq(67L))).thenReturn(Optional.of(original));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/RecommendationRequest")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1)).findById(eq(67L));
    verify(recommendationRequestRepository, times(1))
        .save(edited); // should be saved with updated info
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_recommendationrequest_that_does_not_exist() throws Exception {
    // arrange

    RecommendationRequest edited =
        RecommendationRequest.builder()
            .requesterEmail("student@ucsb.edu")
            .professorEmail("professor@ucsb.edu")
            .explanation("Need recommendation for graduate school")
            .dateRequested(LocalDateTime.parse("2026-10-20T12:00:00"))
            .dateNeeded(LocalDateTime.parse("2026-11-20T12:00:00"))
            .done(false)
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(recommendationRequestRepository.findById(eq(67L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/RecommendationRequest")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1)).findById(eq(67L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("RecommendationRequest with id 67 not found", json.get("message"));
  }
}
