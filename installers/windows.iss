[Setup]
AppName=WWSCC Applications
AppVersion=@VERSION@
OutputDir=.
OutputBaseFilename=WWSCCSetup-@VERSION@
AppPublisher=Brett Wilson
DefaultDirName={pf}\nwrsc
DefaultGroupName=WW Timing
AllowNoIcons=yes
Compression=lzma
SolidCompression=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Types]
Name: "full"; Description: "Full installation"
Name: "custom"; Description: "Custom installation"; Flags: iscustom

[Components]
Name: "java"; Description: "Java Applications"; Types: full
Name: "python"; Description: "Python And Web Service"; Types: full

[Dirs]
Name: "{app}\java"; Permissions: users-modify
Name: "{userdocs}\nwrsc\series"; Permissions: users-modify; Flags: uninsneveruninstall
Name: "{userdocs}\nwrsc\archive"; Permissions: users-modify; Flags: uninsneveruninstall
Name: "{userdocs}\nwrsc\backup"; Permissions: users-modify; Flags: uninsneveruninstall

[InstallDelete]
Type: files; Name: "{app}\java\wwsccapps*.jar";

[Files]
Source: "wwsccapps-@VERSION@.jar"; DestDir: "{app}\java"; Flags: ignoreversion; Components: java
Source: "installers\windows\python\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs; Components: python
Source: "installers\windows\XYNTService.exe"; DestDir: "{app}"; Flags: ignoreversion; Components: python
Source: "installers\windows\XYNTService.ini"; DestDir: "{app}"; Flags: ignoreversion; Components: python
Source: "installers\windows\nwrsc.ini"; DestDir: "{userdocs}\nwrsc"; Flags: ignoreversion; Components: python

[Icons]
Name: "{group}\DataEntry"; Filename: "{code:getJavaPath}\javaw"; Parameters: "-jar wwsccapps-@VERSION@.jar DataEntry";  WorkingDir: "{app}\java"; Components: java
Name: "{group}\Registration"; Filename: "{code:getJavaPath}\javaw"; Parameters: "-jar wwsccapps-@VERSION@.jar Registration";  WorkingDir: "{app}\java"; Components: java
Name: "{group}\BW Timer"; Filename: "{code:getJavaPath}\javaw"; Parameters: "-jar wwsccapps-@VERSION@.jar BWTimer";  WorkingDir: "{app}\java"; Components: java
Name: "{group}\Pro Timer"; Filename: "{code:getJavaPath}\javaw"; Parameters: "-jar wwsccapps-@VERSION@.jar ProTimer";  WorkingDir: "{app}\java"; Components: java
Name: "{group}\Challenge"; Filename: "{code:getJavaPath}\javaw"; Parameters: "-jar wwsccapps-@VERSION@.jar ChallengeGUI";  WorkingDir: "{app}\java"; Components: java
Name: "{group}\Uninstall"; Filename: "{app}\unins000.exe"; WorkingDir: "{app}\java"; Components: java

[Registry]
Root: HKCU; Subkey: "Software\JavaSoft\Prefs\org\wwscc\util"; ValueType: string; ValueName: "installroot"; ValueData: "{userdocs}\nwrsc"

[INI]
Filename: "{app}\XYNTService.ini"; Section: "Process0"; Key: "CommandLine"; String: "pythonw.exe Scripts\paster-script.py serve ""{userdocs}\nwrsc\nwrsc.ini"""; Components: python
Filename: "{app}\XYNTService.ini"; Section: "Process0"; Key: "WorkingDir"; String: """{app}"""; Components: python
Filename: "{app}\XYNTService.ini"; Section: "Process1"; Key: "CommandLine"; String: "pythonw.exe dbDiscovery.py ""{userdocs}\nwrsc\nwrsc.ini"""; Components: python
Filename: "{app}\XYNTService.ini"; Section: "Process1"; Key: "WorkingDir"; String: """{app}"""; Components: python
Filename: "{userdocs}\nwrsc\nwrsc.ini"; Section: "handler_errorlog"; Key: "args"; String: "(r'{userdocs}\nwrsc\nwrsc.log', 'a', 1000000, 10)"; Components: python

