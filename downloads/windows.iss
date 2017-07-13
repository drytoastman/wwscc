#define Version 2.0

[Setup]
AppName=Scorekeeper
Versionsion={#Version}
OutputDir=.
OutputBaseFilename=ScorekeeperSetup-{#Version}
AppPublisher=Brett Wilson
DefaultDirName={userdocs}\scorekeeper-{#Version}
UsePreviousAppDir=yes
DefaultGroupName=Scorekeeper-{#Version}
AllowNoIcons=yes
Compression=lzma
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Dirs]
Name: "{app}\logs"; Permissions: users-modify; Flags: uninsneveruninstall

[Files]
Source: "scorekeeperapps-{#Version}.jar"; DestDir: "{app}";
Source: "docker-compose.yaml";            DestDir: "{app}";
Source: "rxtxSerial.dll";                 DestDir: "{code:getJavaPath}\bin";

[Icons]
Name: "{group}\Launcher-{#Version}"; WorkingDir: "{app}"; Filename: "{code:getJavaPath}\bin\javaw.exe"; Parameters: "-jar scorekeeperapps-{#Version}.jar TrayMonitor";
Name: "{group}\Uninstall";           WorkingDir: "{app}"; Filename: "{app}\unins000.exe";

[Run]
Filename: "{sys}\sc.exe"; Parameters: "stop   w3svc";
Filename: "{sys}\sc.exe"; Parameters: "config w3svc start=disabled";

[Code]
const
  NET_FW_PROTOCOL_TCP   = 6;
  NET_FW_PROTOCOL_UDP   = 17;
  NET_FW_PROFILE2_ALL   = 2147483647;
  NET_FW_RULE_DIR_IN    = 1;
  NET_FW_ACTION_ALLOW   = 1;

var
  JavaPath: string;

function getJavaPath(Param: String): String;
begin Result := JavaPath; end;

function InitializeSetup(): Boolean;
var
 Version: String;
 ResultCode: Integer;
 JavaInstalled: Boolean;
begin
   if RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment', 'CurrentVersion', Version) then begin
     if (StrToInt(Version[3]) >= 8) then begin
       if RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment\'+Version, 'JavaHome', JavaPath) then begin
         JavaInstalled := true;
       end;
     end;
   end;
 
   if not JavaInstalled then begin
     MsgBox('Java version 1.8 or newer is required. You can download from http://java.com/download', mbInformation, MB_OK);
     ShellExec('open', 'http://java.com/download', '', '', SW_SHOW, ewNoWait, ResultCode);
     Result := false;
     Exit;
   end;

   if not ExecAsOriginalUser('docker-machine.exe', 'version', '', SW_SHOW, ewWaitUntilTerminated, ResultCode) then begin
     MsgBox('Docker-Toolbox for Windows is required. Please install from https://docs.docker.com/toolbox/toolbox_install_windows', mbInformation, MB_OK);
     ShellExec('open', 'https://docs.docker.com/toolbox/toolbox_install_windows', '', '', SW_SHOW, ewNoWait, ResultCode);
     Result := false;
     Exit;
   end;

   Result := true;
end;

procedure AddFirewallPort(AppName: string; Protocol, Port: integer);
var
  fwPolicy: Variant;
  rule: Variant;
begin
  try
    rule            := CreateOleObject('HNetCfg.FWRule')
    rule.Name       := AppName
    rule.Profiles   := NET_FW_PROFILE2_ALL
    rule.Action     := NET_FW_ACTION_ALLOW
    rule.Direction  := NET_FW_RULE_DIR_IN
    rule.Enabled    := true
    rule.Protocol   := Protocol
    rule.LocalPorts := Port
    fwPolicy        := CreateOleObject('HNetCfg.FwPolicy2');
    fwPolicy.Rules.Add(rule);
  except
  end;
end;    

procedure RemFirewallPort(AppName: string);
var
  fwPolicy: Variant;
begin
  try
    fwPolicy := CreateOleObject('HNetCfg.FwPolicy2');
    fwPolicy.Rules.Remove(AppName);
  except
  end;
end;    

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep=ssDone then begin
    RemFirewallPort('Scorekeeper Web');
    RemFirewallPort('Scorekeeper DB');
    RemFirewallPort('Scorekeeper MDNS');
    AddFirewallPort('Scorekeeper Web',  NET_FW_PROTOCOL_TCP, 80);
    AddFirewallPort('Scorekeeper DB',   NET_FW_PROTOCOL_TCP, 54329);
    AddFirewallPort('Scorekeeper MDNS', NET_FW_PROTOCOL_UDP, 5353);
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep=usPostUninstall then begin
    RemFirewallPort('Scorekeeper Web');
    RemFirewallPort('Scorekeeper DB');
    RemFirewallPort('Scorekeeper MDNS');
  end;
end;

end.
