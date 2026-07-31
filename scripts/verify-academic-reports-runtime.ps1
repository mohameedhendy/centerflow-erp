Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-AcademicSql {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Sql
    )

    $Sql |
        docker compose exec -T postgres `
            psql `
            -U centerflow_admin `
            -d academic_db `
            -v ON_ERROR_STOP=1

    if ($LASTEXITCODE -ne 0) {
        throw "Academic database command failed"
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

$branchId = [guid]::NewGuid().ToString()
$classroomId = [guid]::NewGuid().ToString()
$courseId = [guid]::NewGuid().ToString()
$courseLevelId = [guid]::NewGuid().ToString()
$instructorId = [guid]::NewGuid().ToString()
$batchId = [guid]::NewGuid().ToString()

$completedSessionId = [guid]::NewGuid().ToString()
$plannedSessionId = [guid]::NewGuid().ToString()

$firstStudentId = [guid]::NewGuid().ToString()
$secondStudentId = [guid]::NewGuid().ToString()
$thirdStudentId = [guid]::NewGuid().ToString()
$fourthStudentId = [guid]::NewGuid().ToString()

$firstSeatId = [guid]::NewGuid().ToString()
$secondSeatId = [guid]::NewGuid().ToString()

$firstSeatEnrollmentId = [guid]::NewGuid().ToString()
$secondSeatEnrollmentId = [guid]::NewGuid().ToString()

$firstAttendanceId = [guid]::NewGuid().ToString()
$secondAttendanceId = [guid]::NewGuid().ToString()
$thirdAttendanceId = [guid]::NewGuid().ToString()
$fourthAttendanceId = [guid]::NewGuid().ToString()

$firstAttendanceEnrollmentId = [guid]::NewGuid().ToString()
$secondAttendanceEnrollmentId = [guid]::NewGuid().ToString()
$thirdAttendanceEnrollmentId = [guid]::NewGuid().ToString()
$fourthAttendanceEnrollmentId = [guid]::NewGuid().ToString()

$suffix = [guid]::NewGuid().ToString("N").Substring(0, 8).ToUpper()
$reportDate = [DateTime]::UtcNow.ToString("yyyy-MM-dd")

$baseUri = "http://localhost:8082/api/v1/academic/reports"

$cleanupSql = @"
BEGIN;

DELETE FROM attendance_records
WHERE session_id IN (
    '$completedSessionId',
    '$plannedSessionId'
);

DELETE FROM seat_reservations
WHERE batch_id = '$batchId';

DELETE FROM batch_sessions
WHERE batch_id = '$batchId';

DELETE FROM batches
WHERE id = '$batchId';

DELETE FROM instructors
WHERE id = '$instructorId';

DELETE FROM course_levels
WHERE id = '$courseLevelId';

DELETE FROM courses
WHERE id = '$courseId';

DELETE FROM classrooms
WHERE id = '$classroomId';

DELETE FROM branches
WHERE id = '$branchId';

COMMIT;
"@

$setupSql = @"
BEGIN;

INSERT INTO branches
(
    id,
    code,
    name,
    city,
    active,
    created_at,
    updated_at
)
VALUES
(
    '$branchId',
    'RPT-BR-$suffix',
    'Runtime Report Branch',
    'Cairo',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO classrooms
(
    id,
    branch_id,
    code,
    name,
    capacity,
    floor,
    active,
    created_at,
    updated_at
)
VALUES
(
    '$classroomId',
    '$branchId',
    'RPT-RM-$suffix',
    'Runtime Report Classroom',
    30,
    'First',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO courses
(
    id,
    code,
    name,
    description,
    active,
    created_at,
    updated_at
)
VALUES
(
    '$courseId',
    'RPT-CR-$suffix',
    'Runtime Java Course',
    'Academic report runtime course',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO course_levels
(
    id,
    course_id,
    code,
    name,
    sequence_number,
    duration_hours,
    description,
    active,
    created_at,
    updated_at
)
VALUES
(
    '$courseLevelId',
    '$courseId',
    'RPT-LV-$suffix',
    'Runtime Spring Level',
    1,
    60,
    'Academic report runtime level',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO instructors
(
    id,
    code,
    first_name,
    last_name,
    email,
    specialization,
    active,
    created_at,
    updated_at
)
VALUES
(
    '$instructorId',
    'RPT-IN-$suffix',
    'Runtime',
    'Instructor',
    'runtime-$suffix@centerflow.test',
    'Java Backend',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO batches
(
    id,
    code,
    name,
    branch_id,
    classroom_id,
    course_level_id,
    instructor_id,
    capacity,
    start_date,
    end_date,
    status,
    created_at,
    updated_at
)
VALUES
(
    '$batchId',
    'RPT-BT-$suffix',
    'Runtime Academic Report Batch',
    '$branchId',
    '$classroomId',
    '$courseLevelId',
    '$instructorId',
    20,
    '$reportDate',
    '$reportDate',
    'IN_PROGRESS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO batch_sessions
(
    id,
    batch_id,
    session_date,
    start_time,
    end_time,
    topic,
    status,
    created_at,
    updated_at
)
VALUES
(
    '$completedSessionId',
    '$batchId',
    '$reportDate',
    '18:00:00',
    '20:00:00',
    'Completed runtime session',
    'COMPLETED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO batch_sessions
(
    id,
    batch_id,
    session_date,
    start_time,
    end_time,
    topic,
    status,
    created_at,
    updated_at
)
VALUES
(
    '$plannedSessionId',
    '$batchId',
    '$reportDate',
    '20:00:00',
    '22:00:00',
    'Planned runtime session',
    'PLANNED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO seat_reservations
(
    id,
    batch_id,
    enrollment_id,
    status,
    reserved_at,
    created_at,
    updated_at
)
VALUES
(
    '$firstSeatId',
    '$batchId',
    '$firstSeatEnrollmentId',
    'RESERVED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO seat_reservations
(
    id,
    batch_id,
    enrollment_id,
    status,
    reserved_at,
    created_at,
    updated_at
)
VALUES
(
    '$secondSeatId',
    '$batchId',
    '$secondSeatEnrollmentId',
    'RESERVED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO attendance_records
(
    id,
    session_id,
    enrollment_id,
    student_id,
    status,
    notes,
    marked_at,
    created_at,
    updated_at
)
VALUES
(
    '$firstAttendanceId',
    '$completedSessionId',
    '$firstAttendanceEnrollmentId',
    '$firstStudentId',
    'PRESENT',
    'Runtime present student',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO attendance_records
(
    id,
    session_id,
    enrollment_id,
    student_id,
    status,
    notes,
    marked_at,
    created_at,
    updated_at
)
VALUES
(
    '$secondAttendanceId',
    '$completedSessionId',
    '$secondAttendanceEnrollmentId',
    '$secondStudentId',
    'ABSENT',
    'Runtime absent student',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO attendance_records
(
    id,
    session_id,
    enrollment_id,
    student_id,
    status,
    notes,
    marked_at,
    created_at,
    updated_at
)
VALUES
(
    '$thirdAttendanceId',
    '$completedSessionId',
    '$thirdAttendanceEnrollmentId',
    '$thirdStudentId',
    'LATE',
    'Runtime late student',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO attendance_records
(
    id,
    session_id,
    enrollment_id,
    student_id,
    status,
    notes,
    marked_at,
    created_at,
    updated_at
)
VALUES
(
    '$fourthAttendanceId',
    '$completedSessionId',
    '$fourthAttendanceEnrollmentId',
    '$fourthStudentId',
    'EXCUSED',
    'Runtime excused student',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

COMMIT;
"@

try {
    $health = Invoke-RestMethod `
        -Method Get `
        -Uri "http://localhost:8082/actuator/health" `
        -ErrorAction Stop

    Assert-Equal `
        -Actual ([string]$health.status) `
        -Expected "UP" `
        -Message "Academic Service health check failed"

    Write-Host "`nAcademic Service health is UP." -ForegroundColor Green

    Invoke-AcademicSql -Sql $cleanupSql
    Invoke-AcademicSql -Sql $setupSql

    Write-Host "Academic Report runtime fixtures created." -ForegroundColor Green

    $overviewUri = "${baseUri}/overview?branchId=${branchId}&fromDate=${reportDate}&toDate=${reportDate}"

    $overview = Invoke-RestMethod `
        -Method Get `
        -Uri $overviewUri `
        -ErrorAction Stop

    Assert-Equal ([int]$overview.batches.totalBatches) 1 "Overview batch total is incorrect"
    Assert-Equal ([int]$overview.batches.inProgressBatches) 1 "Overview in-progress batch total is incorrect"
    Assert-Equal ([int]$overview.sessions.totalSessions) 2 "Overview session total is incorrect"
    Assert-Equal ([int]$overview.sessions.completedSessions) 1 "Overview completed session total is incorrect"
    Assert-Equal ([int]$overview.sessions.plannedSessions) 1 "Overview planned session total is incorrect"
    Assert-Equal ([int]$overview.currentlyReservedSeats) 2 "Overview reserved-seat total is incorrect"
    Assert-Equal ([int]$overview.attendance.totalRecords) 4 "Overview attendance total is incorrect"
    Assert-Equal ([int]$overview.attendance.present) 1 "Overview present total is incorrect"
    Assert-Equal ([int]$overview.attendance.absent) 1 "Overview absent total is incorrect"
    Assert-Equal ([int]$overview.attendance.late) 1 "Overview late total is incorrect"
    Assert-Equal ([int]$overview.attendance.excused) 1 "Overview excused total is incorrect"

    if ([math]::Abs([double]$overview.attendance.attendanceRate - 66.67) -gt 0.001) {
        throw "Overview attendance rate is incorrect. Actual: $($overview.attendance.attendanceRate)"
    }

    Write-Host "Academic overview report passed." -ForegroundColor Green

    $batchReportUri = "${baseUri}/batches/${batchId}?fromDate=${reportDate}&toDate=${reportDate}"

    $batchReport = Invoke-RestMethod `
        -Method Get `
        -Uri $batchReportUri `
        -ErrorAction Stop

    Assert-Equal ([string]$batchReport.batchId) $batchId "Batch report returned the wrong batch"
    Assert-Equal ([string]$batchReport.batchStatus) "IN_PROGRESS" "Batch report returned the wrong status"
    Assert-Equal ([string]$batchReport.courseName) "Runtime Java Course" "Batch report returned the wrong course"
    Assert-Equal ([string]$batchReport.instructorName) "Runtime Instructor" "Batch report returned the wrong instructor"
    Assert-Equal ([int]$batchReport.sessions.totalSessions) 2 "Batch report session total is incorrect"
    Assert-Equal ([int]$batchReport.currentlyReservedSeats) 2 "Batch report reserved-seat total is incorrect"

    if ([math]::Abs([double]$batchReport.attendance.attendanceRate - 66.67) -gt 0.001) {
        throw "Batch attendance rate is incorrect. Actual: $($batchReport.attendance.attendanceRate)"
    }

    Write-Host "Batch academic report passed." -ForegroundColor Green

    $studentReportUri = "${baseUri}/students/${firstStudentId}/attendance?batchId=${batchId}&fromDate=${reportDate}&toDate=${reportDate}"

    $studentReport = Invoke-RestMethod `
        -Method Get `
        -Uri $studentReportUri `
        -ErrorAction Stop

    Assert-Equal ([string]$studentReport.studentId) $firstStudentId "Student report returned the wrong student"
    Assert-Equal ([int]$studentReport.attendedBatches) 1 "Student attended-batch total is incorrect"
    Assert-Equal ([int]$studentReport.sessionsWithAttendance) 1 "Student attendance-session total is incorrect"
    Assert-Equal ([int]$studentReport.attendance.present) 1 "Student present total is incorrect"

    if ([math]::Abs([double]$studentReport.attendance.attendanceRate - 100.00) -gt 0.001) {
        throw "Student attendance rate is incorrect. Actual: $($studentReport.attendance.attendanceRate)"
    }

    Write-Host "Student attendance report passed." -ForegroundColor Green

    $databaseResult = docker compose exec -T postgres `
        psql `
        -U centerflow_admin `
        -d academic_db `
        -t `
        -A `
        -F "|" `
        -c "SELECT (SELECT COUNT(*) FROM batches WHERE id = '$batchId'), (SELECT COUNT(*) FROM batch_sessions WHERE batch_id = '$batchId'), (SELECT COUNT(*) FROM seat_reservations WHERE batch_id = '$batchId' AND status = 'RESERVED'), (SELECT COUNT(*) FROM attendance_records WHERE session_id = '$completedSessionId');"

    if ($LASTEXITCODE -ne 0) {
        throw "Academic database verification query failed"
    }

    $databaseLine = (($databaseResult | Out-String).Trim())
    $parts = $databaseLine.Split("|")

    Assert-Equal $parts.Count 4 "Unexpected database verification result"
    Assert-Equal ([int]$parts[0]) 1 "Runtime batch total is incorrect"
    Assert-Equal ([int]$parts[1]) 2 "Runtime session total is incorrect"
    Assert-Equal ([int]$parts[2]) 2 "Runtime reserved-seat total is incorrect"
    Assert-Equal ([int]$parts[3]) 4 "Runtime attendance total is incorrect"

    Write-Host "Academic Report database records are consistent." -ForegroundColor Green
    Write-Host "`nAll Academic Report runtime checks passed." -ForegroundColor Green
}
catch {
    Write-Host "`nAcademic Report runtime verification failed." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    throw
}
finally {
    try {
        Invoke-AcademicSql -Sql $cleanupSql
        Write-Host "Academic Report runtime fixtures removed." -ForegroundColor DarkGray
    }
    catch {
        Write-Warning "Academic Report runtime fixture cleanup failed: $($_.Exception.Message)"
    }
}