package com.centerflow.academic.batchschedule.repository;

import com.centerflow.academic.batchschedule.domain.BatchSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface BatchScheduleRepository
        extends JpaRepository<BatchSchedule, UUID> {

    @Query("""
            SELECT schedule
            FROM BatchSchedule schedule
            WHERE schedule.batchId = :batchId
            AND (
                :dayOfWeek IS NULL
                OR schedule.dayOfWeek = :dayOfWeek
            )
            AND (
                :active IS NULL
                OR schedule.active = :active
            )
            """)
    Page<BatchSchedule> search(
            @Param("batchId") UUID batchId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT CASE
                        WHEN COUNT(*) > 0 THEN TRUE
                        ELSE FALSE
                    END
                    FROM batch_schedules schedule
                    JOIN batches existing_batch
                      ON existing_batch.id = schedule.batch_id
                    WHERE schedule.active = TRUE
                      AND existing_batch.status IN (
                          'DRAFT',
                          'OPEN_FOR_ENROLLMENT',
                          'IN_PROGRESS'
                      )
                      AND existing_batch.classroom_id = :classroomId
                      AND schedule.day_of_week = :dayOfWeek
                      AND schedule.start_time < :endTime
                      AND schedule.end_time > :startTime
                      AND existing_batch.start_date <= :candidateEndDate
                      AND existing_batch.end_date >= :candidateStartDate
                      AND (
                          :excludedScheduleId IS NULL
                          OR schedule.id <> :excludedScheduleId
                      )
                    """,
            nativeQuery = true
    )
    boolean existsClassroomConflict(
            @Param("classroomId") UUID classroomId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("candidateStartDate")
            LocalDate candidateStartDate,
            @Param("candidateEndDate")
            LocalDate candidateEndDate,
            @Param("excludedScheduleId")
            UUID excludedScheduleId
    );

    @Query(
            value = """
                    SELECT CASE
                        WHEN COUNT(*) > 0 THEN TRUE
                        ELSE FALSE
                    END
                    FROM batch_schedules schedule
                    JOIN batches existing_batch
                      ON existing_batch.id = schedule.batch_id
                    WHERE schedule.active = TRUE
                      AND existing_batch.status IN (
                          'DRAFT',
                          'OPEN_FOR_ENROLLMENT',
                          'IN_PROGRESS'
                      )
                      AND existing_batch.instructor_id = :instructorId
                      AND schedule.day_of_week = :dayOfWeek
                      AND schedule.start_time < :endTime
                      AND schedule.end_time > :startTime
                      AND existing_batch.start_date <= :candidateEndDate
                      AND existing_batch.end_date >= :candidateStartDate
                      AND (
                          :excludedScheduleId IS NULL
                          OR schedule.id <> :excludedScheduleId
                      )
                    """,
            nativeQuery = true
    )
    boolean existsInstructorConflict(
            @Param("instructorId") UUID instructorId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("candidateStartDate")
            LocalDate candidateStartDate,
            @Param("candidateEndDate")
            LocalDate candidateEndDate,
            @Param("excludedScheduleId")
            UUID excludedScheduleId
    );
}