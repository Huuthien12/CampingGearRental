param(
    [string] $JdbcUrl = 'jdbc:mysql://127.0.0.1:3306/camping_gear_rental_test?serverTimezone=UTC',
    [string] $Username = 'camping_test_runner'
)

if ($JdbcUrl -notmatch '^jdbc:mysql://[^/]+/camping_gear_rental_test(?:$|[?;].*)$') { throw 'JDBC URL must target exactly camping_gear_rental_test.' }
if ($Username -ne 'camping_test_runner') { throw 'Username must be camping_test_runner.' }

$securePassword = Read-Host 'MySQL TEST password' -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:CAMPING_TEST_DB_URL = $JdbcUrl
    $env:CAMPING_TEST_DB_USERNAME = $Username
    $env:CAMPING_TEST_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    & mvn '-DexcludedTags=' '-DincludedTags=mysql-integration' '-Dtest=RentalLifecycleMySqlIntegrationTest' test
    exit $LASTEXITCODE
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    Remove-Item Env:CAMPING_TEST_DB_URL, Env:CAMPING_TEST_DB_USERNAME, Env:CAMPING_TEST_DB_PASSWORD -ErrorAction SilentlyContinue
}
