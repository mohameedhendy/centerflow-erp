Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-DatabaseCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Database,

        [Parameter(Mandatory = $true)]
        [string]$Sql
    )

    $Sql |
        docker compose exec -T postgres `
            psql `
            -U centerflow_admin `
            -d $Database `
            -v ON_ERROR_STOP=1

    if ($LASTEXITCODE -ne 0) {
        throw "Database command failed for $Database"
    }
}

function Invoke-ScalarQuery {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Database,

        [Parameter(Mandatory = $true)]
        [string]$Sql
    )

    $result = docker compose exec -T postgres `
        psql `
        -U centerflow_admin `
        -d $Database `
        -t `
        -A `
        -c $Sql

    if ($LASTEXITCODE -ne 0) {
        throw "Database query failed for $Database"
    }

    return (($result | Out-String).Trim())
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

    if ([string]$Actual -ne [string]$Expected) {
        throw "$Message. Expected: $Expected. Actual: $Actual"
    }
}

$pricingPlanId = [guid]::NewGuid().ToString()
$financialAccountId = [guid]::NewGuid().ToString()
$enrollmentId = [guid]::NewGuid().ToString()
$studentId = [guid]::NewGuid().ToString()

$pastInstallmentId = [guid]::NewGuid().ToString()
$currentInstallmentId = [guid]::NewGuid().ToString()

$suffix = [guid]::NewGuid().ToString("N").Substring(0, 8).ToUpperInvariant()

$dueDate = "1900-01-01"
$asOfDate = "1900-01-02"

$processingUri =
    "http://localhost:8084/api/v1/finance/internal/installments/mark-overdue?asOfDate=${asOfDate}"

$financeCleanupSql = @"
BEGIN;

DELETE FROM installments
WHERE id IN (
    '$pastInstallmentId',
    '$currentInstallmentId'
);

DELETE FROM enrollment_financial_accounts
WHERE id = '$financialAccountId';

DELETE FROM pricing_plans
WHERE id = '$pricingPlanId';

COMMIT;
"@

$notificationCleanupSql = @"
BEGIN;

DELETE FROM notifications
WHERE reference_type = 'INSTALLMENT'
  AND reference_id = '$pastInstallmentId';

COMMIT;
"@

