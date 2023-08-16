; Kintsugi3DBuilder NSIS Installer script

;Include Modern UI

!include "MUI2.nsh"
!include "LangFile.nsh"

Name "Kintsugi 3D Builder"
RequestExecutionLevel admin
Unicode True
ManifestDPIAware True

InstallDir $PROGRAMFILES\Kintsugi3DBuilder

InstallDirRegKey HKLM "Software\Kintsugi3DBuilder" "Install_Dir"

; MUI Settings
!define MUI_ICON "ibr.ico"
!define MUI_UNICON "ibr.ico"
!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "Start Kintsugi 3D Builder"
!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchLink"
!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\kintsugi3d-builder-about.txt"

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
Section "Kintsugi 3D Builder (required)" SectionApp

    SectionIn RO

    SetOutPath $INSTDIR
    File "target\Kintsugi3DBuilder.exe"
    File "ibr.ico"
    File "ibr-icon.png"
    File "kintsugi3d-builder-about.txt"

    ; Include shaders
    SetOutPath "$INSTDIR\shaders"
    File /r "shaders\*"

    ; Include JRE
    SetOutPath "$INSTDIR\jre"
    File /r "jre\*"

    ; Write install directory registry key
    WriteRegStr HKLM "SOFTWARE\Kintsugi3DBuilder" "Install_Dir" "$INSTDIR"

    ; Write uninstall keys to registry
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "DisplayName" "Kintsugi 3D Builder"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "UninstallString" '"$INSTDIR\uninstall.exe"'
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "NoRepair" 1
    WriteUninstaller "$INSTDIR\uninstall.exe"

SectionEnd

; Optional File Type associations
Section "File Type Associations" SectionAssociation

    ; Associate .ibr files as Kintsugi 3D Builder Projects
    WriteRegStr HKCR ".ibr" "" "Kintsugi3DBuilder.Project"

    WriteRegStr HKCR "Kintsugi3DBuilder.Project" "" "Kintsugi 3D Builder Project"
    WriteRegStr HKCR "Kintsugi3DBuilder.Project\DefaultIcon" "" "$INSTDIR\Kintsugi3DBuilder.exe,0"
    WriteRegStr HKCR "Kintsugi3DBuilder.Project\Shell\Open\Command" "" '"$INSTDIR\Kintsugi3DBuilder.exe" "%1"'

    ; Associate .vset files as Kintsugi 3D Builder Viewsets
    WriteRegStr HKCR ".vset" "" "Kintsugi3DBuilder.Viewset"

    WriteRegStr HKCR "Kintsugi3DBuilder.Viewset" "" "Kintsugi 3D Builder Viewset"
    WriteRegStr HKCR "Kintsugi3DBuilder.Viewset\DefaultIcon" "" "$INSTDIR\Kintsugi3DBuilder.exe,0"
    WriteRegStr HKCR "Kintsugi3DBuilder.Viewset\Shell\Open\Command" "" '"$INSTDIR\Kintsugi3DBuilder.exe" "%1"'

SectionEnd

; Optional start menu shortcuts
Section "Start Menu Shortcuts" SectionShortcut

    CreateDirectory "$SMPROGRAMS\Kintsugi3DBuilder"
    CreateShortcut "$SMPROGRAMS\Kintsugi3DBuilder\Uninstall.lnk" "$INSTDIR\uninstall.exe"
    CreateShortcut "$SMPROGRAMS\Kintsugi3DBuilder\Kintsugi 3D Builder.lnk" "$INSTDIR\Kintsugi3DBuilder.exe"

SectionEnd

; Optional and default disabled Desktop shortcut
Section /o "Desktop Shortcut" SectionDesktop

    CreateShortcut "$DESKTOP\Kintsugi 3D Builder.lnk" "$INSTDIR\Kintsugi3DBuilder.exe"

SectionEnd

; Uninstaller
Section "Uninstall"

    ; Remove directories
    RMDir /r "$SMPROGRAMS\Kintsugi3DBuilder"
    RMDir /r "$INSTDIR"

    ; Remove Desktop Shortcut
    Delete "$DESKTOP\Kintsugi 3D Builder.lnk"

    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder"
    DeleteRegKey HKLM "SOFTWARE\Kintsugi3DBuilder"

    ; Remove file type associations
    DeleteRegKey HKCR ".ibr"
    DeleteRegKey HKCR ".vset"
    DeleteRegKey HKCR "Kintsugi3DBuilder.Project"
    DeleteRegKey HKCR "Kintsugi3DBuilder.Viewset"

SectionEnd

; Run the application if requested after installation
Function LaunchLink

  ExecShell "" "$INSTDIR\Kintsugi3DBuilder.exe"

FunctionEnd

LangString DESC_SectionApp ${LANG_ENGLISH} "The main Kintsugi 3D Builder Application. This will also install a local instance of the Java 11 Runtime that is necessary to run the application."
LangString DESC_SectionAssociation ${LANG_ENGLISH} "Set up Kintsugi 3D Builder Project file associations (.ibr and .vset)"
LangString DESC_SectionShortcut ${LANG_ENGLISH} "Install shortcuts so the application can be launched from the start menu"
LangString DESC_SectionDesktop ${LANG_ENGLISH} "Add a shortcut to Kintsugi 3D Builder to the desktop"

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SectionApp} $(DESC_SectionApp)
!insertmacro MUI_DESCRIPTION_TEXT ${SectionAssociation} $(DESC_SectionAssociation)
!insertmacro MUI_DESCRIPTION_TEXT ${SectionShortcut} $(DESC_SectionShortcut)
!insertmacro MUI_DESCRIPTION_TEXT ${SectionDesktop} $(DESC_SectionDesktop)
!insertmacro MUI_FUNCTION_DESCRIPTION_END