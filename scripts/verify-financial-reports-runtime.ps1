Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-FinanceSql {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Sql
    )

    $Sql |
        docker compose exec -T postgres `
            psql `
            -U centerflow_admin `
            -d finance_db `
            -v ON_ERROR_STOP=1

    if ($LASTEXITCODE -ne 0) {
        throw "Finance database command failed"
    }
}

function Assert-Equal {
    param(
        [Parameter(Mandatory = $true)]
        $Actual,

        [Parameter(Mandatory = $true)]
        $Expected,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message. Expected: $Expected. Actual: $Actual"
    }
}

function Assert-Money {
    param(
        [Parameter(Mandatory = $true)]
        $Actual,

        [Parameter(Mandatory = $true)]
        [decimal]$Expected,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $actualDecimal = [decimal]$Actual

    if ($actualDecimal -ne $Expected) {
        throw "$Message. Expected: $Expected. Actual: $actualDecimal"
    }
}

function Assert-HttpStatus {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,

        [Parameter(Mandatory = $true)]
        [int]$ExpectedStatus
    )

    $actualStatus = 0

    try {
        Invoke-WebRequest `
            -Method Get `
            -Uri $Uri `
            -UseBasicParsing `
            -ErrorAction Stop |
            Out-Null
    }
    catch {
        if ($null -ne $_.Exception.Response) {
            $actualStatus =
                [int]$_.Exception.Response.StatusCode
        }
    }

    if ($actualStatus -ne $ExpectedStatus) {
        throw "Expected HTTP $ExpectedStatus from $Uri but received $actualStatus"
    }
}

$pricingPlanId = [guid]::NewGuid().ToString()
$financialAccountId = [guid]::NewGuid().ToString()
$enrollmentId = [guid]::NewGuid().ToString()
$studentId = [guid]::NewGuid().ToString()

$firstInstallmentId = [guid]::NewGuid().ToString()
$secondInstallmentId = [guid]::NewGuid().ToString()

$paymentId = [guid]::NewGuid().ToString()
$refundId = [guid]::NewGuid().ToString()

$discountId = [guid]::NewGuid().ToString()
$chargeId = [guid]::NewGuid().ToString()

$expenseId = [guid]::NewGuid().ToString()
$cancelledExpenseId = [guid]::NewGuid().ToString()

$paidEarningId = [guid]::NewGuid().ToString()
$accruedEarningId = [guid]::NewGuid().ToString()

$paidInstructorId = [guid]::NewGuid().ToString()
$accruedInstructorId = [guid]::NewGuid().ToString()

$paidSessionId = [guid]::NewGuid().ToString()
$accruedSessionId = [guid]::NewGuid().ToString()

$paidBatchId = [guid]::NewGuid().ToString()
$accruedBatchId = [guid]::NewGuid().ToString()

$suffix = [guid]::NewGuid().ToString("N").Substring(0, 8).ToUpper()
$reportDate = [DateTime]::UtcNow.ToString("yyyy-MM-dd")

$baseUri = "http://localhost:8084/api/v1/finance/reports"

$cleanupSql = @"
BEGIN;

DELETE FROM refunds
WHERE id = '$refundId';

DELETE FROM payments
WHERE id = '$paymentId';

DELETE FROM financial_adjustments
WHERE id IN (
    '$discountId',
    '$chargeId'
);

DELETE FROM installments
WHERE id IN (
    '$firstInstallmentId',
    '$secondInstallmentId'
);

DELETE FROM enrollment_financial_accounts
WHERE id = '$financialAccountId';

DELETE FROM pricing_plans
WHERE id = '$pricingPlanId';

DELETE FROM expenses
WHERE id IN (
    '$expenseId',
    '$cancelledExpenseId'
);

DELETE FROM instructor_earnings
WHERE id IN (
    '$paidEarningId',
    '$accruedEarningId'
);

COMMIT;
"@

$setupSql = @"
BEGIN;

