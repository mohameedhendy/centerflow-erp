package com.centerflow.enrollment.repository;

import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.domain.EnrollmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnrollmentRepositoryTests {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void shouldSaveAndFindEnrollmentByNumber() {
        Enrollment enrollment = Enrollment.create(
                "ENR-2026-000010",
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        enrollmentRepository.saveAndFlush(enrollment);

        Optional<Enrollment> result =
                enrollmentRepository.findByEnrollmentNumber(
                        "ENR-2026-000010"
                );

        assertThat(result).isPresent();
        assertThat(result.get().getId())
                .isEqualTo(enrollment.getId());
    }

    @Test
    void shouldDetectExistingOpenEnrollmentForStudentAndBatch() {
        UUID studentId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        Enrollment enrollment = Enrollment.create(
                "ENR-2026-000011",
                studentId,
                batchId
        );

        enrollmentRepository.saveAndFlush(enrollment);

        boolean exists =
                enrollmentRepository
                        .existsByStudentIdAndBatchIdAndStatusIn(
                                studentId,
                                batchId,
                                EnumSet.of(
                                        EnrollmentStatus.PENDING_PAYMENT,
                                        EnrollmentStatus.ACTIVE,
                                        EnrollmentStatus.SUSPENDED
                                )
                        );

        assertThat(exists).isTrue();
    }

    @Test
    void shouldFindEnrollmentsByBatchAndStatus() {
        UUID batchId = UUID.randomUUID();

        Enrollment pendingEnrollment = Enrollment.create(
                "ENR-2026-000012",
                UUID.randomUUID(),
                batchId
        );

        Enrollment activeEnrollment = Enrollment.create(
                "ENR-2026-000013",
                UUID.randomUUID(),
                batchId
        );

        activeEnrollment.activate();

        enrollmentRepository.save(pendingEnrollment);
        enrollmentRepository.save(activeEnrollment);
        enrollmentRepository.flush();

        Page<Enrollment> result =
                enrollmentRepository
                        .findAllByBatchIdAndStatus(
                                batchId,
                                EnrollmentStatus.ACTIVE,
                                PageRequest.of(0, 10)
                        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(
                        Enrollment::getEnrollmentNumber
                )
                .containsExactly("ENR-2026-000013");
    }
}