$financeSetupSql = @"
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
    'OD-$suffix',
    'Runtime Overdue Plan',
    'Runtime overdue notification verification',
    600.00,
    'XOD',
    2,
    300.00,
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
    version
)
VALUES
(
    '$financialAccountId',
    '$enrollmentId',
    '$studentId',
    '$pricingPlanId',
    'OD-$suffix',
    600.00,
    'XOD',
    2,
    300.00,
    'OPEN',
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
    '$pastInstallmentId',
    '$financialAccountId',
    1,
    DATE '$dueDate',
    300.00,
    0.00,
    'PENDING',
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
    '$currentInstallmentId',
    '$financialAccountId',
    2,
    DATE '$asOfDate',
    300.00,
    0.00,
    'PENDING',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

COMMIT;
"@

try {
    $financeHealth = Invoke-RestMethod `
        -Method Get `
        -Uri "http://localhost:8084/actuator/health" `
        -ErrorAction Stop

    Assert-Equal `
        -Actual $financeHealth.status `
        -Expected "UP" `
        -Message "Finance Service health check failed"

    $notificationHealth = Invoke-RestMethod `
        -Method Get `
        -Uri "http://localhost:8085/actuator/health" `
        -ErrorAction Stop

    Assert-Equal `
        -Actual $notificationHealth.status `
        -Expected "UP" `
        -Message "Notification Service health check failed"

    Write-Host `
        "`nFinance and Notification Services are UP." `
        -ForegroundColor Green

    Invoke-DatabaseCommand `
        -Database "notification_db" `
        -Sql $notificationCleanupSql

    Invoke-DatabaseCommand `
        -Database "finance_db" `
        -Sql $financeCleanupSql

    $existingOldInstallments = Invoke-ScalarQuery `
        -Database "finance_db" `
        -Sql "SELECT COUNT(*) FROM installments WHERE due_date < DATE '$asOfDate' AND status IN ('PENDING', 'PARTIALLY_PAID');"

    Assert-Equal `
        -Actual $existingOldInstallments `
        -Expected 0 `
        -Message "Safety check found existing installments before $asOfDate"

    Write-Host `
        "Database safety check passed." `
        -ForegroundColor Green

    Invoke-DatabaseCommand `
        -Database "finance_db" `
        -Sql $financeSetupSql

    Write-Host `
        "Runtime fixtures created." `
        -ForegroundColor Green

    $firstResult = Invoke-RestMethod `
        -Method Post `
        -Uri $processingUri `
        -ErrorAction Stop

    Assert-Equal `
        -Actual $firstResult.asOfDate `
        -Expected $asOfDate `
        -Message "Overdue processing date is incorrect"

    Assert-Equal `
        -Actual $firstResult.markedOverdueCount `
        -Expected 1 `
        -Message "Overdue processing count is incorrect"

    Write-Host `
        "One installment was marked overdue." `
        -ForegroundColor Green

    $pastStatus = Invoke-ScalarQuery `
        -Database "finance_db" `
        -Sql "SELECT status FROM installments WHERE id = '$pastInstallmentId';"

    $currentStatus = Invoke-ScalarQuery `
        -Database "finance_db" `
        -Sql "SELECT status FROM installments WHERE id = '$currentInstallmentId';"

    Assert-Equal `
        -Actual $pastStatus `
        -Expected "OVERDUE" `
        -Message "Past installment status is incorrect"

    Assert-Equal `
        -Actual $currentStatus `
        -Expected "PENDING" `
        -Message "Installment due on the processing date must remain pending"

    Write-Host `
        "Installment status rules passed." `
        -ForegroundColor Green

    $notificationCount = 0

    for ($attempt = 1; $attempt -le 20; $attempt++) {
        $notificationCount = [int](
            Invoke-ScalarQuery `
                -Database "notification_db" `
                -Sql "SELECT COUNT(*) FROM notifications WHERE reference_type = 'INSTALLMENT' AND reference_id = '$pastInstallmentId';"
        )

        if ($notificationCount -eq 1) {
            break
        }

        Start-Sleep -Milliseconds 250
    }

    Assert-Equal `
        -Actual $notificationCount `
        -Expected 1 `
        -Message "Overdue installment notification was not created"

    $notificationType = Invoke-ScalarQuery `
        -Database "notification_db" `
        -Sql "SELECT type FROM notifications WHERE reference_type = 'INSTALLMENT' AND reference_id = '$pastInstallmentId';"

    $notificationRecipient = Invoke-ScalarQuery `
        -Database "notification_db" `
        -Sql "SELECT recipient_user_id FROM notifications WHERE reference_type = 'INSTALLMENT' AND reference_id = '$pastInstallmentId';"

    $notificationStatus = Invoke-ScalarQuery `
        -Database "notification_db" `
        -Sql "SELECT status FROM notifications WHERE reference_type = 'INSTALLMENT' AND reference_id = '$pastInstallmentId';"

    $notificationTitle = Invoke-ScalarQuery `
        -Database "notification_db" `
        -Sql "SELECT title FROM notifications WHERE reference_type = 'INSTALLMENT' AND reference_id = '$pastInstallmentId';"

    Assert-Equal `
        -Actual $notificationType `
        -Expected "INSTALLMENT_OVERDUE" `
        -Message "Notification type is incorrect"

    Assert-Equal `
        -Actual $notificationRecipient `
        -Expected $studentId `
        -Message "Notification recipient is incorrect"

    Assert-Equal `
        -Actual $notificationStatus `
        -Expected "UNREAD" `
        -Message "Notification status is incorrect"

    Assert-Equal `
        -Actual $notificationTitle `
        -Expected "Installment overdue" `
        -Message "Notification title is incorrect"

    Write-Host `
        "Overdue installment notification was created." `
        -ForegroundColor Green

    $secondResult = Invoke-RestMethod `
        -Method Post `
        -Uri $processingUri `
        -ErrorAction Stop

    Assert-Equal `
        -Actual $secondResult.markedOverdueCount `
        -Expected 0 `
        -Message "Repeated processing must not update the installment again"

    $notificationCountAfterRetry = Invoke-ScalarQuery `
        -Database "notification_db" `
        -Sql "SELECT COUNT(*) FROM notifications WHERE reference_type = 'INSTALLMENT' AND reference_id = '$pastInstallmentId';"

    Assert-Equal `
        -Actual $notificationCountAfterRetry `
        -Expected 1 `
        -Message "Repeated processing created a duplicate notification"

    Write-Host `
        "Repeated processing is idempotent." `
        -ForegroundColor Green

    Write-Host `
        "`nAll overdue installment runtime checks passed." `
        -ForegroundColor Green
}
catch {
    Write-Host `
        "`nOverdue installment runtime verification failed." `
        -ForegroundColor Red

    Write-Host `
        $_.Exception.Message `
        -ForegroundColor Red

    throw
}
finally {
    try {
        Invoke-DatabaseCommand `
            -Database "notification_db" `
            -Sql $notificationCleanupSql

        Invoke-DatabaseCommand `
            -Database "finance_db" `
            -Sql $financeCleanupSql

        Write-Host `
            "Runtime fixtures removed." `
            -ForegroundColor DarkGray
    }
    catch {
        Write-Warning `
            "Runtime fixture cleanup failed: $($_.Exception.Message)"
    }
}