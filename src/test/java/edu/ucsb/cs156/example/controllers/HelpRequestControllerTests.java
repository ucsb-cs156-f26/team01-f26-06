package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.HelpRequest;
import edu.ucsb.cs156.example.repositories.HelpRequestRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = HelpRequestController.class)
@Import(TestConfig.class)
public class HelpRequestControllerTests extends ControllerTestCase {

  @MockitoBean HelpRequestRepository helpRequestRepository;

  @MockitoBean UserRepository userRepository;

  // Authorization test for /api/helprequest/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/helprequest/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/helprequest/all")).andExpect(status().is(200)); // logged
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_helprequests() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2026-10-20T12:00:00");

    HelpRequest helpRequest1 =
        HelpRequest.builder()
            .requesterEmail("a@ucsb.edu")
            .teamId("team01")
            .tableOrBreakoutRoom("table 1")
            .requestTime(ldt1)
            .explanation("Stuck on tests")
            .solved(false)
            .build();

    LocalDateTime ldt2 = LocalDateTime.parse("2026-10-21T13:30:00");

    HelpRequest helpRequest2 =
        HelpRequest.builder()
            .requesterEmail("b@ucsb.edu")
            .teamId("team02")
            .tableOrBreakoutRoom("room 2")
            .requestTime(ldt2)
            .explanation("Merge conflict")
            .solved(true)
            .build();

    ArrayList<HelpRequest> expectedRequests = new ArrayList<>();
    expectedRequests.addAll(Arrays.asList(helpRequest1, helpRequest2));

    when(helpRequestRepository.findAll()).thenReturn(expectedRequests);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/helprequest/all")).andExpect(status().isOk()).andReturn();

    // assert

    verify(helpRequestRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedRequests);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // Tests for POST /api/helprequest/post

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/helprequest/post")
                .param("requesterEmail", "a@ucsb.edu")
                .param("teamId", "team01")
                .param("tableOrBreakoutRoom", "table 1")
                .param("requestTime", "2026-10-20T12:00:00")
                .param("explanation", "Stuck on tests")
                .param("solved", "false")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/helprequest/post")
                .param("requesterEmail", "a@ucsb.edu")
                .param("teamId", "team01")
                .param("tableOrBreakoutRoom", "table 1")
                .param("requestTime", "2026-10-20T12:00:00")
                .param("explanation", "Stuck on tests")
                .param("solved", "false")
                .with(csrf()))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_helprequest() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2026-10-20T12:00:00");

    HelpRequest helpRequest1 =
        HelpRequest.builder()
            .requesterEmail("a@ucsb.edu")
            .teamId("team01")
            .tableOrBreakoutRoom("table 1")
            .requestTime(ldt1)
            .explanation("Stuck on tests")
            .solved(true)
            .build();

    when(helpRequestRepository.save(eq(helpRequest1))).thenReturn(helpRequest1);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/helprequest/post")
                    .param("requesterEmail", "a@ucsb.edu")
                    .param("teamId", "team01")
                    .param("tableOrBreakoutRoom", "table 1")
                    .param("requestTime", "2026-10-20T12:00:00")
                    .param("explanation", "Stuck on tests")
                    .param("solved", "true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(helpRequestRepository, times(1)).save(helpRequest1);
    String expectedJson = mapper.writeValueAsString(helpRequest1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // Tests for GET /api/helprequest?id=...

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc.perform(get("/api/helprequest").param("id", "7")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {
    // arrange
    LocalDateTime ldt = LocalDateTime.parse("2026-10-20T12:00:00");

    HelpRequest helpRequest =
        HelpRequest.builder()
            .requesterEmail("a@ucsb.edu")
            .teamId("team01")
            .tableOrBreakoutRoom("table 1")
            .requestTime(ldt)
            .explanation("Stuck on tests")
            .solved(false)
            .build();

    when(helpRequestRepository.findById(eq(7L))).thenReturn(Optional.of(helpRequest));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/helprequest").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(helpRequestRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(helpRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {
    // arrange
    when(helpRequestRepository.findById(eq(7L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/helprequest").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(helpRequestRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("HelpRequest with id 7 not found", json.get("message"));
  }
}
