Set-Location $PSScriptRoot
$env:PYTHONUTF8 = "1"
python .\python\lan_device_manager.py
if ($LASTEXITCODE -ne 0) {
    Write-Host "Python 程序退出，按任意键继续..." -ForegroundColor Yellow
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
