# Newtonite DB Setup Script
# Run this AFTER starting PostgreSQL service (as Administrator)
# Usage: .\setup-db.ps1

$pgBin = "C:\Program Files\PostgreSQL\18\bin"
$env:PGPASSWORD = "postgres"  # adjust if your postgres superuser has a different password

Write-Host "Creating 'newtonite' role and 'newtonite_ops' database..." -ForegroundColor Cyan

# Create user if not exists
& "$pgBin\psql.exe" -U postgres -p 5432 -c "DO `$`$ BEGIN IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'newtonite') THEN CREATE ROLE newtonite WITH LOGIN PASSWORD 'newtonite'; END IF; END `$`$;" 2>&1

# Create database if not exists
& "$pgBin\psql.exe" -U postgres -p 5432 -c "SELECT 'CREATE DATABASE newtonite_ops OWNER newtonite' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'newtonite_ops')\gexec" 2>&1

# Grant privileges
& "$pgBin\psql.exe" -U postgres -p 5432 -c "GRANT ALL PRIVILEGES ON DATABASE newtonite_ops TO newtonite;" 2>&1

Write-Host "Done! Testing connection..." -ForegroundColor Cyan
$env:PGPASSWORD = "newtonite"
& "$pgBin\psql.exe" -U newtonite -d newtonite_ops -p 5432 -c "SELECT current_database(), current_user;" 2>&1

Write-Host "`nSetup complete. Run 'mvn test' to execute the test suite." -ForegroundColor Green
