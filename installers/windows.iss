[Setup]
AppName=Scorekeeper
AppVersion=2.0
OutputDir=.
OutputBaseFilename=ScorekeeperSetup-{#SetupSetting("AppVersion")}
AppPublisher=Brett Wilson
DefaultDirName={userdocs}\scorekeeper-{#SetupSetting("AppVersion")}
UsePreviousAppDir=yes
DefaultGroupName=Scorekeeper-{#SetupSetting("AppVersion")}
AllowNoIcons=yes
Compression=lzma
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[InstallDelete]
Type: filesandordirs; Name: "{app}\python";

[UninstallDelete]
Type: filesandordirs; Name: "{app}\python";

[Dirs]
Name: "{app}\database"; Permissions: users-modify; Flags: uninsneveruninstall
Name: "{app}\logs";     Permissions: users-modify; Flags: uninsneveruninstall

[Files]
Source: "stage\siteconfig.json";     DestDir: "{app}";            Flags: uninsneveruninstall
Source: "stage\pgsql\*";             DestDir: "{app}\postgresql"; Flags: ignoreversion recursesubdirs;
Source: "stage\scorekeeperapps.jar"; DestDir: "{app}";
Source: "stage\wheels\*";            DestDir: "{tmp}\wheels";

[Icons]
Name: "{group}\Launcher-{#SetupSetting("AppVersion")}"; WorkingDir: "{app}"; Filename: "{code:getJavaPath}\bin\javaw.exe"; Parameters: "-jar scorekeeperapps.jar TrayMonitor";
Name: "{group}\Uninstall";                              WorkingDir: "{app}"; Filename: "{app}\unins000.exe";                                                      

[Run]
Filename: "{code:getPythonPath}";        Flags: runasoriginaluser; Parameters: "-m venv {app}\python"; AfterInstall: AddRules
Filename: "{app}\python\Scripts\pip";    Flags: runasoriginaluser; Parameters: "install {tmp}\wheels\nwrsc-{#SetupSetting("AppVersion")}-py3-none-any.whl -f file:{tmp}\wheels"; 
Filename: "{app}\python\Scripts\python"; Flags: runasoriginaluser; Parameters: "{app}\python\Scripts\dbensure.py wwwpass";
Filename: "{sys}\sc.exe";                                          Parameters: "stop   w3svc";
Filename: "{sys}\sc.exe";                                          Parameters: "config w3svc start= disabled";

[Code]

const
  NET_FW_SCOPE_ALL = 0;
  NET_FW_IP_VERSION_ANY = 2;
  NET_FW_PROTOCOL_TCP = 6;
  NET_FW_PROTOCOL_UDP = 17;

var
  JavaPath: string;
  PythonPath: string;

function getJavaPath(Param: String): String;
begin Result := JavaPath; end;

function getPythonPath(Param: String): String;
begin Result := PythonPath; end;

function InitializeSetup(): Boolean;
var
 JavaInstalled : Boolean;
 PythonInstalled: Boolean;
 Version: String;
begin
   JavaInstalled := false;
   PythonInstalled := false;
   Result := false;

   if RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment', 'CurrentVersion', Version) then begin
     if (StrToInt(Version[3]) >= 8) then begin
       if RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment\'+Version, 'JavaHome', JavaPath) then begin
         JavaInstalled := true;
       end;
     end;
   end;
 
   if RegQueryStringValue(HKCU, 'SOFTWARE\Python\PythonCore\3.6\InstallPath', 'ExecutablePath', PythonPath) then begin
     PythonInstalled := true;
   end;

   if not JavaInstalled then begin
     MsgBox('This tool requires Java Runtime Environment version 1.8 or newer to run. Please download and install the JRE and run this setup again.', mbInformation, MB_OK);
   end;

   if not PythonInstalled then begin
     MsgBox('This tool requires Python version 3.6 to run. Please download and install run this setup again.', mbInformation, MB_OK);
   end;

   if JavaInstalled and PythonInstalled then begin
     Result := true;
   end;
end;

procedure SetFirewallException(AppName, FileName:string);
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

procedure RemoveFirewallException(FileName:string);
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

procedure SetFirewallPortException(AppName: string; Protocol, Port: integer);
var
  FirewallObject: Variant;
  FirewallManager: Variant;
  FirewallProfile: Variant;
begin
  try
    FirewallObject := CreateOleObject('HNetCfg.FwOpenPort');
    FirewallObject.Name := AppName;
    FirewallObject.Scope := NET_FW_SCOPE_ALL;
    FirewallObject.IpVersion := NET_FW_IP_VERSION_ANY;
    FirewallObject.Protocol := Protocol;
    FirewallObject.Port := Port;
    FirewallObject.Enabled := True;
    FirewallManager := CreateOleObject('HNetCfg.FwMgr');
    FirewallProfile := FirewallManager.LocalPolicy.CurrentProfile;
    FirewallProfile.GloballyOpenPorts.Add(FirewallObject);
  except
  end;
end;    

procedure RemoveFirewallPortException(Protocol, Port: integer);
var
  FirewallManager: Variant;
  FirewallProfile: Variant;
begin
  try
    FirewallManager := CreateOleObject('HNetCfg.FwMgr');
    FirewallProfile := FirewallManager.LocalPolicy.CurrentProfile;
    FireWallProfile.GloballyOpenPorts.Remove(Port, Protocol);
  except
  end;
end;

procedure AddRules();
begin
  SetFirewallException('Scorekeeper Python',     ExpandConstant('{app}')+'\python\Scripts\python.exe');
  SetFirewallException('Scorekeeper Postgresql', ExpandConstant('{app}')+'\postgresql\bin\postgres.exe');
  SetFirewallException('Scorekeeper Java',       JavaPath+'\bin\javaw.exe');
  SetFirewallPortException('Scorekeeper MDNS', NET_FW_PROTOCOL_UDP, 5353);
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep=usPostUninstall then begin
    RemoveFirewallException(ExpandConstant('{app}')+'\python\Scripts\python.exe');
    RemoveFirewallException(ExpandConstant('{app}')+'\postgresql\bin\postgres.exe');
    RemoveFirewallException(JavaPath+'\bin\javaw.exe');
  end;
end;

end.
