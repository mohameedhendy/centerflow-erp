package com.centerflow.enrollment.application;

import com.centerflow.enrollment.api.dto.CreateEnrollmentRequest;
import com.centerflow.enrollment.api.dto.EnrollmentResponse;
import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.domain.EnrollmentStatus;
import com.centerflow.enrollment.exception.EnrollmentConflictException;
import com.centerflow.enrollment.exception.EnrollmentNotFoundException;
import com.centerflow.enrollment.number.EnrollmentNumberGenerator;
import com.centerflow.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTests {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentNumberGenerator numberGenerator;

    @Test
    void createEnrollmentShouldSavePendingPaymentEnrollment() {
        UUID studentId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        CreateEnrollmentRequest request =
                new CreateEnrollmentRequest(
                        studentId,
                        batchId
                );

        when(
                enrollmentRepository
                        .existsByStudentIdAndBatchIdAndStatusIn(
                                eq(studentId),
                                eq(batchId),
                                anyCollection()
                        )
        ).thenReturn(false);

        when(numberGenerator.nextNumber())
                .thenReturn("ENR-2026-000001");

        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentService service = new EnrollmentService(
                enrollmentRepository,
                numberGenerator
        );

        EnrollmentResponse response =
                service.createEnrollment(request);

        assertThat(response.enrollmentNumber())
                .isEqualTo("ENR-2026-000001");

        assertThat(response.studentId())
                .isEqualTo(studentId);

        assertThat(response.batchId())
                .isEqualTo(batchId);

        assertThat(response.status())
                .isEqualTo(
                        EnrollmentStatus.PENDING_PAYMENT
                );

        ArgumentCaptor<Enrollment> enrollmentCaptor =
                ArgumentCaptor.forClass(Enrollment.class);

        verify(enrollmentRepository)
                .save(enrollmentCaptor.capture());

        Enrollment savedEnrollment =
                enrollmentCaptor.getValue();

        assertThat(savedEnrollment.getEnrollmentNumber())
                .isEqualTo("ENR-2026-000001");
    }

    @Test
    void createEnrollmentShouldRejectDuplicateOpenEnrollment() {
        UUID studentId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        CreateEnrollmentRequest request =
                new CreateEnrollmentRequest(
                        studentId,
                        batchId
                );

        when(
                enrollmentRepository
                        .existsByStudentIdAndBatchIdAndStatusIn(
                                eq(studentId),
                                eq(batchId),
                                anyCollection()
                        )
        ).thenReturn(true);

        EnrollmentService service = new EnrollmentService(
                enrollmentRepository,
                numberGenerator
        );

        assertThatThrownBy(
                () -> service.createEnrollment(request)
        )
                .isInstanceOf(
                        EnrollmentConflictException.class
                )
                .hasMessage(
                        "Student already has an open enrollment "
                                + "in this batch"
                );

        verify(numberGenerator, never()).nextNumber();
        verify(enrollmentRepository, never())
                .save(any(Enrollment.class));
    }

    @Test
    void getEnrollmentShouldReturnExistingEnrollment() {
        UUID enrollmentId = UUID.randomUUID();

        Enrollment enrollment = Enrollment.create(
                "ENR-2026-000002",
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.of(enrollment));

        EnrollmentService service = new EnrollmentService(
                enrollmentRepository,
                numberGenerator
        );

        EnrollmentResponse response =
                service.getEnrollment(enrollmentId);

        assertThat(response.id())
                .isEqualTo(enrollment.getId());

        assertThat(response.enrollmentNumber())
                .isEqualTo("ENR-2026-000002");
    }

    @Test
    void getEnrollmentShouldRejectUnknownId() {
        UUID enrollmentId = UUID.randomUUID();

        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.empty());

        EnrollmentService service = new EnrollmentService(
                enrollmentRepository,
                numberGenerator
        );

        assertThatThrownBy(
                () -> service.getEnrollment(enrollmentId)
        )
                .isInstanceOf(
                        EnrollmentNotFoundException.class
                )
                .hasMessageContaining(enrollmentId.toString());
    }
}