; Kintsugi3DBuilder NSIS Installer script

;Include Modern UI

!include "MUI2.nsh"
!include "LangFile.nsh"

Name "Kintsugi 3D Builder"
OutFile "target\Kintsugi3DBuilder-Installer.exe"
RequestExecutionLevel admin
Unicode True
ManifestDPIAware True

InstallDir $PROGRAMFILES\Kintsugi3DBuilder

InstallDirRegKey HKLM "Software\Kintsugi3DBuilder" "Install_Dir"

; MUI Settings
!define MUI_ICON "ibr.ico"
!define MUI_UNICON "ibr.ico"
!define MUI_ABORTWARNING

; ---------------------------

; Installer Pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "kintsugi3d-builder-about.txt"
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
Section "Kintsugi 3D Builder (required)"

    SectionIn RO

    SetOutPath $INSTDIR
    File "target\Kintsugi3DBuilder.exe"
    File "ibr.ico"
    File "kintsugi3d-builder-about.txt"

    ; Include shaders
    SetOutPath "$INSTDIR\shaders"
    File /r "shaders\*"

    ; Write install directory registry key
    WriteRegStr HKLM "SOFTWARE\Kintsugi3DBuilder" "Install_Dir" "$INSTDIR"

    ; Write uninstall keys to registry
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "DisplayName" "Kintsugi 3D Builder"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "UninstallString" '"$INSTDIR\uninstall.exe"'
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "NoRepair" 1
    WriteUninstaller "$INSTDIR\uninstall.exe"

SectionEnd

; Bundled JRE
Section "Bundled Java Runtime"

    ; Include JRE
    SetOutPath "$INSTDIR\jre"
    File /r "jre\*"

SectionEnd

; Optional start menu shortcuts
Section "Start Menu Shortcuts"

    CreateDirectory "$SMPROGRAMS\Kintsugi3DBuilder"
    CreateShortcut "$SMPROGRAMS\Kintsugi3DBuilder\Uninstall.lnk" "$INSTDIR\uninstall.exe"
    CreateShortcut "$SMPROGRAMS\Kintsugi3DBuilder\Kintsugi 3D Builder.lnk" "$INSTDIR\Kintsugi3DBuilder.exe"

SectionEnd

; Uninstaller
Section "Uninstall"

    ; Remove directories
    RMDir /r "$SMPROGRAMS\Kintsugi3DBuilder"
    RMDir /r "$INSTDIR"

    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder"
    DeleteRegKey HKLM "SOFTWARE\Kintsugi3DBuilder"

SectionEnd
