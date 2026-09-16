# Launcher for LAN Device Manager (interactive menu)
# Double-click or right-click -> Run with PowerShell to start.
# Note: execution policy may block scripts; run `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` if needed.
nSet-Location -Path $PSScriptRoot
# Ensure console uses UTF-8 for correct Chinese output
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
n$python = 'python'ntry {
    & $python ".\python\lan_device_manager.py" @args
} catch {
    Write-Error $_
    exit 1
}

Read-Host -Prompt '按回车退出'