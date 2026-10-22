package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.errors.EntityNotFoundException;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** This is a REST controller for RecommendationRequest */
@Tag(name = "RecommendationRequest")
@RequestMapping("/api/RecommendationRequest")
@RestController
@Slf4j
public class RecommendationRequestController extends ApiController {

  @Autowired RecommendationRequestRepository recommendationRequestRepository;

  /**
   * List all recommendation requests
   *
   * @return an iterable of RecommendationRequest
   */
  @Operation(summary = "List all recommendation requests")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/all")
  public Iterable<RecommendationRequest> allRecommendationRequests() {
    return recommendationRequestRepository.findAll();
  }

  /**
   * Create a new recommendation request
   *
   * @param requesterEmail email of the student requesting the recommendation
   * @param professorEmail email of the professor being asked to write the recommendation
   * @param explanation what the recommendation is for
   * @param dateRequested when the request was made (ISO format)
   * @param dateNeeded when the recommendation is needed by (ISO format)
   * @param done whether the request has been completed
   * @return the saved recommendation request
   */
  @Operation(summary = "Create a new recommendation request")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/post")
  public RecommendationRequest postRecommendationRequest(
      @Parameter(
              name = "requesterEmail",
              description = "Email of the student requesting the recommendation")
          @RequestParam
          String requesterEmail,
      @Parameter(
              name = "professorEmail",
              description = "Email of the professor asked to write the recommendation")
          @RequestParam
          String professorEmail,
      @Parameter(
              name = "explanation",
              description = "What the recommendation is for, e.g. grad school application")
          @RequestParam
          String explanation,
      @Parameter(
              name = "dateRequested",
              description =
                  "Date the request was made (in iso format, e.g. YYYY-mm-ddTHH:MM:SS; see https://en.wikipedia.org/wiki/ISO_8601)")
          @RequestParam("dateRequested")
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateRequested,
      @Parameter(
              name = "dateNeeded",
              description =
                  "Date the recommendation is needed by (in iso format, e.g. YYYY-mm-ddTHH:MM:SS; see https://en.wikipedia.org/wiki/ISO_8601)")
          @RequestParam("dateNeeded")
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime dateNeeded,
      @Parameter(name = "done", description = "true if the request has been completed")
          @RequestParam
          boolean done) {

    RecommendationRequest recommendationRequest = new RecommendationRequest();
    recommendationRequest.setRequesterEmail(requesterEmail);
    recommendationRequest.setProfessorEmail(professorEmail);
    recommendationRequest.setExplanation(explanation);
    recommendationRequest.setDateRequested(dateRequested);
    recommendationRequest.setDateNeeded(dateNeeded);
    recommendationRequest.setDone(done);

    return recommendationRequestRepository.save(recommendationRequest);
  }

  /**
   * Get a single recommendation request by id
   *
   * @param id the id of the recommendation request
   * @return the recommendation request with the given id
   */
  @Operation(
      summary = "Get a single recommendation request",
      description = "Returns the recommendation request with the given id, or a 404 if none exists")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("")
  public RecommendationRequest getById(
      @Parameter(name = "id", description = "Database id of the recommendation request, e.g. 7")
          @RequestParam
          Long id) {
    RecommendationRequest recommendationRequest =
        recommendationRequestRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(RecommendationRequest.class, id));

    return recommendationRequest;
  }

  @Operation(
      summary = "Update a single recommendation request",
      description =
          "Replaces every field except id of the recommendation request with the given id, using"
              + " the JSON body. Returns a 404 if no recommendation request has that id.")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PutMapping("")
  public RecommendationRequest updateRecommendationRequest(
      @Parameter(name = "id", description = "Database id of the recommendation request, e.g. 7")
          @RequestParam
          Long id,
      @RequestBody @Valid RecommendationRequest incoming) {

    RecommendationRequest recommendationRequest =
        recommendationRequestRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(RecommendationRequest.class, id));

    recommendationRequest.setRequesterEmail(incoming.getRequesterEmail());
    recommendationRequest.setProfessorEmail(incoming.getProfessorEmail());
    recommendationRequest.setExplanation(incoming.getExplanation());
    recommendationRequest.setDateRequested(incoming.getDateRequested());
    recommendationRequest.setDateNeeded(incoming.getDateNeeded());
    recommendationRequest.setDone(incoming.getDone());

    recommendationRequestRepository.save(recommendationRequest);

    return recommendationRequest;
  }
}
