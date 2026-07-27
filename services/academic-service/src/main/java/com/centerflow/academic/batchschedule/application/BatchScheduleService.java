package com.centerflow.academic.batchschedule.application;

import com.centerflow.academic.batch.domain.Batch;
import com.centerflow.academic.batch.repository.BatchRepository;
import com.centerflow.academic.batchschedule.domain.BatchSchedule;
import com.centerflow.academic.batchschedule.repository.BatchScheduleRepository;
import com.centerflow.academic.classroom.repository.ClassroomRepository;
import com.centerflow.academic.common.exception.BatchConfigurationLockedException;
import com.centerflow.academic.common.exception.BatchNotFoundException;
import com.centerflow.academic.common.exception.BatchScheduleNotFoundException;
import com.centerflow.academic.common.exception.ClassroomNotFoundException;
import com.centerflow.academic.common.exception.InstructorNotFoundException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import com.centerflow.academic.common.exception.InvalidScheduleConfigurationException;
import com.centerflow.academic.common.exception.ScheduleConflictException;
import com.centerflow.academic.instructor.repository.InstructorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class BatchScheduleService {

    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final BatchScheduleRepository scheduleRepository;
    private final BatchRepository batchRepository;
    private final ClassroomRepository classroomRepository;
    private final InstructorRepository instructorRepository;
    private final Clock clock;

    public BatchScheduleService(
            BatchScheduleRepository scheduleRepository,
            BatchRepository batchRepository,
            ClassroomRepository classroomRepository,
            InstructorRepository instructorRepository,
            Clock clock
    ) {
        this.scheduleRepository = scheduleRepository;
        this.batchRepository = batchRepository;
        this.classroomRepository = classroomRepository;
        this.instructorRepository = instructorRepository;
        this.clock = clock;
    }

    @Transactional
    public BatchScheduleResult create(
            UUID batchId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        validateTimeRange(startTime, endTime);

        Batch batch = findEditableBatch(batchId);

        lockSchedulingResources(batch);

        validateConflicts(
                batch,
                dayOfWeek,
                startTime,
                endTime,
                null
        );

        BatchSchedule schedule =
                BatchSchedule.create(
                        batchId,
                        dayOfWeek,
                        startTime,
                        endTime,
                        Instant.now(clock)
                );

        try {
            BatchSchedule savedSchedule =
                    scheduleRepository
                            .saveAndFlush(schedule);

            return BatchScheduleResult.from(
                    savedSchedule
            );

        } catch (DataIntegrityViolationException exception) {
            throw new ScheduleConflictException(
                    "An identical schedule already exists for this batch",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public BatchScheduleResult getById(
            UUID scheduleId
    ) {
        return BatchScheduleResult.from(
                findSchedule(scheduleId)
        );
    }

    @Transactional(readOnly = true)
    public BatchSchedulePageResult search(
            UUID batchId,
            DayOfWeek dayOfWeek,
            Boolean active,
            int page,
            int size
    ) {
        validatePagination(page, size);

        if (!batchRepository.existsById(batchId)) {
            throw new BatchNotFoundException(batchId);
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("dayOfWeek"),
                        Sort.Order.asc("startTime")
                )
        );

        Page<BatchSchedule> schedulePage =
                scheduleRepository.search(
                        batchId,
                        dayOfWeek,
                        active,
                        pageRequest
                );

        List<BatchScheduleResult> content =
                schedulePage.getContent()
                        .stream()
                        .map(BatchScheduleResult::from)
                        .toList();

        return BatchSchedulePageResult.from(
                schedulePage,
                content
        );
    }

    @Transactional
    public BatchScheduleResult update(
            UUID scheduleId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        validateTimeRange(startTime, endTime);

        BatchSchedule schedule =
                findSchedule(scheduleId);

        Batch batch =
                findEditableBatch(
                        schedule.getBatchId()
                );

        lockSchedulingResources(batch);

        if (schedule.isActive()) {
            validateConflicts(
                    batch,
                    dayOfWeek,
                    startTime,
                    endTime,
                    scheduleId
            );
        }

        schedule.update(
                dayOfWeek,
                startTime,
                endTime,
                Instant.now(clock)
        );

        try {
            scheduleRepository.flush();

            return BatchScheduleResult.from(schedule);

        } catch (DataIntegrityViolationException exception) {
            throw new ScheduleConflictException(
                    "An identical schedule already exists for this batch",
                    exception
            );
        }
    }

    @Transactional
    public BatchScheduleResult changeStatus(
            UUID scheduleId,
            boolean active
    ) {
        BatchSchedule schedule =
                findSchedule(scheduleId);

        Batch batch =
                findEditableBatch(
                        schedule.getBatchId()
                );

        lockSchedulingResources(batch);

        if (active && !schedule.isActive()) {
            validateConflicts(
                    batch,
                    schedule.getDayOfWeek(),
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    scheduleId
            );
        }

        schedule.changeStatus(
                active,
                Instant.now(clock)
        );

        return BatchScheduleResult.from(schedule);
    }

    private Batch findEditableBatch(UUID batchId) {
        Batch batch = batchRepository
                .findByIdForUpdate(batchId)
                .orElseThrow(
                        () -> new BatchNotFoundException(
                                batchId
                        )
                );

        if (!batch.getStatus()
                .allowsConfigurationChanges()) {
            throw new BatchConfigurationLockedException(
                    batch.getStatus()
            );
        }

        return batch;
    }

    private BatchSchedule findSchedule(
            UUID scheduleId
    ) {
        return scheduleRepository
                .findById(scheduleId)
                .orElseThrow(
                        () -> new BatchScheduleNotFoundException(
                                scheduleId
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
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            UUID excludedScheduleId
    ) {
        boolean classroomConflict =
                scheduleRepository
                        .existsClassroomConflict(
                                batch.getClassroomId(),
                                dayOfWeek.name(),
                                startTime,
                                endTime,
                                batch.getStartDate(),
                                batch.getEndDate(),
                                excludedScheduleId
                        );

        if (classroomConflict) {
            throw new ScheduleConflictException(
                    "Classroom "
                            + batch.getClassroomId()
                            + " is already scheduled on "
                            + dayOfWeek
                            + " during the requested time"
            );
        }

        boolean instructorConflict =
                scheduleRepository
                        .existsInstructorConflict(
                                batch.getInstructorId(),
                                dayOfWeek.name(),
                                startTime,
                                endTime,
                                batch.getStartDate(),
                                batch.getEndDate(),
                                excludedScheduleId
                        );

        if (instructorConflict) {
            throw new ScheduleConflictException(
                    "Instructor "
                            + batch.getInstructorId()
                            + " is already scheduled on "
                            + dayOfWeek
                            + " during the requested time"
            );
        }
    }

    private void validateTimeRange(
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (startTime == null || endTime == null) {
            throw new InvalidScheduleConfigurationException(
                    "Schedule start time and end time are required"
            );
        }

        if (!endTime.isAfter(startTime)) {
            throw new InvalidScheduleConfigurationException(
                    "Schedule end time must be after start time"
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