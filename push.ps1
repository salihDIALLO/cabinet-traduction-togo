# push.ps1 — Pousse les commits vers GitHub (perso/feat/cabinet-traduction-togo)
# Usage : .\push.ps1
# Usage : .\push.ps1 "mon message de commit"

param(
    [string]$message = ""
)

Set-Location $PSScriptRoot

# Stage + commit si un message est fourni
if ($message -ne "") {
    git add -A
    git commit -m $message
}

# Push — on supprime le 2>&1 pour eviter les faux rouges PowerShell
$output = git push perso feat/cabinet-traduction-togo 2>&1
$exitCode = $LASTEXITCODE

foreach ($line in $output) {
    if ($line -match "error:|fatal:" ) {
        Write-Host $line -ForegroundColor Red
    } elseif ($line -match "\.\.") {
        # Ligne de résumé du push (ex: 9517fda..8fd5968)
        Write-Host $line -ForegroundColor Green
    } else {
        Write-Host $line -ForegroundColor Cyan
    }
}

if ($exitCode -eq 0) {
    Write-Host ""
    Write-Host "Push OK -> https://github.com/salihDIALLO/cabinet-traduction-togo" -ForegroundColor Green
} else {
    Write-Host "Push echoue (code $exitCode)" -ForegroundColor Red
}
