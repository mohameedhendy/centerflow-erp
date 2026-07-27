package com.centerflow.academic.attendance.application;

import com.centerflow.academic.attendance.domain.AttendanceRecord;
import com.centerflow.academic.attendance.domain.AttendanceStatus;
import com.centerflow.academic.attendance.repository.AttendanceRecordRepository;
import com.centerflow.academic.batch.repository.BatchRepository;
import com.centerflow.academic.batchsession.domain.BatchSession;
import com.centerflow.academic.batchsession.domain.BatchSessionStatus;
import com.centerflow.academic.batchsession.repository.BatchSessionRepository;
import com.centerflow.academic.common.exception.AttendanceConflictException;
import com.centerflow.academic.common.exception.AttendanceNotAllowedException;
import com.centerflow.academic.common.exception.AttendanceStudentMismatchException;
import com.centerflow.academic.common.exception.BatchNotFoundException;
import com.centerflow.academic.common.exception.BatchSessionNotFoundException;
import com.centerflow.academic.common.exception.InvalidAttendanceConfigurationException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AttendanceService {

    private static final int MAXIMUM_PAGE_SIZE = 100;
    private static final int MAXIMUM_RECORDS_PER_REQUEST = 500;

    private final AttendanceRecordRepository
            attendanceRecordRepository;

    private final BatchSessionRepository
            batchSessionRepository;

    private final BatchRepository batchRepository;
    private final Clock clock;

    public AttendanceService(
            AttendanceRecordRepository
                    attendanceRecordRepository,
            BatchSessionRepository
                    batchSessionRepository,
            BatchRepository batchRepository,
            Clock clock
    ) {
        this.attendanceRecordRepository =
                attendanceRecordRepository;

        this.batchSessionRepository =
                batchSessionRepository;

        this.batchRepository = batchRepository;
        this.clock = clock;
    }

    @Transactional
    public AttendanceMarkingResult markAttendance(
            UUID sessionId,
            List<AttendanceEntryCommand> commands
    ) {
        validateCommands(commands);

        BatchSession session =
                batchSessionRepository
                        .findByIdForUpdate(sessionId)
                        .orElseThrow(
                                () ->
                                        new BatchSessionNotFoundException(
                                                sessionId
                                        )
                        );

        if (session.getStatus()
                == BatchSessionStatus.CANCELLED) {
            throw new AttendanceNotAllowedException(
                    "Attendance cannot be recorded for a cancelled session"
            );
        }

        validateNoDuplicateReferences(commands);

        List<UUID> enrollmentIds =
                commands.stream()
                        .map(
                                AttendanceEntryCommand
                                        ::enrollmentId
                        )
                        .toList();

        List<AttendanceRecord> existingRecords =
                attendanceRecordRepository
                        .findAllBySessionIdAndEnrollmentIdIn(
                                sessionId,
                                enrollmentIds
                        );

        Map<UUID, AttendanceRecord>
                recordsByEnrollmentId =
                new HashMap<>();

        for (AttendanceRecord record
                : existingRecords) {
            recordsByEnrollmentId.put(
                    record.getEnrollmentId(),
                    record
            );
        }

        Instant now = Instant.now(clock);

        List<AttendanceRecord> recordsToSave =
                new ArrayList<>();

        for (AttendanceEntryCommand command
                : commands) {

            AttendanceRecord existingRecord =
                    recordsByEnrollmentId.get(
                            command.enrollmentId()
                    );

            if (existingRecord == null) {
                AttendanceRecord newRecord =
                        AttendanceRecord.create(
                                sessionId,
                                command.enrollmentId(),
                                command.studentId(),
                                command.status(),
                                command.notes(),
                                now
                        );

                recordsToSave.add(newRecord);
                continue;
            }

            if (!existingRecord.getStudentId()
                    .equals(command.studentId())) {
                throw new AttendanceStudentMismatchException(
                        command.enrollmentId()
                );
            }

            existingRecord.mark(
                    command.status(),
                    command.notes(),
                    now
            );

            recordsToSave.add(existingRecord);
        }

        try {
            List<AttendanceRecord> savedRecords =
                    attendanceRecordRepository
                            .saveAllAndFlush(
                                    recordsToSave
                            );

            List<AttendanceRecordResult> results =
                    savedRecords.stream()
                            .map(
                                    AttendanceRecordResult
                                            ::from
                            )
                            .toList();

            return new AttendanceMarkingResult(
                    results
            );

        } catch (DataIntegrityViolationException exception) {
            throw new AttendanceConflictException(
                    "Attendance already contains a conflicting enrollment or student record for this session",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public AttendancePageResult searchBySession(
            UUID sessionId,
            AttendanceStatus status,
            int page,
            int size
    ) {
        validatePagination(page, size);

        if (!batchSessionRepository
                .existsById(sessionId)) {
            throw new BatchSessionNotFoundException(
                    sessionId
            );
        }

        Page<AttendanceRecord> attendancePage =
                attendanceRecordRepository
                        .searchBySession(
                                sessionId,
                                status,
                                PageRequest.of(
                                        page,
                                        size
                                )
                        );

        return toPageResult(attendancePage);
    }

    @Transactional(readOnly = true)
    public AttendancePageResult searchByBatch(
            UUID batchId,
            UUID studentId,
            UUID enrollmentId,
            AttendanceStatus status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        validatePagination(page, size);
        validateDateRange(dateFrom, dateTo);

        if (!batchRepository.existsById(batchId)) {
            throw new BatchNotFoundException(batchId);
        }

        Page<AttendanceRecord> attendancePage =
                attendanceRecordRepository
                        .searchByBatch(
                                batchId,
                                studentId,
                                enrollmentId,
                                status,
                                dateFrom,
                                dateTo,
                                PageRequest.of(
                                        page,
                                        size
                                )
                        );

        return toPageResult(attendancePage);
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResult getSummary(
            UUID sessionId
    ) {
        if (!batchSessionRepository
                .existsById(sessionId)) {
            throw new BatchSessionNotFoundException(
                    sessionId
            );
        }

        Map<AttendanceStatus, Long> counts =
                new EnumMap<>(
                        AttendanceStatus.class
                );

        for (AttendanceStatus status
                : AttendanceStatus.values()) {
            counts.put(status, 0L);
        }

        attendanceRecordRepository
                .countBySessionIdGroupedByStatus(
                        sessionId
                )
                .forEach(
                        count -> counts.put(
                                count.getStatus(),
                                count.getTotal()
                        )
                );

        long present =
                counts.get(AttendanceStatus.PRESENT);

        long absent =
                counts.get(AttendanceStatus.ABSENT);

        long late =
                counts.get(AttendanceStatus.LATE);

        long excused =
                counts.get(AttendanceStatus.EXCUSED);

        return new AttendanceSummaryResult(
                sessionId,
                present + absent + late + excused,
                present,
                absent,
                late,
                excused
        );
    }

    private AttendancePageResult toPageResult(
            Page<AttendanceRecord> attendancePage
    ) {
        List<AttendanceRecordResult> content =
                attendancePage.getContent()
                        .stream()
                        .map(
                                AttendanceRecordResult
                                        ::from
                        )
                        .toList();

        return AttendancePageResult.from(
                attendancePage,
                content
        );
    }

    private void validateCommands(
            List<AttendanceEntryCommand> commands
    ) {
        if (commands == null || commands.isEmpty()) {
            throw new InvalidAttendanceConfigurationException(
                    "At least one attendance record is required"
            );
        }

        if (commands.size()
                > MAXIMUM_RECORDS_PER_REQUEST) {
            throw new InvalidAttendanceConfigurationException(
                    "Attendance request must not contain more than "
                            + MAXIMUM_RECORDS_PER_REQUEST
                            + " records"
            );
        }
    }

    private void validateNoDuplicateReferences(
            List<AttendanceEntryCommand> commands
    ) {
        Set<UUID> enrollmentIds = new HashSet<>();
        Set<UUID> studentIds = new HashSet<>();

        for (AttendanceEntryCommand command
                : commands) {

            if (command.enrollmentId() == null
                    || command.studentId() == null
                    || command.status() == null) {
                throw new InvalidAttendanceConfigurationException(
                        "Enrollment ID, student ID, and attendance status are required"
                );
            }

            if (!enrollmentIds.add(
                    command.enrollmentId()
            )) {
                throw new InvalidAttendanceConfigurationException(
                        "Attendance request contains duplicate enrollment ID: "
                                + command.enrollmentId()
                );
            }

            if (!studentIds.add(
                    command.studentId()
            )) {
                throw new InvalidAttendanceConfigurationException(
                        "Attendance request contains duplicate student ID: "
                                + command.studentId()
                );
            }
        }
    }

    private void validateDateRange(
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (dateFrom != null
                && dateTo != null
                && dateFrom.isAfter(dateTo)) {
            throw new InvalidAttendanceConfigurationException(
                    "Date from must not be after date to"
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