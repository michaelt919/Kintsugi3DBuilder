; IBRelight NSIS Installer script

Name "IBRelight"
OutFile "target\IBRelight-Installer.exe"
RequestExecutionLevel admin
Unicode True

InstallDir $PROGRAMFILES\IBRelight

InstallDirRegKey HKLM "Software\IBRelight" "Install_Dir"

; ---------------------------

; Pages
Page components
Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instFiles

; ---------------------------

; Main installation
Section "IBRelight (required)"

    SectionIn RO

    SetOutPath $INSTDIR
    File "target\IBRelight.exe"
    File "ibr.ico"
    File "ibrelight-about.txt"

    ; Include shaders
    SetOutPath "$INSTDIR\shaders"
    File /r "shaders\*"

    ; Write install directory registry key
    WriteRegStr HKLM SOFTWARE\IBRelight "Install_Dir" "$INSTDIR"

    ; Write uninstall keys to registry
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\IBRelight" "DisplayName" "IBRelight"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\IBRelight" "UninstallString" '"$INSTDIR\uninstall.exe"'
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\IBRelight" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\IBRelight" "NoRepair" 1
    WriteUninstaller "$INSTDIR\uninstall.exe"

SectionEnd

; ---------------------------

; Optional start menu shortcuts
Section "Start Menu Shortcuts"

    CreateDirectory "$SMPROGRAMS\IBRelight"
    CreateShortcut "$SMPROGRAMS\IBRelight\Uninstall.lnk" "$INSTDIR\uninstall.exe"
    CreateShortcut "$SMPROGRAMS\IBRelight\IBRelight.lnk" "$INSTDIR\IBRelight.exe"

SectionEnd

; ---------------------------

; Uninstaller
Section "Uninstall"

    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\IBRelight"
    DeleteRegKey HKLM SOFTWARE\IBRelight

    ; Remove files and uninstaller
    Delete "$INSTDIR\IBRelight.exe"
    RMDir /R "$INSTDIR\shaders"
    Delete "$INSTDIR\*"

    Delete $INSTDIR\uninstall.exe

    ; Remove shortcuts, if any
    Delete "$SMPROGRAMS\IBRelight\*.lnk"

    ; Remove directories
    RMDir "$SMPROGRAMS\IBRelight"
    RMDir "$INSTDIR"

SectionEnd
