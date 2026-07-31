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
            -v ON_ERROR_STOP=1 |
        Out-Null

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

function Assert-Money {
    param(
        [Parameter(Mandatory = $true)]
        $Actual,

        [Parameter(Mandatory = $true)]
        [decimal]$Expected,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $actualValue = [decimal]$Actual

    if ($actualValue -ne $Expected) {
        throw "$Message. Expected: $Expected. Actual: $actualValue"
    }
}

function Get-OptionalJson {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri
    )

    try {
        return Invoke-RestMethod `
            -Method Get `
            -Uri $Uri `
            -ErrorAction Stop
    }
    catch {
        if ($null -ne $_.Exception.Response) {
            $statusCode =
                [int]$_.Exception.Response.StatusCode

            if ($statusCode -eq 404) {
                return $null
            }
        }

        throw
    }
}

$studentId = [guid]::NewGuid().ToString()
$batchId = [guid]::NewGuid().ToString()

$suffix = [guid]::NewGuid().ToString("N")
$suffix = $suffix.Substring(0, 8).ToUpperInvariant()

$planCode = "E2E-$suffix"
$paymentExternalReference = "E2E-PAY-$suffix"

$firstInstallmentDueDate = [DateTime]::UtcNow.Date.AddDays(30).ToString("yyyy-MM-dd")

$enrollmentBaseUri =
    "http://localhost:8083/api/v1/enrollments"

$financeBaseUri =
    "http://localhost:8084/api/v1/finance"

$notificationBaseUri =
    "http://localhost:8085/api/v1/notifications"

$notificationCleanupSql = @"
BEGIN;

DELETE FROM notifications
WHERE recipient_user_id = '$studentId'
  AND type = 'PAYMENT_RECORDED';

COMMIT;
"@

$financeCleanupSql = @"
BEGIN;

DELETE FROM enrollment_activation_tasks
WHERE enrollment_id IN (
    SELECT enrollment_id
    FROM enrollment_financial_accounts
    WHERE pricing_plan_code = '$planCode'
)
OR payment_id IN (
    SELECT id
    FROM payments
    WHERE external_reference = '$paymentExternalReference'
);

DELETE FROM payment_allocations
WHERE payment_id IN (
    SELECT id
    FROM payments
    WHERE external_reference = '$paymentExternalReference'
);

DELETE FROM payments
WHERE external_reference = '$paymentExternalReference';

DELETE FROM installments
WHERE financial_account_id IN (
    SELECT id
    FROM enrollment_financial_accounts
    WHERE pricing_plan_code = '$planCode'
);

DELETE FROM enrollment_financial_accounts
WHERE pricing_plan_code = '$planCode';

DELETE FROM pricing_plans
WHERE code = '$planCode';

COMMIT;
"@

$enrollmentCleanupSql = @"
BEGIN;

DELETE FROM enrollments
WHERE student_id = '$studentId'
  AND batch_id = '$batchId';

COMMIT;
"@

try {
    # ========================================================
    # Service health checks
    # ========================================================

    $services = @(
        @{
            Name = "Enrollment Service"
            Uri = "http://localhost:8083/actuator/health"
        },
        @{
            Name = "Finance Service"
            Uri = "http://localhost:8084/actuator/health"
        },
        @{
            Name = "Notification Service"
            Uri = "http://localhost:8085/actuator/health"
        }
    )

    foreach ($service in $services) {
        $health = Invoke-RestMethod `
            -Method Get `
            -Uri $service.Uri `
            -ErrorAction Stop

        Assert-Equal `
            -Actual $health.status `
            -Expected "UP" `
            -Message "$($service.Name) health check failed"
    }

    Write-Host `
        "`nEnrollment, Finance and Notification Services are UP." `
        -ForegroundColor Green

    # ========================================================
    # Remove any previous fixtures
    # ========================================================

    Invoke-DatabaseCommand `
        -Database "notification_db" `
        -Sql $notificationCleanupSql

    Invoke-DatabaseCommand `
        -Database "finance_db" `
        -Sql $financeCleanupSql

    Invoke-DatabaseCommand `
        -Database "enrollment_db" `
        -Sql $enrollmentCleanupSql

    Write-Host `
        "E2E cleanup check completed." `
        -ForegroundColor Green

    # ========================================================
    # Create pricing plan
    # ========================================================

    $pricingPlanBody = @{
        code = $planCode
        name = "Enrollment Payment E2E Plan"
        description =
            "Pricing plan used by the enrollment payment E2E verification"
        totalAmount = 1200.00
        currency = "XEE"
        installmentCount = 3
        initialPaymentAmount = 400.00
    } | ConvertTo-Json

    $pricingPlan = Invoke-RestMethod `
        -Method Post `
        -Uri "${financeBaseUri}/pricing-plans" `
        -ContentType "application/json" `
        -Body $pricingPlanBody `
        -ErrorAction Stop

    $pricingPlanId = [string]$pricingPlan.id

    Assert-Equal `
        -Actual $pricingPlan.code `
        -Expected $planCode `
        -Message "Pricing plan code is incorrect"

    Assert-Equal `
        -Actual $pricingPlan.status `
        -Expected "ACTIVE" `
        -Message "Pricing plan status is incorrect"

    Assert-Money `
        -Actual $pricingPlan.totalAmount `
        -Expected 1200.00 `
        -Message "Pricing plan total amount is incorrect"

    Assert-Money `
        -Actual $pricingPlan.initialPaymentAmount `
        -Expected 400.00 `
        -Message "Pricing plan initial payment is incorrect"

    Write-Host `
        "Active pricing plan was created." `
        -ForegroundColor Green

    # ========================================================
    # Create pending enrollment
    # ========================================================

    $enrollmentBody = @{
        studentId = $studentId
        batchId = $batchId
    } | ConvertTo-Json

    $enrollment = Invoke-RestMethod `
        -Method Post `
        -Uri $enrollmentBaseUri `
        -ContentType "application/json" `
        -Body $enrollmentBody `
        -ErrorAction Stop

    $enrollmentId = [string]$enrollment.id

    Assert-Equal `
        -Actual $enrollment.studentId `
        -Expected $studentId `
        -Message "Enrollment student ID is incorrect"

    Assert-Equal `
        -Actual $enrollment.batchId `
        -Expected $batchId `
        -Message "Enrollment batch ID is incorrect"

    Assert-Equal `
        -Actual $enrollment.status `
        -Expected "PENDING_PAYMENT" `
        -Message "New enrollment status is incorrect"

    Write-Host `
        "Enrollment was created with PENDING_PAYMENT status." `
        -ForegroundColor Green

    # ========================================================
    # Create financial account and installments
    # ========================================================

    $financialAccountBody = @{
        enrollmentId = $enrollmentId
        studentId = $studentId
        pricingPlanId = $pricingPlanId
        firstInstallmentDueDate =
            $firstInstallmentDueDate
    } | ConvertTo-Json

    $financialAccount = Invoke-RestMethod `
        -Method Post `
        -Uri "${financeBaseUri}/internal/enrollment-accounts" `
        -ContentType "application/json" `
        -Body $financialAccountBody `
        -ErrorAction Stop

    $financialAccountId =
        [string]$financialAccount.id

    Assert-Equal `
        -Actual $financialAccount.enrollmentId `
        -Expected $enrollmentId `
        -Message "Financial account enrollment ID is incorrect"

    Assert-Equal `
        -Actual $financialAccount.studentId `
        -Expected $studentId `
        -Message "Financial account student ID is incorrect"

    Assert-Equal `
        -Actual $financialAccount.status `
        -Expected "OPEN" `
        -Message "Financial account status is incorrect"

    Assert-Money `
        -Actual $financialAccount.totalAmount `
        -Expected 1200.00 `
        -Message "Financial account total amount is incorrect"

    Assert-Money `
        -Actual $financialAccount.paidAmount `
        -Expected 0.00 `
        -Message "Initial financial account paid amount is incorrect"

    Assert-Money `
        -Actual $financialAccount.remainingAmount `
        -Expected 1200.00 `
        -Message "Initial financial account remaining amount is incorrect"

    if ([bool]$financialAccount.initialPaymentSatisfied) {
        throw "Initial payment must not be satisfied before recording payment"
    }

    Assert-Equal `
        -Actual $financialAccount.installments.Count `
        -Expected 3 `
        -Message "Financial account installment count is incorrect"

    Write-Host `
        "Financial account and three installments were created." `
        -ForegroundColor Green

    # ========================================================
    # Record initial payment
    # ========================================================

    $paymentBody = @{
        amount = 400.00
        method = "CARD"
        externalReference =
            $paymentExternalReference
    } | ConvertTo-Json

    $payment = Invoke-RestMethod `
        -Method Post `
        -Uri "${financeBaseUri}/enrollment-accounts/${enrollmentId}/payments" `
        -ContentType "application/json" `
        -Body $paymentBody `
        -ErrorAction Stop

    $paymentId = [string]$payment.id

    Assert-Equal `
        -Actual $payment.enrollmentId `
        -Expected $enrollmentId `
        -Message "Payment enrollment ID is incorrect"

    Assert-Equal `
        -Actual $payment.financialAccountId `
        -Expected $financialAccountId `
        -Message "Payment financial account ID is incorrect"

    Assert-Equal `
        -Actual $payment.status `
        -Expected "RECORDED" `
        -Message "Payment status is incorrect"

    Assert-Equal `
        -Actual $payment.method `
        -Expected "CARD" `
        -Message "Payment method is incorrect"

    Assert-Money `
        -Actual $payment.amount `
        -Expected 400.00 `
        -Message "Payment amount is incorrect"

    Assert-Money `
        -Actual $payment.accountPaidAmount `
        -Expected 400.00 `
        -Message "Account paid amount after payment is incorrect"

    Assert-Money `
        -Actual $payment.accountRemainingAmount `
        -Expected 800.00 `
        -Message "Account remaining amount after payment is incorrect"

    if (-not ([bool]$payment.initialPaymentSatisfied)) {
        throw "Initial payment must be satisfied after recording payment"
    }

    Assert-Equal `
        -Actual $payment.allocations.Count `
        -Expected 1 `
        -Message "Payment allocation count is incorrect"

    Write-Host `
        "Initial payment was recorded and allocated." `
        -ForegroundColor Green

    # ========================================================
    # Verify enrollment activation task
    # ========================================================

    $activationTaskUri =
        "${financeBaseUri}/internal/enrollment-activation-tasks/by-enrollment/${enrollmentId}"

    $activationTask = $null

    for ($attempt = 1; $attempt -le 120; $attempt++) {
        $activationTask =
            Get-OptionalJson `
                -Uri $activationTaskUri

        if ($null -ne $activationTask) {
            $activationStatus =
                [string]$activationTask.status

            if (
                $activationStatus -eq "SUCCEEDED" -or
                $activationStatus -eq "FAILED"
            ) {
                break
            }
        }

        Start-Sleep -Milliseconds 500
    }

    if (
        $null -ne $activationTask -and
        [string]$activationTask.status -eq "PENDING"
    ) {
        Write-Host `
            "`nActivation task remained PENDING:" `
            -ForegroundColor Yellow

        Write-Host (
            $activationTask |
                ConvertTo-Json -Depth 10
        )
    }

    if ($null -eq $activationTask) {
        throw "Enrollment activation task was not created"
    }

    if ([string]$activationTask.status -eq "FAILED") {
        throw "Enrollment activation failed: $($activationTask.lastError)"
    }

    Assert-Equal `
        -Actual $activationTask.status `
        -Expected "SUCCEEDED" `
        -Message "Enrollment activation task status is incorrect"

    Assert-Equal `
        -Actual $activationTask.paymentId `
        -Expected $paymentId `
        -Message "Activation task payment ID is incorrect"

    Assert-Equal `
        -Actual $activationTask.attemptCount `
        -Expected 1 `
        -Message "Activation task attempt count is incorrect"

    Write-Host `
        "Enrollment activation task succeeded." `
        -ForegroundColor Green

    # ========================================================
    # Verify Enrollment Service received activation
    # ========================================================

    $activeEnrollment = $null

    for ($attempt = 1; $attempt -le 20; $attempt++) {
        $activeEnrollment = Invoke-RestMethod `
            -Method Get `
            -Uri "${enrollmentBaseUri}/${enrollmentId}" `
            -ErrorAction Stop

        if ([string]$activeEnrollment.status -eq "ACTIVE") {
            break
        }

        Start-Sleep -Milliseconds 250
    }

    Assert-Equal `
        -Actual $activeEnrollment.status `
        -Expected "ACTIVE" `
        -Message "Enrollment was not activated"

    Write-Host `
        "Enrollment status changed to ACTIVE." `
        -ForegroundColor Green

    # ========================================================
    # Verify updated financial account
    # ========================================================

    $updatedAccount = Invoke-RestMethod `
        -Method Get `
        -Uri "${financeBaseUri}/enrollment-accounts/${enrollmentId}" `
        -ErrorAction Stop

    Assert-Money `
        -Actual $updatedAccount.paidAmount `
        -Expected 400.00 `
        -Message "Updated account paid amount is incorrect"

    Assert-Money `
        -Actual $updatedAccount.remainingAmount `
        -Expected 800.00 `
        -Message "Updated account remaining amount is incorrect"

    if (-not ([bool]$updatedAccount.initialPaymentSatisfied)) {
        throw "Updated financial account did not satisfy initial payment"
    }

    Write-Host `
        "Financial account state is consistent." `
        -ForegroundColor Green

    # ========================================================
    # Verify payment notification
    # ========================================================

    $notificationHeaders = @{
        "X-User-Id" = $studentId
    }

    $notificationUri =
        "${notificationBaseUri}?type=PAYMENT_RECORDED&referenceType=ENROLLMENT&referenceId=${enrollmentId}&page=0&size=10"

    $notificationPage = $null

    for ($attempt = 1; $attempt -le 20; $attempt++) {
        $notificationPage = Invoke-RestMethod `
            -Method Get `
            -Uri $notificationUri `
            -Headers $notificationHeaders `
            -ErrorAction Stop

        if ([int]$notificationPage.totalElements -eq 1) {
            break
        }

        Start-Sleep -Milliseconds 250
    }

    Assert-Equal `
        -Actual $notificationPage.totalElements `
        -Expected 1 `
        -Message "Payment notification was not created"

    $notification =
        $notificationPage.content[0]

    Assert-Equal `
        -Actual $notification.recipientUserId `
        -Expected $studentId `
        -Message "Notification recipient is incorrect"

    Assert-Equal `
        -Actual $notification.type `
        -Expected "PAYMENT_RECORDED" `
        -Message "Notification type is incorrect"

    Assert-Equal `
        -Actual $notification.referenceType `
        -Expected "ENROLLMENT" `
        -Message "Notification reference type is incorrect"

    Assert-Equal `
        -Actual $notification.referenceId `
        -Expected $enrollmentId `
        -Message "Notification reference ID is incorrect"

    Assert-Equal `
        -Actual $notification.status `
        -Expected "UNREAD" `
        -Message "Notification status is incorrect"

    Assert-Equal `
        -Actual $notification.title `
        -Expected "Payment received" `
        -Message "Notification title is incorrect"

    Write-Host `
        "PAYMENT_RECORDED notification was created." `
        -ForegroundColor Green

    # ========================================================
    # Verify database consistency
    # ========================================================

    $databaseEnrollmentStatus =
        Invoke-ScalarQuery `
            -Database "enrollment_db" `
            -Sql "SELECT status FROM enrollments WHERE id = '$enrollmentId';"

    $databaseAccountPaidAmount =
        Invoke-ScalarQuery `
            -Database "finance_db" `
            -Sql "SELECT paid_amount FROM enrollment_financial_accounts WHERE id = '$financialAccountId';"

    $databasePaymentStatus =
        Invoke-ScalarQuery `
            -Database "finance_db" `
            -Sql "SELECT status FROM payments WHERE id = '$paymentId';"

    $databaseActivationStatus =
        Invoke-ScalarQuery `
            -Database "finance_db" `
            -Sql "SELECT status FROM enrollment_activation_tasks WHERE enrollment_id = '$enrollmentId';"

    $databaseNotificationCount =
        Invoke-ScalarQuery `
            -Database "notification_db" `
            -Sql "SELECT COUNT(*) FROM notifications WHERE recipient_user_id = '$studentId' AND type = 'PAYMENT_RECORDED' AND reference_id = '$enrollmentId';"

    Assert-Equal `
        -Actual $databaseEnrollmentStatus `
        -Expected "ACTIVE" `
        -Message "Enrollment database status is incorrect"

    Assert-Money `
        -Actual $databaseAccountPaidAmount `
        -Expected 400.00 `
        -Message "Finance database paid amount is incorrect"

    Assert-Equal `
        -Actual $databasePaymentStatus `
        -Expected "RECORDED" `
        -Message "Payment database status is incorrect"

    Assert-Equal `
        -Actual $databaseActivationStatus `
        -Expected "SUCCEEDED" `
        -Message "Activation database status is incorrect"

    Assert-Equal `
        -Actual $databaseNotificationCount `
        -Expected 1 `
        -Message "Notification database count is incorrect"

    Write-Host `
        "All service databases are consistent." `
        -ForegroundColor Green

    Write-Host `
        "`nEnrollment Payment End-to-End scenario passed." `
        -ForegroundColor Green
}
catch {
    Write-Host `
        "`nEnrollment Payment End-to-End scenario failed." `
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

        Invoke-DatabaseCommand `
            -Database "enrollment_db" `
            -Sql $enrollmentCleanupSql

        Write-Host `
            "End-to-End runtime fixtures removed." `
            -ForegroundColor DarkGray
    }
    catch {
        Write-Warning `
            "End-to-End cleanup failed: $($_.Exception.Message)"
    }
}