INSERT INTO pricing_plans
(
    id,
    code,
    name,
    description,
    total_amount,
    currency,
    installment_count,
    initial_payment_amount,
    status,
    created_at,
    updated_at,
    version
)
VALUES
(
    '$pricingPlanId',
    'RPT-PLAN-$suffix',
    'Runtime Financial Report Plan',
    'Runtime financial report plan',
    1000.00,
    'XRT',
    2,
    500.00,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO enrollment_financial_accounts
(
    id,
    enrollment_id,
    student_id,
    pricing_plan_id,
    pricing_plan_code,
    total_amount,
    currency,
    installment_count,
    initial_payment_amount,
    status,
    created_at,
    updated_at,
    version,
    paid_amount
)
VALUES
(
    '$financialAccountId',
    '$enrollmentId',
    '$studentId',
    '$pricingPlanId',
    'RPT-PLAN-$suffix',
    1100.00,
    'XRT',
    2,
    500.00,
    'OPEN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0,
    600.00
);

INSERT INTO installments
(
    id,
    financial_account_id,
    installment_number,
    due_date,
    amount,
    paid_amount,
    status,
    created_at,
    updated_at,
    version
)
VALUES
(
    '$firstInstallmentId',
    '$financialAccountId',
    1,
    '$reportDate',
    500.00,
    500.00,
    'PAID',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO installments
(
    id,
    financial_account_id,
    installment_number,
    due_date,
    amount,
    paid_amount,
    status,
    created_at,
    updated_at,
    version
)
VALUES
(
    '$secondInstallmentId',
    '$financialAccountId',
    2,
    '$reportDate',
    600.00,
    100.00,
    'OVERDUE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO payments
(
    id,
    payment_number,
    financial_account_id,
    amount,
    currency,
    method,
    external_reference,
    status,
    recorded_at,
    version,
    refunded_amount
)
VALUES
(
    '$paymentId',
    'PAY-RPT-$suffix',
    '$financialAccountId',
    700.00,
    'XRT',
    'CARD',
    'PAY-RPT-EXT-$suffix',
    'PARTIALLY_REFUNDED',
    CURRENT_TIMESTAMP,
    0,
    100.00
);

INSERT INTO refunds
(
    id,
    refund_number,
    payment_id,
    amount,
    currency,
    reason,
    external_reference,
    status,
    recorded_at,
    version
)
VALUES
(
    '$refundId',
    'REF-RPT-$suffix',
    '$paymentId',
    100.00,
    'XRT',
    'Runtime financial report refund',
    'REF-RPT-EXT-$suffix',
    'RECORDED',
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO financial_adjustments
(
    id,
    financial_account_id,
    type,
    amount,
    currency,
    reason,
    external_reference,
    created_at
)
VALUES
(
    '$discountId',
    '$financialAccountId',
    'DISCOUNT',
    50.00,
    'XRT',
    'Runtime financial report discount',
    'ADJ-DISCOUNT-$suffix',
    CURRENT_TIMESTAMP
);

INSERT INTO financial_adjustments
(
    id,
    financial_account_id,
    type,
    amount,
    currency,
    reason,
    external_reference,
    created_at
)
VALUES
(
    '$chargeId',
    '$financialAccountId',
    'CHARGE',
    150.00,
    'XRT',
    'Runtime financial report charge',
    'ADJ-CHARGE-$suffix',
    CURRENT_TIMESTAMP
);

INSERT INTO expenses
(
    id,
    expense_number,
    branch_id,
    category,
    amount,
    currency,
    payment_method,
    payee,
    description,
    expense_date,
    external_reference,
    status,
    cancellation_reason,
    created_at,
    updated_at,
    cancelled_at,
    version
)
VALUES
(
    '$expenseId',
    'EXP-RPT-$suffix',
    NULL,
    'RENT',
    200.00,
    'XRT',
    'BANK_TRANSFER',
    'Runtime Property Owner',
    'Runtime financial report rent',
    '$reportDate',
    'EXP-RPT-EXT-$suffix',
    'RECORDED',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL,
    0
);

INSERT INTO expenses
(
    id,
    expense_number,
    branch_id,
    category,
    amount,
    currency,
    payment_method,
    payee,
    description,
    expense_date,
    external_reference,
    status,
    cancellation_reason,
    created_at,
    updated_at,
    cancelled_at,
    version
)
VALUES
(
    '$cancelledExpenseId',
    'EXP-CAN-$suffix',
    NULL,
    'OTHER',
    50.00,
    'XRT',
    'CASH',
    'Cancelled Runtime Payee',
    'Cancelled runtime expense',
    '$reportDate',
    'EXP-CAN-EXT-$suffix',
    'CANCELLED',
    'Runtime expense entered incorrectly',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO instructor_earnings
(
    id,
    earning_number,
    instructor_id,
    session_id,
    batch_id,
    amount,
    currency,
    session_date,
    description,
    status,
    payment_method,
    payment_reference,
    cancellation_reason,
    accrued_at,
    paid_at,
    cancelled_at,
    created_at,
    updated_at,
    version
)
VALUES
(
    '$paidEarningId',
    'ERN-PAID-$suffix',
    '$paidInstructorId',
    '$paidSessionId',
    '$paidBatchId',
    300.00,
    'XRT',
    '$reportDate',
    'Paid runtime instructor earning',
    'PAID',
    'BANK_TRANSFER',
    'ERN-PAY-$suffix',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO instructor_earnings
(
    id,
    earning_number,
    instructor_id,
    session_id,
    batch_id,
    amount,
    currency,
    session_date,
    description,
    status,
    payment_method,
    payment_reference,
    cancellation_reason,
    accrued_at,
    paid_at,
    cancelled_at,
    created_at,
    updated_at,
    version
)
VALUES
(
    '$accruedEarningId',
    'ERN-ACC-$suffix',
    '$accruedInstructorId',
    '$accruedSessionId',
    '$accruedBatchId',
    150.00,
    'XRT',
    '$reportDate',
    'Accrued runtime instructor earning',
    'ACCRUED',
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

COMMIT;
"@

try {
    $health = Invoke-RestMethod `
        -Method Get `
        -Uri "http://localhost:8084/actuator/health" `
        -ErrorAction Stop

    Assert-Equal `
        ([string]$health.status) `
        "UP" `
        "Finance Service health check failed"

    Write-Host "`nFinance Service health is UP." -ForegroundColor Green

    Invoke-FinanceSql -Sql $cleanupSql
    Invoke-FinanceSql -Sql $setupSql

    Write-Host "Financial Report runtime fixtures created." -ForegroundColor Green

    $overviewUri = "${baseUri}/overview?currency=XRT&fromDate=${reportDate}&toDate=${reportDate}"

    $overview = Invoke-RestMethod `
        -Method Get `
        -Uri $overviewUri `
        -ErrorAction Stop

    Assert-Equal ([string]$overview.currency) "XRT" "Overview currency is incorrect"
    Assert-Equal ([int]$overview.accounts.totalAccounts) 1 "Account total is incorrect"
    Assert-Equal ([int]$overview.accounts.openAccounts) 1 "Open account total is incorrect"
    Assert-Money $overview.accounts.billedAmount 1100 "Billed amount is incorrect"
    Assert-Money $overview.accounts.paidAmount 600 "Paid account amount is incorrect"
    Assert-Money $overview.accounts.outstandingAmount 500 "Outstanding amount is incorrect"
    Assert-Equal ([int]$overview.accounts.overdueInstallments) 1 "Overdue installment total is incorrect"
    Assert-Money $overview.accounts.overdueAmount 500 "Overdue amount is incorrect"

    Assert-Equal ([int]$overview.cashFlow.paymentCount) 1 "Payment total is incorrect"
    Assert-Money $overview.cashFlow.collectedAmount 700 "Collected amount is incorrect"
    Assert-Money $overview.cashFlow.refundedAmount 100 "Refunded amount is incorrect"
    Assert-Money $overview.cashFlow.expenseAmount 200 "Expense amount is incorrect"
    Assert-Money $overview.cashFlow.instructorPaidAmount 300 "Instructor paid amount is incorrect"
    Assert-Money $overview.cashFlow.netCashFlow 100 "Net cash flow is incorrect"

    Assert-Money $overview.adjustments.discountAmount 50 "Discount amount is incorrect"
    Assert-Money $overview.adjustments.chargeAmount 150 "Charge amount is incorrect"

    Assert-Equal ([int]$overview.currentLiabilities.accruedInstructorEarnings) 1 "Accrued earning total is incorrect"
    Assert-Money $overview.currentLiabilities.accruedInstructorAmount 150 "Accrued earning amount is incorrect"

    Assert-Equal ([int]$overview.paymentMethods.Count) 1 "Payment-method breakdown is incorrect"
    Assert-Equal ([string]$overview.paymentMethods[0].paymentMethod) "CARD" "Payment method is incorrect"
    Assert-Money $overview.paymentMethods[0].amount 700 "Payment-method amount is incorrect"

    Assert-Equal ([int]$overview.expenseCategories.Count) 1 "Expense-category breakdown is incorrect"
    Assert-Equal ([string]$overview.expenseCategories[0].category) "RENT" "Expense category is incorrect"
    Assert-Money $overview.expenseCategories[0].amount 200 "Expense-category amount is incorrect"

    Write-Host "Financial overview report passed." -ForegroundColor Green

    $accountUri = "${baseUri}/accounts/${financialAccountId}"

    $accountReport = Invoke-RestMethod `
        -Method Get `
        -Uri $accountUri `
        -ErrorAction Stop

    Assert-Equal ([string]$accountReport.financialAccountId) $financialAccountId "Account report returned the wrong account"
    Assert-Equal ([string]$accountReport.status) "OPEN" "Account status is incorrect"
    Assert-Money $accountReport.totalAmount 1100 "Account total amount is incorrect"
    Assert-Money $accountReport.paidAmount 600 "Account paid amount is incorrect"
    Assert-Money $accountReport.outstandingAmount 500 "Account outstanding amount is incorrect"

    Assert-Money $accountReport.payments.grossCollectedAmount 700 "Gross collected amount is incorrect"
    Assert-Money $accountReport.payments.refundedAmount 100 "Account refunded amount is incorrect"
    Assert-Money $accountReport.payments.netCollectedAmount 600 "Net collected amount is incorrect"

    Assert-Equal ([int]$accountReport.installments.totalInstallments) 2 "Installment total is incorrect"
    Assert-Equal ([int]$accountReport.installments.paidInstallments) 1 "Paid installment total is incorrect"
    Assert-Equal ([int]$accountReport.installments.overdueInstallments) 1 "Overdue installment report total is incorrect"
    Assert-Money $accountReport.installments.outstandingAmount 500 "Installment outstanding amount is incorrect"

    Assert-Money $accountReport.adjustments.discountAmount 50 "Account discount amount is incorrect"
    Assert-Money $accountReport.adjustments.chargeAmount 150 "Account charge amount is incorrect"

    Write-Host "Financial account report passed." -ForegroundColor Green

    $invalidPeriodUri = "${baseUri}/overview?currency=XRT&fromDate=2026-08-02&toDate=2026-08-01"
    Assert-HttpStatus $invalidPeriodUri 400

    $missingAccountId = [guid]::NewGuid().ToString()
    $missingAccountUri = "${baseUri}/accounts/${missingAccountId}"
    Assert-HttpStatus $missingAccountUri 404

    Write-Host "Financial Report error responses passed." -ForegroundColor Green

    $databaseResult = docker compose exec -T postgres `
        psql `
        -U centerflow_admin `
        -d finance_db `
        -t `
        -A `
        -F "|" `
        -c "SELECT (SELECT COUNT(*) FROM enrollment_financial_accounts WHERE id = '$financialAccountId'), (SELECT COUNT(*) FROM payments WHERE id = '$paymentId'), (SELECT COUNT(*) FROM refunds WHERE id = '$refundId'), (SELECT COUNT(*) FROM expenses WHERE id = '$expenseId'), (SELECT COUNT(*) FROM instructor_earnings WHERE id IN ('$paidEarningId', '$accruedEarningId'));"

    if ($LASTEXITCODE -ne 0) {
        throw "Financial Report database verification query failed"
    }

    $databaseLine = (($databaseResult | Out-String).Trim())
    $parts = $databaseLine.Split("|")

    Assert-Equal $parts.Count 5 "Unexpected database verification result"
    Assert-Equal ([int]$parts[0]) 1 "Runtime account total is incorrect"
    Assert-Equal ([int]$parts[1]) 1 "Runtime payment total is incorrect"
    Assert-Equal ([int]$parts[2]) 1 "Runtime refund total is incorrect"
    Assert-Equal ([int]$parts[3]) 1 "Runtime recorded-expense total is incorrect"
    Assert-Equal ([int]$parts[4]) 2 "Runtime instructor-earning total is incorrect"

    Write-Host "Financial Report database records are consistent." -ForegroundColor Green
    Write-Host "`nAll Financial Report runtime checks passed." -ForegroundColor Green
}
catch {
    Write-Host "`nFinancial Report runtime verification failed." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    throw
}
finally {
    try {
        Invoke-FinanceSql -Sql $cleanupSql
        Write-Host "Financial Report runtime fixtures removed." -ForegroundColor DarkGray
    }
    catch {
        Write-Warning "Financial Report fixture cleanup failed: $($_.Exception.Message)"
    }
}