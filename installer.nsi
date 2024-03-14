; Kintsugi3DBuilder NSIS Installer script

;Include Modern UI

!include "MUI2.nsh"
!include "LangFile.nsh"

Name "Kintsugi 3D Builder"
RequestExecutionLevel admin
Unicode True
ManifestDPIAware True

InstallDir $PROGRAMFILES64\Kintsugi3DBuilder

; MUI Settings
!define MUI_ICON "Kintsugi3D.ico"
!define MUI_UNICON "Kintsugi3D.ico"
!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "Start Kintsugi 3D Builder"
!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchLink"
!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\target\classes\kintsugi3d-builder-about.txt"

; ---------------------------

; Installer Pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "target\classes\kintsugi3d-builder-about.txt"
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
    SetRegView 64

    SetOutPath $INSTDIR
    File "target\Kintsugi3DBuilder.exe"
    File "Kintsugi3D.ico"
    File "Kintsugi3D-icon.png"
    File "target\classes\kintsugi3d-builder-about.txt"

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
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "DefaultIcon" "$INSTDIR\Kintsugi3DBuilder.exe,0"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "UninstallString" '"$INSTDIR\uninstall.exe"'
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder" "NoRepair" 1
    WriteUninstaller "$INSTDIR\uninstall.exe"

SectionEnd

; Optional: Run installer executable for Kintsugi 3D Viewer
Section "Kintsugi 3D Viewer" SectionViewer

    SetOutPath $INSTDIR
    File "viewer\Kintsugi3DViewer.exe"

SectionEnd

Var ViewerInstalled

; Optional File Type associations
Section "File Type Associations" SectionAssociation

    SetRegView 64
        ; Associate .ibr files as Kintsugi 3D Builder Projects [for early dev legacy test projects]
        WriteRegStr HKCR ".ibr" "" "Kintsugi3DBuilder.Project"

        ; Associate .k3d files as Kintsugi 3D Builder Projects
        WriteRegStr HKCR ".k3d" "" "Kintsugi3DBuilder.Project"

        WriteRegStr HKCR "Kintsugi3DBuilder.Project" "" "Kintsugi 3D Builder Project"
        WriteRegStr HKCR "Kintsugi3DBuilder.Project\DefaultIcon" "" "$INSTDIR\Kintsugi3DBuilder.exe,0"
        WriteRegStr HKCR "Kintsugi3DBuilder.Project\Shell\Open\Command" "" '"$INSTDIR\Kintsugi3DBuilder.exe" "%1"'

        ; Associate .vset files as Kintsugi 3D Builder Viewsets
        WriteRegStr HKCR ".vset" "" "Kintsugi3DBuilder.Viewset"

        WriteRegStr HKCR "Kintsugi3DBuilder.Viewset" "" "Kintsugi 3D Builder Viewset"
        WriteRegStr HKCR "Kintsugi3DBuilder.Viewset\DefaultIcon" "" "$INSTDIR\Kintsugi3DBuilder.exe,0"
        WriteRegStr HKCR "Kintsugi3DBuilder.Viewset\Shell\Open\Command" "" '"$INSTDIR\Kintsugi3DBuilder.exe" "%1"'

    SectionGetFlags ${SectionViewer} $ViewerInstalled
    ${If} $ViewerInstalled == 1
        ; Associations for Kintsugi 3D Viewer
        WriteRegStr HKCR ".glb\OpenWithProgids" "Kintsugi3DViewer.glb" ""
        WriteRegStr HKCR ".gltf\OpenWithProgids" "Kintsugi3DViewer.gltf" ""

        WriteRegStr HKCR "Kintsugi3DViewer.glb" "" "Kintsugi 3D Viewer"
        WriteRegStr HKCR "Kintsugi3DViewer.gltf" "" "Kintsugi 3D Viewer"

        WriteRegStr HKCR "Kintsugi3DViewer.glb\DefaultIcon" "" "$INSTDIR\Kintsugi3DViewer.exe,0"
        WriteRegStr HKCR "Kintsugi3DViewer.glb\Shell\Open\Command" "" '"$INSTDIR\Kintsugi3DViewer.exe" "%1"'
        WriteRegStr HKCR "Kintsugi3DViewer.gltf\DefaultIcon" "" "$INSTDIR\Kintsugi3DViewer.exe,0"
        WriteRegStr HKCR "Kintsugi3DViewer.gltf\Shell\Open\Command" "" '"$INSTDIR\Kintsugi3DViewer.exe" "%1"'

        WriteRegStr HKCR "Applications\Kintsugi3DViewer.exe\DefaultIcon" "" "$INSTDIR\Kintsugi3DViewer.exe,0"
        WriteRegStr HKCR "Applications\Kintsugi3DViewer.exe\Shell\Open\Command" "" '"$INSTDIR\Kintsugi3DViewer.exe" "%1"'
        WriteRegStr HKCR "Applications\Kintsugi3DViewer.exe\SupportedTypes" ".gltf" ""
        WriteRegStr HKCR "Applications\Kintsugi3DViewer.exe\SupportedTypes" ".glb" ""
    ${EndIf}

