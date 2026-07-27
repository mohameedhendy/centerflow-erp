package com.centerflow.academic.batchsession.application;

import com.centerflow.academic.batch.domain.Batch;
import com.centerflow.academic.batch.domain.BatchStatus;
import com.centerflow.academic.batch.repository.BatchRepository;
import com.centerflow.academic.batchschedule.domain.BatchSchedule;
import com.centerflow.academic.batchschedule.repository.BatchScheduleRepository;
import com.centerflow.academic.batchsession.domain.BatchSession;
import com.centerflow.academic.batchsession.domain.BatchSessionStatus;
import com.centerflow.academic.batchsession.repository.BatchSessionRepository;
import com.centerflow.academic.classroom.repository.ClassroomRepository;
import com.centerflow.academic.common.exception.BatchNotFoundException;
import com.centerflow.academic.common.exception.BatchSessionNotFoundException;
import com.centerflow.academic.common.exception.ClassroomNotFoundException;
import com.centerflow.academic.common.exception.InstructorNotFoundException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import com.centerflow.academic.common.exception.InvalidSessionConfigurationException;
import com.centerflow.academic.common.exception.InvalidSessionStatusTransitionException;
import com.centerflow.academic.common.exception.SessionConfigurationLockedException;
import com.centerflow.academic.common.exception.SessionConflictException;
import com.centerflow.academic.instructor.repository.InstructorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class BatchSessionService {

    private static final int MAXIMUM_PAGE_SIZE = 100;
    private static final long MAXIMUM_GENERATION_DAYS = 366;

    private final BatchSessionRepository sessionRepository;
    private final BatchScheduleRepository scheduleRepository;
    private final BatchRepository batchRepository;
    private final ClassroomRepository classroomRepository;
    private final InstructorRepository instructorRepository;
    private final Clock clock;

    public BatchSessionService(
            BatchSessionRepository sessionRepository,
            BatchScheduleRepository scheduleRepository,
            BatchRepository batchRepository,
            ClassroomRepository classroomRepository,
            InstructorRepository instructorRepository,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.scheduleRepository = scheduleRepository;
        this.batchRepository = batchRepository;
        this.classroomRepository = classroomRepository;
        this.instructorRepository = instructorRepository;
        this.clock = clock;
    }

    @Transactional
    public BatchSessionResult create(
            UUID batchId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String topic
    ) {
        validateTimeRange(startTime, endTime);

        Batch batch = findSchedulableBatch(batchId);

        validateDateWithinBatch(batch, sessionDate);
        lockSchedulingResources(batch);

        validateConflicts(
                batch,
                sessionDate,
                startTime,
                endTime,
                null
        );

        BatchSession session = BatchSession.create(
                batchId,
                null,
                sessionDate,
                startTime,
                endTime,
                topic,
                Instant.now(clock)
        );

        return saveSession(session);
    }

    @Transactional
    public SessionGenerationResult generate(
            UUID batchId,
            LocalDate requestedDateFrom,
            LocalDate requestedDateTo
    ) {
        Batch batch = findSchedulableBatch(batchId);

        LocalDate dateFrom = requestedDateFrom == null
                ? batch.getStartDate()
                : requestedDateFrom;

        LocalDate dateTo = requestedDateTo == null
                ? batch.getEndDate()
                : requestedDateTo;

        validateGenerationRange(
                batch,
                dateFrom,
                dateTo
        );

        List<BatchSchedule> schedules =
                scheduleRepository
                        .findAllByBatchIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                                batchId
                        );

        if (schedules.isEmpty()) {
            throw new InvalidSessionConfigurationException(
                    "Batch has no active weekly schedules"
            );
        }

        lockSchedulingResources(batch);

        int generatedCount = 0;
        int skippedCount = 0;

        LocalDate currentDate = dateFrom;

        while (!currentDate.isAfter(dateTo)) {
            for (BatchSchedule schedule : schedules) {
                if (currentDate.getDayOfWeek()
                        != schedule.getDayOfWeek()) {
                    continue;
                }

                boolean exists =
                        sessionRepository
                                .existsByBatchIdAndSessionDateAndStartTimeAndEndTime(
                                        batchId,
                                        currentDate,
                                        schedule.getStartTime(),
                                        schedule.getEndTime()
                                );

                if (exists) {
                    skippedCount++;
                    continue;
                }

                validateConflicts(
                        batch,
                        currentDate,
                        schedule.getStartTime(),
                        schedule.getEndTime(),
                        null
                );

                BatchSession session =
                        BatchSession.create(
                                batchId,
                                schedule.getId(),
                                currentDate,
                                schedule.getStartTime(),
                                schedule.getEndTime(),
                                null,
                                Instant.now(clock)
                        );

                sessionRepository.save(session);
                generatedCount++;
            }

            currentDate = currentDate.plusDays(1);
        }

        sessionRepository.flush();

        return new SessionGenerationResult(
                generatedCount,
                skippedCount
        );
    }

    @Transactional(readOnly = true)
    public BatchSessionResult getById(
            UUID sessionId
    ) {
        return BatchSessionResult.from(
                findSession(sessionId)
        );
    }

    @Transactional(readOnly = true)
    public BatchSessionPageResult search(
            UUID batchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            BatchSessionStatus status,
            int page,
            int size
    ) {
        validatePagination(page, size);

        if (!batchRepository.existsById(batchId)) {
            throw new BatchNotFoundException(batchId);
        }

        if (dateFrom != null
                && dateTo != null
                && dateFrom.isAfter(dateTo)) {
            throw new InvalidSessionConfigurationException(
                    "Date from must not be after date to"
            );
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("sessionDate"),
                        Sort.Order.asc("startTime")
                )
        );

        Page<BatchSession> sessionPage =
                sessionRepository.search(
                        batchId,
                        dateFrom,
                        dateTo,
                        status,
                        pageRequest
                );

        List<BatchSessionResult> content =
                sessionPage.getContent()
                        .stream()
                        .map(BatchSessionResult::from)
                        .toList();

        return BatchSessionPageResult.from(
                sessionPage,
                content
        );
    }

    @Transactional
    public BatchSessionResult update(
            UUID sessionId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String topic
    ) {
        validateTimeRange(startTime, endTime);

        BatchSession session = findSession(sessionId);

        if (!session.getStatus().canBeEdited()) {
            throw new SessionConfigurationLockedException();
        }

        Batch batch =
                findSchedulableBatch(
                        session.getBatchId()
                );

        validateDateWithinBatch(batch, sessionDate);

        if (session.getStatus()
                != BatchSessionStatus.CANCELLED) {
            lockSchedulingResources(batch);

            validateConflicts(
                    batch,
                    sessionDate,
                    startTime,
                    endTime,
                    sessionId
            );
        }

        session.updateDetails(
                sessionDate,
                startTime,
                endTime,
                topic,
                Instant.now(clock)
        );

        try {
            sessionRepository.flush();
            return BatchSessionResult.from(session);

        } catch (DataIntegrityViolationException exception) {
            throw new SessionConflictException(
                    "An identical session already exists for this batch",
                    exception
            );
        }
    }

    @Transactional
    public BatchSessionResult changeStatus(
            UUID sessionId,
            BatchSessionStatus targetStatus
    ) {
        BatchSession session = findSession(sessionId);

        if (!session.getStatus()
                .canTransitionTo(targetStatus)) {
            throw new InvalidSessionStatusTransitionException(
                    session.getStatus(),
                    targetStatus
            );
        }

        Batch batch =
                findBatchForUpdate(
                        session.getBatchId()
                );

        if (targetStatus == BatchSessionStatus.COMPLETED
                && batch.getStatus()
                != BatchStatus.IN_PROGRESS) {
            throw new InvalidSessionConfigurationException(
                    "A session can only be completed while the batch is IN_PROGRESS"
            );
        }

        if (targetStatus == BatchSessionStatus.PLANNED
                && session.getStatus()
                == BatchSessionStatus.CANCELLED) {

            validateBatchIsNotFinished(batch);
            lockSchedulingResources(batch);

            validateConflicts(
                    batch,
                    session.getSessionDate(),
                    session.getStartTime(),
                    session.getEndTime(),
                    sessionId
            );
        }

        session.changeStatus(
                targetStatus,
                Instant.now(clock)
        );

        return BatchSessionResult.from(session);
    }

    private BatchSessionResult saveSession(
            BatchSession session
    ) {
        try {
            BatchSession savedSession =
                    sessionRepository.saveAndFlush(
                            session
                    );

            return BatchSessionResult.from(
                    savedSession
            );

        } catch (DataIntegrityViolationException exception) {
            throw new SessionConflictException(
                    "An identical session already exists for this batch",
                    exception
            );
        }
    }

    private Batch findSchedulableBatch(
            UUID batchId
    ) {
        Batch batch = findBatchForUpdate(batchId);
        validateBatchIsNotFinished(batch);
        return batch;
    }

    private Batch findBatchForUpdate(
            UUID batchId
    ) {
        return batchRepository
                .findByIdForUpdate(batchId)
                .orElseThrow(
                        () -> new BatchNotFoundException(
                                batchId
                        )
                );
    }

    private void validateBatchIsNotFinished(
            Batch batch
    ) {
        if (batch.getStatus() == BatchStatus.COMPLETED
                || batch.getStatus()
                == BatchStatus.CANCELLED) {
            throw new InvalidSessionConfigurationException(
                    "Sessions cannot be changed for a batch with status "
                            + batch.getStatus()
            );
        }
    }

    private BatchSession findSession(
            UUID sessionId
    ) {
        return sessionRepository
                .findById(sessionId)
                .orElseThrow(
                        () -> new BatchSessionNotFoundException(
                                sessionId
                        )
                );
    }

    private void lockSchedulingResources(
            Batch batch
    ) {
        classroomRepository
                .findByIdForUpdate(
                        batch.getClassroomId()
                )
                .orElseThrow(
                        () -> new ClassroomNotFoundException(
                                batch.getClassroomId()
                        )
                );

        instructorRepository
                .findByIdForUpdate(
                        batch.getInstructorId()
                )
                .orElseThrow(
                        () -> new InstructorNotFoundException(
                                batch.getInstructorId()
                        )
                );
    }

    private void validateConflicts(
            Batch batch,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            UUID excludedSessionId
    ) {
        long classroomConflicts =
                sessionRepository
                        .countClassroomConflicts(
                                batch.getClassroomId(),
                                sessionDate,
                                startTime,
                                endTime,
                                excludedSessionId
                        );

        if (classroomConflicts > 0) {
            throw new SessionConflictException(
                    "Classroom "
                            + batch.getClassroomId()
                            + " already has a session on "
                            + sessionDate
                            + " during the requested time"
            );
        }

        long instructorConflicts =
                sessionRepository
                        .countInstructorConflicts(
                                batch.getInstructorId(),
                                sessionDate,
                                startTime,
                                endTime,
                                excludedSessionId
                        );

        if (instructorConflicts > 0) {
            throw new SessionConflictException(
                    "Instructor "
                            + batch.getInstructorId()
                            + " already has a session on "
                            + sessionDate
                            + " during the requested time"
            );
        }
    }

    private void validateDateWithinBatch(
            Batch batch,
            LocalDate sessionDate
    ) {
        if (sessionDate == null) {
            throw new InvalidSessionConfigurationException(
                    "Session date is required"
            );
        }

        if (sessionDate.isBefore(batch.getStartDate())
                || sessionDate.isAfter(batch.getEndDate())) {
            throw new InvalidSessionConfigurationException(
                    "Session date must be within the batch period"
            );
        }
    }

    private void validateGenerationRange(
            Batch batch,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (dateFrom.isAfter(dateTo)) {
            throw new InvalidSessionConfigurationException(
                    "Generation date from must not be after date to"
            );
        }

        if (dateFrom.isBefore(batch.getStartDate())
                || dateTo.isAfter(batch.getEndDate())) {
            throw new InvalidSessionConfigurationException(
                    "Generation range must be within the batch period"
            );
        }

        long numberOfDays =
                ChronoUnit.DAYS.between(
                        dateFrom,
                        dateTo
                ) + 1;

        if (numberOfDays > MAXIMUM_GENERATION_DAYS) {
            throw new InvalidSessionConfigurationException(
                    "Session generation range must not exceed "
                            + MAXIMUM_GENERATION_DAYS
                            + " days"
            );
        }
    }

    private void validateTimeRange(
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (startTime == null || endTime == null) {
            throw new InvalidSessionConfigurationException(
                    "Session start time and end time are required"
            );
        }

        if (!endTime.isAfter(startTime)) {
            throw new InvalidSessionConfigurationException(
                    "Session end time must be after start time"
            );
        }
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidPaginationException(
                    "Page number must not be negative"
            );
        }

        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Page size must be between 1 and "
                            + MAXIMUM_PAGE_SIZE
            );
        }
    }
}