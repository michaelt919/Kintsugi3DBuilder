; IBRelight NSIS Installer script

;Include Modern UI

!include "MUI2.nsh"
!include "LangFile.nsh"

Name "IBRelight"
OutFile "target\IBRelight-Installer.exe"
RequestExecutionLevel admin
Unicode True
ManifestDPIAware True

InstallDir $PROGRAMFILES\IBRelight

InstallDirRegKey HKLM "Software\IBRelight" "Install_Dir"

; MUI Settings
!define MUI_ICON "ibr.ico"
!define MUI_UNICON "ibr.ico"
!define MUI_ABORTWARNING

; ---------------------------

; Installer Pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "ibrelight-about.txt"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

; Uninstaller Pages
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

; Language Files
!insertmacro MUI_LANGUAGE "English"

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

; Optional start menu shortcuts
Section "Start Menu Shortcuts"

    CreateDirectory "$SMPROGRAMS\IBRelight"
    CreateShortcut "$SMPROGRAMS\IBRelight\Uninstall.lnk" "$INSTDIR\uninstall.exe"
    CreateShortcut "$SMPROGRAMS\IBRelight\IBRelight.lnk" "$INSTDIR\IBRelight.exe"

SectionEnd

; Uninstaller
Section "Uninstall"

    ; Remove directories
    RMDir /r "$SMPROGRAMS\IBRelight"
    RMDir /r "$INSTDIR"

    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\IBRelight"
    DeleteRegKey HKLM SOFTWARE\IBRelight

SectionEnd
