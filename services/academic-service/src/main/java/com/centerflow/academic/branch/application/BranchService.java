package com.centerflow.academic.branch.application;

import com.centerflow.academic.branch.domain.Branch;
import com.centerflow.academic.branch.repository.BranchRepository;
import com.centerflow.academic.common.exception.BranchNotFoundException;
import com.centerflow.academic.common.exception.DuplicateBranchCodeException;
import com.centerflow.academic.common.exception.InvalidPaginationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BranchService {

    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final BranchRepository branchRepository;
    private final Clock clock;

    public BranchService(
            BranchRepository branchRepository,
            Clock clock
    ) {
        this.branchRepository = branchRepository;
        this.clock = clock;
    }

    @Transactional
    public BranchResult create(
            String code,
            String name,
            String phone,
            String email,
            String address,
            String city
    ) {
        String normalizedCode =
                Branch.normalizedCode(code);

        if (branchRepository.existsByCode(
                normalizedCode
        )) {
            throw new DuplicateBranchCodeException(
                    normalizedCode
            );
        }

        Instant createdAt = Instant.now(clock);

        Branch branch = Branch.create(
                normalizedCode,
                name,
                phone,
                email,
                address,
                city,
                createdAt
        );

        try {
            Branch savedBranch =
                    branchRepository.saveAndFlush(branch);

            return BranchResult.from(savedBranch);

        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateBranchCodeException(
                    normalizedCode,
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public BranchResult getById(UUID branchId) {
        return BranchResult.from(
                findBranch(branchId)
        );
    }

    @Transactional(readOnly = true)
    public BranchPageResult search(
            String search,
            String city,
            Boolean active,
            int page,
            int size
    ) {
        validatePagination(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.ASC,
                        "name"
                )
        );

        Page<Branch> branchPage =
                branchRepository.search(
                        normalizeFilter(search),
                        normalizeFilter(city),
                        active,
                        pageRequest
                );

        List<BranchResult> content =
                branchPage.getContent()
                        .stream()
                        .map(BranchResult::from)
                        .toList();

        return BranchPageResult.from(
                branchPage,
                content
        );
    }

    @Transactional
    public BranchResult update(
            UUID branchId,
            String name,
            String phone,
            String email,
            String address,
            String city
    ) {
        Branch branch = findBranch(branchId);

        branch.updateDetails(
                name,
                phone,
                email,
                address,
                city,
                Instant.now(clock)
        );

        return BranchResult.from(branch);
    }

    @Transactional
    public BranchResult changeStatus(
            UUID branchId,
            boolean active
    ) {
        Branch branch = findBranch(branchId);

        branch.changeStatus(
                active,
                Instant.now(clock)
        );

        return BranchResult.from(branch);
    }

    private Branch findBranch(UUID branchId) {
        return branchRepository
                .findById(branchId)
                .orElseThrow(
                        () -> new BranchNotFoundException(
                                branchId
                        )
                );
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