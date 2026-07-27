package com.centerflow.academic.batch.application;

import com.centerflow.academic.batch.domain.Batch;
import com.centerflow.academic.batch.domain.BatchStatus;
import com.centerflow.academic.batch.repository.BatchRepository;
import com.centerflow.academic.branch.domain.Branch;
import com.centerflow.academic.branch.repository.BranchRepository;
import com.centerflow.academic.classroom.domain.Classroom;
import com.centerflow.academic.classroom.repository.ClassroomRepository;
import com.centerflow.academic.common.exception.AcademicResourceUnavailableException;
import com.centerflow.academic.common.exception.BatchConfigurationLockedException;
import com.centerflow.academic.common.exception.BatchNotFoundException;
import com.centerflow.academic.common.exception.BranchNotFoundException;
import com.centerflow.academic.common.exception.ClassroomNotFoundException;
import com.centerflow.academic.common.exception.CourseLevelNotFoundException;
import com.centerflow.academic.common.exception.CourseNotFoundException;
import com.centerflow.academic.common.exception.DuplicateBatchCodeException;
import com.centerflow.academic.common.exception.InstructorNotFoundException;
import com.centerflow.academic.common.exception.InvalidBatchConfigurationException;
import com.centerflow.academic.common.exception.InvalidBatchStatusTransitionException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import com.centerflow.academic.course.domain.Course;
import com.centerflow.academic.course.repository.CourseRepository;
import com.centerflow.academic.courselevel.domain.CourseLevel;
import com.centerflow.academic.courselevel.repository.CourseLevelRepository;
import com.centerflow.academic.instructor.domain.Instructor;
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
import java.util.List;
import java.util.UUID;

@Service
public class BatchService {

    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final BatchRepository batchRepository;
    private final BranchRepository branchRepository;
    private final ClassroomRepository classroomRepository;
    private final CourseLevelRepository courseLevelRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final Clock clock;

    public BatchService(
            BatchRepository batchRepository,
            BranchRepository branchRepository,
            ClassroomRepository classroomRepository,
            CourseLevelRepository courseLevelRepository,
            CourseRepository courseRepository,
            InstructorRepository instructorRepository,
            Clock clock
    ) {
        this.batchRepository = batchRepository;
        this.branchRepository = branchRepository;
        this.classroomRepository = classroomRepository;
        this.courseLevelRepository = courseLevelRepository;
        this.courseRepository = courseRepository;
        this.instructorRepository = instructorRepository;
        this.clock = clock;
    }