[Run]
Filename: "{app}\XYNTService.exe"; Parameters: "-i"; Components: python
Filename: "{app}\XYNTService.exe"; Parameters: "-r NWRSc"; Components: python
# mkdir $DEST
# cd $DEST
# $PYTHONPATH/python -m venv python
# $DEST/python/Scripts/pip install $WHEELS/nwrsc-2.0-py3-none-any.whl -f file:$WHEELS


[UninstallRun]
Filename: "{app}\XYNTService.exe"; Parameters: "-k NWRSc"; Components: python
Filename: "{app}\XYNTService.exe"; Parameters: "-u"; Components: python

[Code]

var
  JavaPath: string;

function getJavaPath(Param: String): String;
begin
  Result := JavaPath + '\bin';
end;

function InitializeSetup(): Boolean;
var
 ErrorCode: Integer;
 JavaInstalled : Boolean;
 Result1 : Boolean;
 Versions: TArrayOfString;
 I: Integer;
begin
  // Make sure service is stopped if its present so that files can be replaced
 Exec('sc', 'stop NWRSc', '', SW_SHOW, ewWaitUntilTerminated, ErrorCode);

 if RegGetSubkeyNames(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment', Versions) then
 begin
  for I := 0 to GetArrayLength(Versions)-1 do
   if JavaInstalled = true then
   begin
    //do nothing
   end else
   begin
    if ( Versions[I][2]='.' ) and ( ( StrToInt(Versions[I][1]) > 1 ) or ( ( StrToInt(Versions[I][1]) = 1 ) and ( StrToInt(Versions[I][3]) >= 6 ) ) ) then
    begin
     RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment\'+Versions[I], 'JavaHome', JavaPath);
     JavaInstalled := true;
    end else
    begin
     JavaInstalled := false;
    end;
   end;
 end else
 begin
  JavaInstalled := false;
 end;

 if JavaInstalled then
 begin
  Result := true;
 end else
    begin
  Result1 := MsgBox('This tool requires Java Runtime Environment version 1.6 or newer to run. Please download and install the JRE and run this setup again. Do you want to download it now?',
   mbConfirmation, MB_YESNO) = idYes;
  if Result1 = false then
  begin
   Result:=false;
  end else
  begin
   Result:=false;
   ShellExec('open','http://www.java.com/getjava/','','',SW_SHOWNORMAL,ewNoWait,ErrorCode);
  end;
    end;
end;

const
  NET_FW_SCOPE_ALL = 0;
  NET_FW_IP_VERSION_ANY = 2;

procedure SetFirewallException(AppName,FileName:string);
var
  FirewallObject: Variant;
  FirewallManager: Variant;
  FirewallProfile: Variant;
begin
  try
    FirewallObject := CreateOleObject('HNetCfg.FwAuthorizedApplication');
    FirewallObject.ProcessImageFileName := FileName;
    FirewallObject.Name := AppName;
    FirewallObject.Scope := NET_FW_SCOPE_ALL;
    FirewallObject.IpVersion := NET_FW_IP_VERSION_ANY;
    FirewallObject.Enabled := True;
    FirewallManager := CreateOleObject('HNetCfg.FwMgr');
    FirewallProfile := FirewallManager.LocalPolicy.CurrentProfile;
    FirewallProfile.AuthorizedApplications.Add(FirewallObject);
  except
  end;
end;

procedure RemoveFirewallException( FileName:string );
var
  FirewallManager: Variant;
  FirewallProfile: Variant;
begin
  try
    FirewallManager := CreateOleObject('HNetCfg.FwMgr');
    FirewallProfile := FirewallManager.LocalPolicy.CurrentProfile;
    FireWallProfile.AuthorizedApplications.Remove(FileName);
  except
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep=ssPostInstall then
     SetFirewallException('NWRSc Python', ExpandConstant('{app}\pythonw.exe'));
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep=usPostUninstall then
     RemoveFirewallException(ExpandConstant('{app}\pythonw.exe'));
end;

end.
