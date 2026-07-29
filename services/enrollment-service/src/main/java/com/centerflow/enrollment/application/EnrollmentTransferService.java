package com.centerflow.enrollment.application;

import com.centerflow.enrollment.api.dto.EnrollmentTransferResponse;
import com.centerflow.enrollment.api.dto.PageResponse;
import com.centerflow.enrollment.api.dto.TransferEnrollmentRequest;
import com.centerflow.enrollment.domain.Enrollment;
import com.centerflow.enrollment.domain.EnrollmentStatus;
import com.centerflow.enrollment.domain.EnrollmentTransfer;
import com.centerflow.enrollment.exception.EnrollmentConflictException;
import com.centerflow.enrollment.exception.EnrollmentNotFoundException;
import com.centerflow.enrollment.exception.InvalidPaginationException;
import com.centerflow.enrollment.repository.EnrollmentRepository;
import com.centerflow.enrollment.repository.EnrollmentTransferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class EnrollmentTransferService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<EnrollmentStatus>
            OPEN_ENROLLMENT_STATUSES = EnumSet.of(
            EnrollmentStatus.PENDING_PAYMENT,
            EnrollmentStatus.ACTIVE,
            EnrollmentStatus.SUSPENDED
    );

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentTransferRepository transferRepository;

    public EnrollmentTransferService(
            EnrollmentRepository enrollmentRepository,
            EnrollmentTransferRepository transferRepository
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.transferRepository = transferRepository;
    }

    @Transactional
    public EnrollmentTransferResponse transferEnrollment(
            UUID enrollmentId,
            TransferEnrollmentRequest request
    ) {
        Enrollment enrollment = enrollmentRepository
                .findByIdForUpdate(enrollmentId)
                .orElseThrow(
                        () -> new EnrollmentNotFoundException(
                                enrollmentId
                        )
                );

        enrollment.ensureTransferAllowed(
                request.targetBatchId()
        );

        boolean targetEnrollmentExists =
                enrollmentRepository
                        .existsByStudentIdAndBatchIdAndStatusInAndIdNot(
                                enrollment.getStudentId(),
                                request.targetBatchId(),
                                OPEN_ENROLLMENT_STATUSES,
                                enrollmentId
                        );

        if (targetEnrollmentExists) {
            throw new EnrollmentConflictException(
                    "Student already has an open enrollment "
                            + "in the target batch"
            );
        }

        UUID previousBatchId = enrollment.getBatchId();

        enrollment.transferTo(request.targetBatchId());

        EnrollmentTransfer transfer =
                EnrollmentTransfer.create(
                        enrollment.getId(),
                        previousBatchId,
                        request.targetBatchId(),
                        request.reason()
                );

        EnrollmentTransfer savedTransfer =
                transferRepository.save(transfer);

        return EnrollmentTransferResponse.from(savedTransfer);
    }

    @Transactional(readOnly = true)
    public PageResponse<EnrollmentTransferResponse>
    getTransferHistory(
            UUID enrollmentId,
            int page,
            int size
    ) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new EnrollmentNotFoundException(enrollmentId);
        }

        validatePagination(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("transferredAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<EnrollmentTransfer> transfers =
                transferRepository.findAllByEnrollmentId(
                        enrollmentId,
                        pageRequest
                );

        return PageResponse.from(
                transfers,
                EnrollmentTransferResponse::from
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidPaginationException(
                    "Page index must be zero or greater"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Page size must be between 1 and 100"
            );
        }
    }
}