    @Transactional
    public BatchResult create(
            String code,
            String name,
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId,
            int capacity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String normalizedCode =
                Batch.normalizedCode(code);

        if (batchRepository.existsByCode(
                normalizedCode
        )) {
            throw new DuplicateBatchCodeException(
                    normalizedCode
            );
        }

        validateConfiguration(
                branchId,
                classroomId,
                courseLevelId,
                instructorId,
                capacity,
                startDate,
                endDate
        );

        Batch batch = Batch.create(
                normalizedCode,
                name,
                branchId,
                classroomId,
                courseLevelId,
                instructorId,
                capacity,
                startDate,
                endDate,
                Instant.now(clock)
        );

        try {
            Batch savedBatch =
                    batchRepository.saveAndFlush(batch);

            return BatchResult.from(savedBatch);

        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateBatchCodeException(
                    normalizedCode,
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public BatchResult getById(UUID batchId) {
        return BatchResult.from(
                findBatch(batchId)
        );
    }

    @Transactional(readOnly = true)
    public BatchPageResult search(
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId,
            BatchStatus status,
            String search,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            int page,
            int size
    ) {
        validatePagination(page, size);

        if (startDateFrom != null
                && startDateTo != null
                && startDateFrom.isAfter(startDateTo)) {
            throw new InvalidBatchConfigurationException(
                    "Start date from must not be after start date to"
            );
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("startDate"),
                        Sort.Order.asc("code")
                )
        );

        Page<Batch> batchPage =
                batchRepository.search(
                        branchId,
                        classroomId,
                        courseLevelId,
                        instructorId,
                        status,
                        normalizeFilter(search),
                        startDateFrom,
                        startDateTo,
                        pageRequest
                );

        List<BatchResult> content =
                batchPage.getContent()
                        .stream()
                        .map(BatchResult::from)
                        .toList();

        return BatchPageResult.from(
                batchPage,
                content
        );
    }

    @Transactional
    public BatchResult update(
            UUID batchId,
            String name,
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId,
            int capacity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Batch batch = findBatch(batchId);

        if (!batch.getStatus()
                .allowsConfigurationChanges()) {
            throw new BatchConfigurationLockedException(
                    batch.getStatus()
            );
        }

        validateConfiguration(
                branchId,
                classroomId,
                courseLevelId,
                instructorId,
                capacity,
                startDate,
                endDate
        );

        batch.reconfigure(
                name,
                branchId,
                classroomId,
                courseLevelId,
                instructorId,
                capacity,
                startDate,
                endDate,
                Instant.now(clock)
        );

        return BatchResult.from(batch);
    }

    @Transactional
    public BatchResult changeStatus(
            UUID batchId,
            BatchStatus targetStatus
    ) {
        Batch batch = findBatch(batchId);

        if (!batch.getStatus()
                .canTransitionTo(targetStatus)) {
            throw new InvalidBatchStatusTransitionException(
                    batch.getStatus(),
                    targetStatus
            );
        }

        if (targetStatus == BatchStatus.OPEN_FOR_ENROLLMENT
                || targetStatus == BatchStatus.IN_PROGRESS) {

            validateConfiguration(
                    batch.getBranchId(),
                    batch.getClassroomId(),
                    batch.getCourseLevelId(),
                    batch.getInstructorId(),
                    batch.getCapacity(),
                    batch.getStartDate(),
                    batch.getEndDate()
            );
        }

        batch.changeStatus(
                targetStatus,
                Instant.now(clock)
        );

        return BatchResult.from(batch);
    }

    private void validateConfiguration(
            UUID branchId,
            UUID classroomId,
            UUID courseLevelId,
            UUID instructorId,
            int capacity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validatePeriod(startDate, endDate);

        Branch branch = branchRepository
                .findById(branchId)
                .orElseThrow(
                        () -> new BranchNotFoundException(
                                branchId
                        )
                );

        if (!branch.isActive()) {
            throw new AcademicResourceUnavailableException(
                    "Branch is inactive: " + branchId
            );
        }

        Classroom classroom = classroomRepository
                .findById(classroomId)
                .orElseThrow(
                        () -> new ClassroomNotFoundException(
                                classroomId
                        )
                );

        if (!classroom.isActive()) {
            throw new AcademicResourceUnavailableException(
                    "Classroom is inactive: "
                            + classroomId
            );
        }

        if (!classroom.getBranchId().equals(branchId)) {
            throw new InvalidBatchConfigurationException(
                    "Classroom "
                            + classroomId
                            + " does not belong to branch "
                            + branchId
            );
        }

        if (capacity > classroom.getCapacity()) {
            throw new InvalidBatchConfigurationException(
                    "Batch capacity "
                            + capacity
                            + " exceeds classroom capacity "
                            + classroom.getCapacity()
            );
        }

        CourseLevel courseLevel =
                courseLevelRepository
                        .findById(courseLevelId)
                        .orElseThrow(
                                () -> new CourseLevelNotFoundException(
                                        courseLevelId
                                )
                        );

        if (!courseLevel.isActive()) {
            throw new AcademicResourceUnavailableException(
                    "Course level is inactive: "
                            + courseLevelId
            );
        }

        Course course = courseRepository
                .findById(courseLevel.getCourseId())
                .orElseThrow(
                        () -> new CourseNotFoundException(
                                courseLevel.getCourseId()
                        )
                );

        if (!course.isActive()) {
            throw new AcademicResourceUnavailableException(
                    "Course is inactive: "
                            + course.getId()
            );
        }

        Instructor instructor =
                instructorRepository
                        .findById(instructorId)
                        .orElseThrow(
                                () -> new InstructorNotFoundException(
                                        instructorId
                                )
                        );

        if (!instructor.isActive()) {
            throw new AcademicResourceUnavailableException(
                    "Instructor is inactive: "
                            + instructorId
            );
        }
    }

    private Batch findBatch(UUID batchId) {
        return batchRepository
                .findById(batchId)
                .orElseThrow(
                        () -> new BatchNotFoundException(
                                batchId
                        )
                );
    }

    private void validatePeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new InvalidBatchConfigurationException(
                    "Batch start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new InvalidBatchConfigurationException(
                    "Batch end date must not be before start date"
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

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}