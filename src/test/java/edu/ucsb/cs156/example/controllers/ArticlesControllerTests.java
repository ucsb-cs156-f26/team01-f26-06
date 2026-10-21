package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.Articles;
import edu.ucsb.cs156.example.repositories.ArticlesRepository;
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

@WebMvcTest(controllers = ArticlesController.class)
@Import(TestConfig.class)
public class ArticlesControllerTests extends ControllerTestCase {

  @MockitoBean ArticlesRepository articlesRepository;

  @MockitoBean UserRepository userRepository;

  // Authorization tests for /api/articles/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/articles/all")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/articles/all")).andExpect(status().is(200));
  }

  // Authorization tests for /api/articles?id=...

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc.perform(get("/api/articles").param("id", "7")).andExpect(status().is(403));
  }

  // Authorization tests for /api/articles/post

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/articles/post")
                .param("title", "Using testing-playground with React Testing Library")
                .param("url", "https://dev.to/katieraby/using-testing-playground")
                .param("explanation", "Helpful when we get to front end development")
                .param("email", "phtcon@ucsb.edu")
                .param("dateAdded", "2022-04-20T00:00:00")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/articles/post")
                .param("title", "Using testing-playground with React Testing Library")
                .param("url", "https://dev.to/katieraby/using-testing-playground")
                .param("explanation", "Helpful when we get to front end development")
                .param("email", "phtcon@ucsb.edu")
                .param("dateAdded", "2022-04-20T00:00:00")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  // Tests with mocks for database actions

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange
    LocalDateTime ldt = LocalDateTime.parse("2022-04-20T00:00:00");

    Articles article =
        Articles.builder()
            .title("Using testing-playground with React Testing Library")
            .url("https://dev.to/katieraby/using-testing-playground")
            .explanation("Helpful when we get to front end development")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt)
            .build();

    when(articlesRepository.findById(eq(7L))).thenReturn(Optional.of(article));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/articles").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(articlesRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(article);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange
    when(articlesRepository.findById(eq(7L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/articles").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(articlesRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Articles with id 7 not found", json.get("message"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_articles() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-04-20T00:00:00");

    Articles article1 =
        Articles.builder()
            .title("Using testing-playground with React Testing Library")
            .url("https://dev.to/katieraby/using-testing-playground")
            .explanation("Helpful when we get to front end development")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt1)
            .build();

    LocalDateTime ldt2 = LocalDateTime.parse("2022-04-19T00:00:00");

    Articles article2 =
        Articles.builder()
            .title("Handy Spring Utility Classes")
            .url("https://twitter.com/maciejwalkowiak/status/1511736828369719300")
            .explanation("A lot of really useful classes are built into Spring")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt2)
            .build();

    ArrayList<Articles> expectedArticles = new ArrayList<>();
    expectedArticles.addAll(Arrays.asList(article1, article2));

    when(articlesRepository.findAll()).thenReturn(expectedArticles);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/articles/all")).andExpect(status().isOk()).andReturn();

    // assert
    verify(articlesRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedArticles);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_article() throws Exception {
    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-04-20T00:00:00");

    Articles article1 =
        Articles.builder()
            .title("Using testing-playground with React Testing Library")
            .url("https://dev.to/katieraby/using-testing-playground")
            .explanation("Helpful when we get to front end development")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt1)
            .build();

    when(articlesRepository.save(eq(article1))).thenReturn(article1);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/articles/post")
                    .param("title", "Using testing-playground with React Testing Library")
                    .param("url", "https://dev.to/katieraby/using-testing-playground")
                    .param("explanation", "Helpful when we get to front end development")
                    .param("email", "phtcon@ucsb.edu")
                    .param("dateAdded", "2022-04-20T00:00:00")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(articlesRepository, times(1)).save(article1);
    String expectedJson = mapper.writeValueAsString(article1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_an_article() throws Exception {
    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-04-20T00:00:00");

    Articles article1 =
        Articles.builder()
            .title("Using testing-playground with React Testing Library")
            .url("https://dev.to/katieraby/using-testing-playground")
            .explanation("Helpful when we get to front end development")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt1)
            .build();

    when(articlesRepository.findById(eq(15L))).thenReturn(Optional.of(article1));

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/articles").param("id", "15").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(articlesRepository, times(1)).findById(15L);
    verify(articlesRepository, times(1)).delete(any());

    Map<String, Object> json = responseToJson(response);
    assertEquals("Articles with id 15 deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_tries_to_delete_non_existant_article_and_gets_right_error_message()
      throws Exception {
    // arrange
    when(articlesRepository.findById(eq(15L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/articles").param("id", "15").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(articlesRepository, times(1)).findById(15L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("Articles with id 15 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_article() throws Exception {
    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-04-20T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2023-01-03T00:00:00");

    Articles articleOrig =
        Articles.builder()
            .title("Using testing-playground with React Testing Library")
            .url("https://dev.to/katieraby/using-testing-playground")
            .explanation("Helpful when we get to front end development")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt1)
            .build();

    Articles articleEdited =
        Articles.builder()
            .title("Handy Spring Utility Classes")
            .url("https://twitter.com/maciejwalkowiak/status/1511736828369719300")
            .explanation("A lot of really useful classes are built into Spring")
            .email("cgaucho@ucsb.edu")
            .dateAdded(ldt2)
            .build();

    String requestBody = mapper.writeValueAsString(articleEdited);

    when(articlesRepository.findById(eq(67L))).thenReturn(Optional.of(articleOrig));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/articles")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(articlesRepository, times(1)).findById(67L);
    verify(articlesRepository, times(1)).save(articleEdited);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_article_that_does_not_exist() throws Exception {
    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-04-20T00:00:00");

    Articles articleEdited =
        Articles.builder()
            .title("Using testing-playground with React Testing Library")
            .url("https://dev.to/katieraby/using-testing-playground")
            .explanation("Helpful when we get to front end development")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt1)
            .build();

    String requestBody = mapper.writeValueAsString(articleEdited);

    when(articlesRepository.findById(eq(67L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/articles")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(articlesRepository, times(1)).findById(67L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("Articles with id 67 not found", json.get("message"));
  }
}