SectionEnd

; Optional start menu shortcuts
Section "Start Menu Shortcuts" SectionShortcut

    CreateDirectory "$SMPROGRAMS\Kintsugi 3D"
    CreateShortcut "$SMPROGRAMS\Kintsugi 3D\Uninstall Kintsugi 3D.lnk" "$INSTDIR\uninstall.exe"
    CreateShortcut "$SMPROGRAMS\Kintsugi 3D\Kintsugi 3D Builder.lnk" "$INSTDIR\Kintsugi3DBuilder.exe"

    SectionGetFlags ${SectionViewer} $ViewerInstalled
    ${If} $ViewerInstalled == 1
        CreateShortcut "$SMPROGRAMS\Kintsugi 3D\Kintsugi 3D Viewer.lnk" "$INSTDIR\Kintsugi3DViewer.exe"
    ${EndIf}
SectionEnd

; Optional and default disabled Desktop shortcut
Section /o "Desktop Shortcut" SectionDesktop

    CreateShortcut "$DESKTOP\Kintsugi 3D Builder.lnk" "$INSTDIR\Kintsugi3DBuilder.exe"

    SectionGetFlags ${SectionViewer} $ViewerInstalled
    ${If} $ViewerInstalled == 1
        CreateShortcut "$DESKTOP\Kintsugi 3D Viewer.lnk" "$INSTDIR\Kintsugi3DViewer.exe"
    ${EndIf}

SectionEnd

; Uninstaller
Section "Uninstall"

    SetRegView 64

    ; Remove directories
    RMDir /r "$SMPROGRAMS\Kintsugi 3D"
    RMDir /r "$INSTDIR"

    ; Remove Desktop Shortcut
    Delete "$DESKTOP\Kintsugi 3D Builder.lnk"
    Delete "$DESKTOP\Kintsugi 3D Viewer.lnk"

    ; Remove registry keys
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DBuilder"
    DeleteRegKey HKLM "SOFTWARE\Kintsugi3DBuilder"
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Kintsugi3DViewer"
    DeleteRegKey HKLM "SOFTWARE\Kintsugi3DViewer"

    ; Remove file type associations
    DeleteRegKey HKCR ".ibr"
    DeleteRegKey HKCR ".k3d"
    DeleteRegKey HKCR ".vset"
    DeleteRegKey HKCR "Kintsugi3DBuilder.Project"
    DeleteRegKey HKCR "Kintsugi3DBuilder.Viewset"
    DeleteRegKey HKCR "Applications\Kintsugi3DViewer.exe"
    DeleteRegKey HKCR "Kintsugi3DViewer.glb"
    DeleteRegKey HKCR "Kintsugi3DViewer.gltf"
    DeleteRegValue HKCR ".glb\OpenWithProgids" "Kintsugi3DViewer.glb"
    DeleteRegValue HKCR ".gltf\OpenWithProgids" "Kintsugi3DViewer.gltf"

SectionEnd

; Run the application if requested after installation
Function LaunchLink

  ExecShell "" "$INSTDIR\Kintsugi3DBuilder.exe"

FunctionEnd

; Init function, read previous installation directory
Function .onInit

	SetRegView 64
	ClearErrors
	ReadRegStr $0 HKLM "Software\Kintsugi3DBuilder" "Install_Dir"

	${If} ${Errors}
    ${Else}
         StrCpy $INSTDIR $0
    ${EndIf}

FunctionEnd

LangString DESC_SectionApp ${LANG_ENGLISH} "The main Kintsugi 3D Builder Application. This will also install a local instance of the Java 11 Runtime that is necessary to run the application."
LangString DESC_SectionViewer ${LANG_ENGLISH} "Kintsugi 3D Viewer Application.  This allows the Viewer to be launched from the Builder application for previewing the results as they will appear in the Viewer app."
LangString DESC_SectionAssociation ${LANG_ENGLISH} "Set up Kintsugi 3D Builder Project file associations (.k3d and .vset)"
LangString DESC_SectionShortcut ${LANG_ENGLISH} "Install shortcuts so the application can be launched from the start menu"
LangString DESC_SectionDesktop ${LANG_ENGLISH} "Add a shortcut to Kintsugi 3D Builder to the desktop"

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SectionApp} $(DESC_SectionApp)
!insertmacro MUI_DESCRIPTION_TEXT ${SectionViewer} $(DESC_SectionViewer)
!insertmacro MUI_DESCRIPTION_TEXT ${SectionAssociation} $(DESC_SectionAssociation)
!insertmacro MUI_DESCRIPTION_TEXT ${SectionShortcut} $(DESC_SectionShortcut)
!insertmacro MUI_DESCRIPTION_TEXT ${SectionDesktop} $(DESC_SectionDesktop)
!insertmacro MUI_FUNCTION_DESCRIPTION_END