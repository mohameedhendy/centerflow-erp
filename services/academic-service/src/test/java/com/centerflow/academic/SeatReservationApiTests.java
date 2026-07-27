package com.centerflow.academic;

import com.centerflow.academic.seatreservation.domain.SeatReservationStatus;
import com.centerflow.academic.seatreservation.repository.SeatReservationRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
class SeatReservationApiTests {

    private static final String BRANCHES_URL =
            "/api/v1/academic/branches";

    private static final String CLASSROOMS_URL =
            "/api/v1/academic/classrooms";

    private static final String COURSES_URL =
            "/api/v1/academic/courses";

    private static final String LEVELS_URL =
            "/api/v1/academic/course-levels";

    private static final String INSTRUCTORS_URL =
            "/api/v1/academic/instructors";

    private static final String BATCHES_URL =
            "/api/v1/academic/batches";

    private static final String INTERNAL_URL =
            "/api/v1/academic/internal";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final SeatReservationRepository
            seatReservationRepository;

    @Autowired
    SeatReservationApiTests(
            MockMvc mockMvc,
            JdbcTemplate jdbcTemplate,
            SeatReservationRepository
                    seatReservationRepository
    ) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.seatReservationRepository =
                seatReservationRepository;
    }

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.update(
                "DELETE FROM seat_reservations"
        );

        jdbcTemplate.update(
                "DELETE FROM batch_sessions"
        );

        jdbcTemplate.update(
                "DELETE FROM batch_schedules"
        );

        jdbcTemplate.update(
                "DELETE FROM batches"
        );

        jdbcTemplate.update(
                "DELETE FROM instructors"
        );

        jdbcTemplate.update(
                "DELETE FROM course_levels"
        );

        jdbcTemplate.update(
                "DELETE FROM courses"
        );

        jdbcTemplate.update(
                "DELETE FROM classrooms"
        );

        jdbcTemplate.update(
                "DELETE FROM branches"
        );
    }

    @Test
    void rejectsReservationBeforeBatchOpens()
            throws Exception {

        AcademicResources resources =
                createResources("NOT-OPEN");

        UUID batchId = createBatch(
                "SEAT-NOT-OPEN",
                resources,
                2
        );

        UUID enrollmentId = UUID.randomUUID();

        mockMvc.perform(
                        post(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-reservations"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        reserveRequest(
                                                enrollmentId
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Seats can only be reserved while batch status is OPEN_FOR_ENROLLMENT. Current status: DRAFT"
                                )
                );
    }

    @Test
    void reservesSeatAndReportsAvailability()
            throws Exception {

        AcademicResources resources =
                createResources("RESERVE");

        UUID batchId = createBatch(
                "SEAT-RESERVE",
                resources,
                3
        );

        openBatch(batchId);

        UUID enrollmentId = UUID.randomUUID();

        UUID reservationId = reserveSeat(
                batchId,
                enrollmentId
        );

        mockMvc.perform(
                        get(
                                INTERNAL_URL
                                        + "/seat-reservations/"
                                        + reservationId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        reservationId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.batchId")
                                .value(batchId.toString())
                )
                .andExpect(
                        jsonPath("$.enrollmentId")
                                .value(
                                        enrollmentId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("RESERVED")
                )
                .andExpect(
                        jsonPath("$.reservedAt")
                                .isNotEmpty()
                );

        mockMvc.perform(
                        get(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-availability"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.batchStatus")
                                .value(
                                        "OPEN_FOR_ENROLLMENT"
                                )
                )
                .andExpect(
                        jsonPath("$.capacity")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.reservedSeats")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.availableSeats")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.full")
                                .value(false)
                );
    }

    @Test
    void repeatedReservationIsIdempotent()
            throws Exception {

        AcademicResources resources =
                createResources("IDEMPOTENT");

        UUID batchId = createBatch(
                "SEAT-IDEMPOTENT",
                resources,
                2
        );

        openBatch(batchId);

        UUID enrollmentId = UUID.randomUUID();

        UUID firstReservationId = reserveSeat(
                batchId,
                enrollmentId
        );

        mockMvc.perform(
                        post(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-reservations"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        reserveRequest(
                                                enrollmentId
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        firstReservationId
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("RESERVED")
                );

        long reservedSeats =
                seatReservationRepository
                        .countByBatchIdAndStatus(
                                batchId,
                                SeatReservationStatus.RESERVED
                        );

        assertThat(reservedSeats).isEqualTo(1);
        assertThat(
                seatReservationRepository.count()
        ).isEqualTo(1);
    }

    @Test
    void rejectsReservationWhenBatchIsFull()
            throws Exception {

        AcademicResources resources =
                createResources("FULL");

        UUID batchId = createBatch(
                "SEAT-FULL",
                resources,
                1
        );

        openBatch(batchId);

        reserveSeat(
                batchId,
                UUID.randomUUID()
        );

        UUID secondEnrollmentId =
                UUID.randomUUID();

        mockMvc.perform(
                        post(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-reservations"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        reserveRequest(
                                                secondEnrollmentId
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Batch has no available seats: "
                                                + batchId
                                )
                );

        mockMvc.perform(
                        get(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-availability"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.reservedSeats")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.availableSeats")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.full")
                                .value(true)
                );
    }

    @Test
    void repeatedReleaseIsIdempotent()
            throws Exception {

        AcademicResources resources =
                createResources("RELEASE");

        UUID batchId = createBatch(
                "SEAT-RELEASE",
                resources,
                1
        );

        openBatch(batchId);

        UUID reservationId = reserveSeat(
                batchId,
                UUID.randomUUID()
        );

        releaseSeat(reservationId);

        mockMvc.perform(
                        post(
                                INTERNAL_URL
                                        + "/seat-reservations/"
                                        + reservationId
                                        + "/release"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        reservationId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("RELEASED")
                )
                .andExpect(
                        jsonPath("$.releasedAt")
                                .isNotEmpty()
                );

        mockMvc.perform(
                        get(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-availability"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.reservedSeats")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.availableSeats")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.full")
                                .value(false)
                );
    }

    @Test
    void reservesReleasedSeatAgainUsingSameRecord()
            throws Exception {

        AcademicResources resources =
                createResources("RERESERVE");

        UUID batchId = createBatch(
                "SEAT-RERESERVE",
                resources,
                1
        );

        openBatch(batchId);

        UUID enrollmentId = UUID.randomUUID();

        UUID originalReservationId =
                reserveSeat(
                        batchId,
                        enrollmentId
                );

        releaseSeat(originalReservationId);

        mockMvc.perform(
                        post(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-reservations"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        reserveRequest(
                                                enrollmentId
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        originalReservationId
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("RESERVED")
                );

        assertThat(
                seatReservationRepository.count()
        ).isEqualTo(1);

        assertThat(
                seatReservationRepository
                        .countByBatchIdAndStatus(
                                batchId,
                                SeatReservationStatus.RESERVED
                        )
        ).isEqualTo(1);
    }

    @Test
    void preventsReducingCapacityBelowReservedSeats()
            throws Exception {

        AcademicResources resources =
                createResources("CAPACITY");

        UUID batchId = createBatch(
                "SEAT-CAPACITY",
                resources,
                3
        );

        openBatch(batchId);

        reserveSeat(
                batchId,
                UUID.randomUUID()
        );

        reserveSeat(
                batchId,
                UUID.randomUUID()
        );

        mockMvc.perform(
                        put(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        updateBatchRequest(
                                                "Capacity Protected Batch",
                                                resources,
                                                1
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Batch capacity cannot be less than reserved seats. Reserved seats: 2"
                                )
                );
    }

    @Test
    void returnsNotFoundForMissingReservation()
            throws Exception {

        UUID reservationId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                INTERNAL_URL
                                        + "/seat-reservations/"
                                        + reservationId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Seat reservation was not found: "
                                                + reservationId
                                )
                );
    }

    @Test
    void allowsOnlyOneConcurrentReservationForLastSeat()
            throws Exception {

        String suffix = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        AcademicResources resources =
                createResources(
                        "CON-" + suffix
                );

        UUID batchId = createBatch(
                "SEAT-CON-" + suffix,
                resources,
                1
        );

        openBatch(batchId);

        UUID firstEnrollmentId =
                UUID.randomUUID();

        UUID secondEnrollmentId =
                UUID.randomUUID();

        ExecutorService executorService =
                Executors.newFixedThreadPool(2);

        CountDownLatch readyLatch =
                new CountDownLatch(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        try {
            Future<Integer> firstResult =
                    executorService.submit(
                            () -> reserveConcurrently(
                                    batchId,
                                    firstEnrollmentId,
                                    readyLatch,
                                    startLatch
                            )
                    );

            Future<Integer> secondResult =
                    executorService.submit(
                            () -> reserveConcurrently(
                                    batchId,
                                    secondEnrollmentId,
                                    readyLatch,
                                    startLatch
                            )
                    );

            assertThat(
                    readyLatch.await(
                            5,
                            TimeUnit.SECONDS
                    )
            ).isTrue();

            startLatch.countDown();

            List<Integer> responseStatuses =
                    List.of(
                            firstResult.get(
                                    15,
                                    TimeUnit.SECONDS
                            ),
                            secondResult.get(
                                    15,
                                    TimeUnit.SECONDS
                            )
                    );

            assertThat(responseStatuses)
                    .containsExactlyInAnyOrder(
                            200,
                            409
                    );

        } finally {
            executorService.shutdownNow();

            executorService.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            );
        }

        assertThat(
                seatReservationRepository
                        .countByBatchIdAndStatus(
                                batchId,
                                SeatReservationStatus.RESERVED
                        )
        ).isEqualTo(1);

        mockMvc.perform(
                        get(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-availability"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.reservedSeats")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.availableSeats")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.full")
                                .value(true)
                );
    }

    private int reserveConcurrently(
            UUID batchId,
            UUID enrollmentId,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) throws Exception {

        readyLatch.countDown();

        boolean started = startLatch.await(
                5,
                TimeUnit.SECONDS
        );

        if (!started) {
            throw new IllegalStateException(
                    "Concurrent reservation test did not start in time"
            );
        }

        return mockMvc.perform(
                        post(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-reservations"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        reserveRequest(
                                                enrollmentId
                                        )
                                )
                )
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private AcademicResources createResources(
            String suffix
    ) throws Exception {

        UUID branchId = createBranch(
                "BR-" + suffix,
                suffix + " Branch"
        );

        UUID classroomId = createClassroom(
                branchId,
                "ROOM-" + suffix,
                50
        );

        UUID courseId = createCourse(
                "COURSE-" + suffix,
                suffix + " Course"
        );

        UUID courseLevelId = createCourseLevel(
                courseId,
                "LEVEL-" + suffix
        );

        UUID instructorId = createInstructor(
                "INS-" + suffix,
                suffix.toLowerCase()
                        + "@centerflow.com"
        );

        return new AcademicResources(
                branchId,
                classroomId,
                courseLevelId,
                instructorId
        );
    }

    private UUID createBranch(
            String code,
            String name
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
                                          "city": "Cairo"
                                        }
                                        """.formatted(
                                                code,
                                                name
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createClassroom(
            UUID branchId,
            String code,
            int capacity
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(CLASSROOMS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "branchId": "%s",
                                          "code": "%s",
                                          "name": "Seat Test Classroom",
                                          "capacity": %d,
                                          "floor": "First Floor"
                                        }
                                        """.formatted(
                                                branchId,
                                                code,
                                                capacity
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createCourse(
            String code,
            String name
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(COURSES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "%s",
                                          "name": "%s",
                                          "description": "Seat reservation test course"
                                        }
                                        """.formatted(
                                                code,
                                                name
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createCourseLevel(
            UUID courseId,
            String code
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(LEVELS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "courseId": "%s",
                                          "code": "%s",
                                          "name": "Seat Test Level",
                                          "sequenceNumber": 1,
                                          "durationHours": 60,
                                          "description": "Seat reservation test level"
                                        }
                                        """.formatted(
                                                courseId,
                                                code
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createInstructor(
            String code,
            String email
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(INSTRUCTORS_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "%s",
                                          "firstName": "Ahmed",
                                          "lastName": "Mohamed",
                                          "email": "%s",
                                          "phone": "+20 100 000 0000",
                                          "specialization": "Java Backend",
                                          "bio": "Seat reservation test instructor"
                                        }
                                        """.formatted(
                                                code,
                                                email
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private UUID createBatch(
            String code,
            AcademicResources resources,
            int capacity
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(BATCHES_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "%s",
                                          "name": "%s Batch",
                                          "branchId": "%s",
                                          "classroomId": "%s",
                                          "courseLevelId": "%s",
                                          "instructorId": "%s",
                                          "capacity": %d,
                                          "startDate": "2026-09-01",
                                          "endDate": "2026-12-31"
                                        }
                                        """.formatted(
                                                code,
                                                code,
                                                resources.branchId(),
                                                resources.classroomId(),
                                                resources.courseLevelId(),
                                                resources.instructorId(),
                                                capacity
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private void openBatch(
            UUID batchId
    ) throws Exception {

        mockMvc.perform(
                        patch(
                                BATCHES_URL
                                        + "/"
                                        + batchId
                                        + "/status"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "status": "OPEN_FOR_ENROLLMENT"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "OPEN_FOR_ENROLLMENT"
                                )
                );
    }

    private UUID reserveSeat(
            UUID batchId,
            UUID enrollmentId
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(
                                INTERNAL_URL
                                        + "/batches/"
                                        + batchId
                                        + "/seat-reservations"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        reserveRequest(
                                                enrollmentId
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("RESERVED")
                )
                .andReturn();

        return extractId(result);
    }

    private void releaseSeat(
            UUID reservationId
    ) throws Exception {

        mockMvc.perform(
                        post(
                                INTERNAL_URL
                                        + "/seat-reservations/"
                                        + reservationId
                                        + "/release"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("RELEASED")
                );
    }

    private String reserveRequest(
            UUID enrollmentId
    ) {
        return """
                {
                  "enrollmentId": "%s"
                }
                """.formatted(enrollmentId);
    }

    private String updateBatchRequest(
            String name,
            AcademicResources resources,
            int capacity
    ) {
        return """
                {
                  "name": "%s",
                  "branchId": "%s",
                  "classroomId": "%s",
                  "courseLevelId": "%s",
                  "instructorId": "%s",
                  "capacity": %d,
                  "startDate": "2026-09-01",
                  "endDate": "2026-12-31"
                }
                """.formatted(
                name,
                resources.branchId(),
                resources.classroomId(),
                resources.courseLevelId(),
                resources.instructorId(),
                capacity
        );
    }

    private UUID extractId(
            MvcResult result
    ) throws Exception {

        String id = JsonPath.read(
                result.getResponse()
                        .getContentAsString(),
                "$.id"
        );

        return UUID.fromString(id);
    }

    private record AcademicResources(
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId
    ) {
    }
}