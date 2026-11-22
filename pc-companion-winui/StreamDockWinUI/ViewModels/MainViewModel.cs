using System;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Threading;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Win32;
using Newtonsoft.Json.Linq;
using StreamDockWinUI.Models;
using StreamDockWinUI.Services;
using Application = System.Windows.Application;
using OpenFileDialog = Microsoft.Win32.OpenFileDialog;

namespace StreamDockWinUI.ViewModels
{
    public partial class MainViewModel : ObservableObject
    {
        [DllImport("user32.dll")]
        private static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

        [DllImport("user32.dll")]
        private static extern bool LockWorkStation();

        private const uint KEYEVENTF_KEYUP = 0x0002;

        private readonly WebSocketService _webSocketService;

        [ObservableProperty]
        private string _selectedClientUuid = "UUID-AAA-111";

        [ObservableProperty]
        private ObservableCollection<ClientDeviceItem> _connectedClients = new();

        [ObservableProperty]
        private ClientDeviceItem? _selectedClient;

        partial void OnSelectedClientChanged(ClientDeviceItem? value)
        {
            if (value != null)
            {
                SelectedClientUuid = value.Uuid;
                StatusMessage = $"Dispositivo Activo: {value.DisplayName} ({value.Uuid})";
                _ = RequestWorkspaceAsync();
            }
        }

        private async Task RequestWorkspaceAsync()
        {
            try
            {
                await _webSocketService.SendMessageAsync(new
                {
                    type = "get_workspace",
                    uuid = SelectedClientUuid
                });
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Error requesting workspace: {ex.Message}");
            }
        }

        [ObservableProperty]
        private ObservableCollection<ButtonModel> _gridButtons = new();

        private List<ButtonModel> _allButtons = new();

        [ObservableProperty]
        private ObservableCollection<ProfileModel> _profiles = new();

        [ObservableProperty]
        private ProfileModel? _selectedProfile;

        [ObservableProperty]
        private string _activeProfileId = string.Empty;

        [ObservableProperty]
        private string _editingProfileName = string.Empty;

        [ObservableProperty]
        private string _editingProfileAutoProcess = string.Empty;

        [ObservableProperty]
        private bool _isAutoProfileSwitchEnabled = true;

        [ObservableProperty]
        private bool _isRecordingHotkey = false;

        [ObservableProperty]
        private string _localIpDisplay = "127.0.0.1:8081";

        private DispatcherTimer? _autoSwitchTimer;
        private string? _lastForegroundProcess;

        private long _workspaceVersion = 1;

        [ObservableProperty]
        private ButtonModel? _selectedButton;

        [ObservableProperty]
        private ButtonModel? _movingButton;

        [ObservableProperty]
        private string _selectedCategoryTab = "AUDIO_PC";

        private static string InferCategoryTab(ButtonModel? btn)
        {
            if (btn == null) return "AUDIO_PC";
            var type = btn.ActionType ?? "NONE";
            var val = btn.ActionValue ?? string.Empty;

            return type switch
            {
                "MEDIA_CONTROL" when val.StartsWith("SPOTIFY_", StringComparison.OrdinalIgnoreCase) || val.StartsWith("VOLUME_SLIDER_SPOTIFY", StringComparison.OrdinalIgnoreCase) => "SPOTIFY",
                "MEDIA_CONTROL" => "AUDIO_PC",
                "LAUNCH_APP" when WindowController.IsWindowAction(val) => "WINDOWS",
                "SYSTEM_COMMAND" when string.Equals(val, "SHOW_DESKTOP", StringComparison.OrdinalIgnoreCase) => "WINDOWS",
                "LAUNCH_APP" or "LAUNCH_SHORTCUT" => "LAUNCH_APP",
                "KEYBOARD_HOTKEY" => "KEYBOARD_HOTKEY",
                "OBS_SCENE_SWITCH" => "OBS_SCENE_SWITCH",
                "NAVIGATE" => "NAVIGATE",
                "SYSTEM_COMMAND" => "SYSTEM_COMMAND",
                "STREAMER_BOT_ACTION" => "STREAMER_BOT_ACTION",
                _ => "AUDIO_PC"
            };
        }

        [ObservableProperty]
        private bool _hasPcVolumeBarInProfile;

        [ObservableProperty]
        private bool _hasSpotifyVolumeBarInProfile;

        public bool HasAnyProfileVolumeBar => HasPcVolumeBarInProfile || HasSpotifyVolumeBarInProfile;

        [ObservableProperty]
        private int _pcVolumeLevel = 80;

        [ObservableProperty]
        private int _spotifyVolumeLevel = 85;

        private bool _isRefreshingVolumeSliders;

        partial void OnPcVolumeLevelChanged(int value)
        {
            if (_isRefreshingVolumeSliders) return;
            var (ok, msg) = SpotifyController.SetMasterVolumePercent(value);
            StatusMessage = ok ? $"🎚️ {msg}" : $"⚠️ {msg}";
        }

        partial void OnSpotifyVolumeLevelChanged(int value)
        {
            if (_isRefreshingVolumeSliders) return;
            var (ok, msg) = SpotifyController.SetSpotifyVolumeScalar(value / 100f);
            StatusMessage = ok ? $"🎚️ {msg}" : $"⚠️ {msg}";
        }

        public void RefreshProfileVolumeBarsState()
        {
            var profileButtons = _allButtons.Where(b => b.ProfileId == ActiveProfileId).ToList();
            bool hasPc = profileButtons.Any(b =>
                b.ActionType == "MEDIA_CONTROL" &&
                string.Equals(b.ActionValue, "VOLUME_SLIDER_PC", StringComparison.OrdinalIgnoreCase));
            bool hasSpotify = profileButtons.Any(b =>
                b.ActionType == "MEDIA_CONTROL" &&
                string.Equals(b.ActionValue, "VOLUME_SLIDER_SPOTIFY", StringComparison.OrdinalIgnoreCase));

            HasPcVolumeBarInProfile = hasPc;
            HasSpotifyVolumeBarInProfile = hasSpotify;
            OnPropertyChanged(nameof(HasAnyProfileVolumeBar));

            _isRefreshingVolumeSliders = true;
            try
            {
                if (hasPc)
                    PcVolumeLevel = SpotifyController.GetMasterVolumePercent();
                if (hasSpotify)
                    SpotifyVolumeLevel = SpotifyController.GetSpotifyVolumePercent();
            }
            finally
            {
                _isRefreshingVolumeSliders = false;
            }
        }

        [RelayCommand]
        public void MutePcFromBar()
        {
            keybd_event(0xAD, 0, 0, UIntPtr.Zero);
            keybd_event(0xAD, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
            StatusMessage = "🔇 Audio Maestro de PC silenciado / reactivado";
        }

        [RelayCommand]
        public void MuteSpotifyFromBar()
        {
            var (ok, msg) = SpotifyController.ExecuteAction("SPOTIFY_MUTE");
            StatusMessage = ok ? $"🔇 {msg}" : $"⚠️ {msg}";
        }

        [RelayCommand]
        public async Task RemovePcVolumeBarFromProfile()
        {
            var target = _allButtons.FirstOrDefault(b =>
                b.ProfileId == ActiveProfileId &&
                string.Equals(b.ActionValue, "VOLUME_SLIDER_PC", StringComparison.OrdinalIgnoreCase));
            if (target != null)
            {
                SelectButton(target);
                await DeleteButtonConfiguration();
            }
        }

        [RelayCommand]
        public async Task RemoveSpotifyVolumeBarFromProfile()
        {
            var target = _allButtons.FirstOrDefault(b =>
                b.ProfileId == ActiveProfileId &&
                string.Equals(b.ActionValue, "VOLUME_SLIDER_SPOTIFY", StringComparison.OrdinalIgnoreCase));
            if (target != null)
            {
                SelectButton(target);
                await DeleteButtonConfiguration();
            }
        }

        [RelayCommand]
        public async Task TogglePcVolumeBarInProfile()
        {
            if (HasPcVolumeBarInProfile)
            {
                await RemovePcVolumeBarFromProfile();
                return;
            }

            var target = _allButtons
                .Where(b => b.ProfileId == ActiveProfileId)
                .OrderBy(b => b.Position)
                .FirstOrDefault(b => !b.IsConfigured) ?? SelectedButton;

            if (target != null)
            {
                SelectButton(target);
                SelectActionPreset("VOLUME_SLIDER_PC");
            }
        }

        [RelayCommand]
        public async Task ToggleSpotifyVolumeBarInProfile()
        {
            if (HasSpotifyVolumeBarInProfile)
            {
                await RemoveSpotifyVolumeBarFromProfile();
                return;
            }

            var target = _allButtons
                .Where(b => b.ProfileId == ActiveProfileId)
                .OrderBy(b => b.Position)
                .FirstOrDefault(b => !b.IsConfigured) ?? SelectedButton;

            if (target != null)
            {
                SelectButton(target);
                SelectActionPreset("VOLUME_SLIDER_SPOTIFY");
            }
        }

        partial void OnSelectedProfileChanged(ProfileModel? value)
        {
            if (value != null)
            {
                ActiveProfileId = value.Id;
                EditingProfileName = value.Name;
                EditingProfileAutoProcess = value.AutoSwitchProcess ?? string.Empty;
                UpdateVisibleButtons();
            }
            else
            {
                EditingProfileName = string.Empty;
                EditingProfileAutoProcess = string.Empty;
            }
        }


        private void UpdateVisibleButtons()
        {
            Application.Current?.Dispatcher?.Invoke(() =>
            {
                GridButtons.Clear();
                var filtered = _allButtons
                    .Where(b => b.ProfileId == ActiveProfileId)
                    .OrderBy(b => b.Position);

                foreach (var btn in filtered)
                {
                    GridButtons.Add(btn);
                }

                RefreshProfileVolumeBarsState();

                if (GridButtons.Any())
                    SelectButton(GridButtons.First());
            });
        }

        [ObservableProperty]
        private string _statusMessage = "Listo";

        [ObservableProperty]
        private string _obsHost = "127.0.0.1";

        [ObservableProperty]
        private string _obsPort = "4455";

        [ObservableProperty]
        private string _obsPassword = string.Empty;

        [ObservableProperty]
        private string _obsScanStatus = "Presiona 'Escanear Escenas y Fuentes OBS' para sincronizar con OBS Studio";

        [ObservableProperty]
        private bool _isObsScanning = false;

        [ObservableProperty]
        private ObservableCollection<ObsPresetItem> _obsScenes = new();

        [ObservableProperty]
        private ObservableCollection<ObsPresetItem> _obsSources = new();

        public bool HasObsScenes => ObsScenes.Count > 0;
        public bool HasObsSources => ObsSources.Count > 0;

        [ObservableProperty]
        private bool _isSpotifyInstalled = SpotifyController.IsSpotifyInstalled();

        private bool _isLoadingObsSettings = false;

        partial void OnObsHostChanged(string value) => SaveAndSyncObsConfig();
        partial void OnObsPortChanged(string value) => SaveAndSyncObsConfig();
        partial void OnObsPasswordChanged(string value) => SaveAndSyncObsConfig();

        private void LoadObsSettings()
        {
            _isLoadingObsSettings = true;
            try
            {
                var saved = ObsSettingsStorage.Load();
                ObsHost = saved.Host;
                ObsPort = saved.Port.ToString();
                ObsPassword = saved.Password;
            }
            finally
            {
                _isLoadingObsSettings = false;
            }
        }

        private void SaveAndSyncObsConfig()
        {
            int portNumber = int.TryParse(ObsPort, out var p) && p > 0 ? p : 4455;
            if (!_isLoadingObsSettings)
            {
                ObsSettingsStorage.Save(new ObsConnectionSettings
                {
                    Host = string.IsNullOrWhiteSpace(ObsHost) ? "127.0.0.1" : ObsHost.Trim(),
                    Port = portNumber,
                    Password = ObsPassword ?? string.Empty
                });
            }

            _ = _webSocketService.SendMessageAsync(new
            {
                type = "obs_config",
                uuid = SelectedClientUuid,
                payload = new
                {
                    host = ObsHost,
                    port = portNumber,
                    password = ObsPassword
                }
            });
        }

        public MainViewModel()
        {
            _webSocketService = new WebSocketService();
            DetectLocalIpAddress();
            LoadObsSettings();
            InitializeClients();
            SubscribeWebSocketEvents();
            SaveAndSyncObsConfig();
            StartAutoProfileMonitor();
        }

        public MainViewModel(WebSocketService webSocketService)
        {
            _webSocketService = webSocketService;
            DetectLocalIpAddress();
            LoadObsSettings();
            InitializeClients();
            SubscribeWebSocketEvents();
            SaveAndSyncObsConfig();
            StartAutoProfileMonitor();
        }

        public void DetectLocalIpAddress()
        {
            try
            {
                string? bestIp = null;
                foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
                {
                    if (ni.OperationalStatus != OperationalStatus.Up) continue;
                    if (ni.NetworkInterfaceType == NetworkInterfaceType.Loopback) continue;
                    var props = ni.GetIPProperties();
                    if (props.GatewayAddresses.Count == 0) continue;

                    foreach (var ua in props.UnicastAddresses)
                    {
                        if (ua.Address.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(ua.Address))
                        {
                            bestIp = ua.Address.ToString();
                            break;
                        }
                    }
                    if (bestIp != null) break;
                }

                if (bestIp == null)
                {
                    bestIp = Dns.GetHostEntry(Dns.GetHostName())
                        .AddressList
                        .FirstOrDefault(ip => ip.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(ip))
                        ?.ToString() ?? "127.0.0.1";
                }

                LocalIpDisplay = $"{bestIp}:8081";
            }
            catch
            {
                LocalIpDisplay = "127.0.0.1:8081";
            }
        }

        private void StartAutoProfileMonitor()
        {
            _autoSwitchTimer = new DispatcherTimer
            {
                Interval = TimeSpan.FromMilliseconds(1500)
            };
            _autoSwitchTimer.Tick += async (_, _) => await CheckForegroundAppForAutoProfileAsync();
            _autoSwitchTimer.Start();
        }

        private async Task CheckForegroundAppForAutoProfileAsync()
        {
            if (!IsAutoProfileSwitchEnabled || Profiles.Count <= 1) return;

            var procName = WindowController.GetForegroundProcessName();
            if (string.IsNullOrWhiteSpace(procName) ||
                string.Equals(procName, _lastForegroundProcess, StringComparison.OrdinalIgnoreCase))
            {
                return;
            }

            _lastForegroundProcess = procName;

            var matched = Profiles.FirstOrDefault(p =>
                !string.IsNullOrWhiteSpace(p.AutoSwitchProcess) &&
                (string.Equals(Path.GetFileNameWithoutExtension(p.AutoSwitchProcess.Trim()), procName, StringComparison.OrdinalIgnoreCase) ||
                 procName.Contains(Path.GetFileNameWithoutExtension(p.AutoSwitchProcess.Trim()), StringComparison.OrdinalIgnoreCase)));

            if (matched != null && SelectedProfile?.Id != matched.Id)
            {
                SelectedProfile = matched;
                StatusMessage = $"🔄 Auto-Perfil activado por '{procName}.exe' → '{matched.Name}'";
                await SyncFullWorkspace();
            }
        }

        [RelayCommand]
        public void CopyLocalIp()
        {
            try
            {
                System.Windows.Clipboard.SetText(LocalIpDisplay);
                StatusMessage = $"📋 Dirección IP copiada al portapapeles: {LocalIpDisplay} (introdúcela en la pestaña Config de la App)";
            }
            catch
            {
            }
        }

        [RelayCommand]
        public void ToggleHotkeyRecording()
        {
            IsRecordingHotkey = !IsRecordingHotkey;
            StatusMessage = IsRecordingHotkey
                ? "🔴 Grabando atajo: presiona cualquier combinación de teclas en tu teclado..."
                : "Grabación de atajo cancelada";
        }

        public void ApplyRecordedHotkey(string hotkeyString)
        {
            IsRecordingHotkey = false;
            if (SelectedButton == null || string.IsNullOrWhiteSpace(hotkeyString)) return;

            SelectedButton.ActionType = "KEYBOARD_HOTKEY";
            SelectedButton.ActionValue = hotkeyString;
            if (string.IsNullOrWhiteSpace(SelectedButton.Label) || SelectedButton.Label.StartsWith("Botón"))
            {
                SelectedButton.Label = hotkeyString;
            }
            if (string.IsNullOrWhiteSpace(SelectedButton.IconName) || SelectedButton.IconName == "none")
            {
                SelectedButton.IconName = "keyboard";
            }
            StatusMessage = $"⌨️ Atajo capturado automáticamente: {hotkeyString}";
        }

        [RelayCommand]
        public void BrowseProfileProcess()
        {
            if (SelectedProfile == null) return;
            var dialog = new OpenFileDialog
            {
                Title = "Seleccionar Programa o Juego para Auto-Cambiar a este Perfil",
                Filter = "Ejecutables (*.exe)|*.exe|Todos los Archivos (*.*)|*.*"
            };
            if (dialog.ShowDialog() == true)
            {
                var exeName = Path.GetFileName(dialog.FileName);
                EditingProfileAutoProcess = exeName;
                SelectedProfile.AutoSwitchProcess = exeName;
                _ = SaveProfileName();
            }
        }

        public async Task SwapButtonsDirectAsync(ButtonModel source, ButtonModel target)
        {
            if (source == null || target == null || source == target) return;
            if (source.ProfileId != target.ProfileId) return;

            int fromPos = source.Position;
            int toPos = target.Position;

            source.Position = toPos;
            source.Id = $"btn-{source.ProfileId}-{toPos}";

            target.Position = fromPos;
            target.Id = $"btn-{target.ProfileId}-{fromPos}";

            UpdateVisibleButtons();
            SelectButton(source);
            await SyncFullWorkspace();
            StatusMessage = $"✓ Botón #{fromPos + 1} intercambiado con #{toPos + 1}";
        }

        private void InitializeClients()
        {
            var defaultClient = new ClientDeviceItem("UUID-AAA-111", "Cliente Principal (Android)", "Online");
            ConnectedClients.Add(defaultClient);
            SelectedClient = defaultClient;
        }

        private void SubscribeWebSocketEvents()
        {
            _webSocketService.OnMessageReceived += HandleWebSocketMessage;
            _ = RequestClientListAsync();
        }

        [RelayCommand]
        public async Task CreateProfile()
        {
            var newId = $"prof-{Guid.NewGuid()}";
            var defaultName = $"Nueva Página {Profiles.Count + 1}";
            var newProfile = new ProfileModel(newId, defaultName)
            {
                IsDefault = Profiles.Count == 0,
                IconName = "apps",
                BackgroundColor = "#1E1E2E"
            };

            Application.Current?.Dispatcher?.Invoke(() =>
            {
                // Crear 20 botones vacíos para este nuevo perfil
                for (int i = 0; i < 20; i++)
                {
                    var btn = new ButtonModel(i, "")
                    {
                        Id = $"btn-{newId}-{i}",
                        ProfileId = newId,
                        ActionType = "NONE",
                        ActionValue = "",
                        BackgroundColor = "#1E1E2E",
                        IconName = "none"
                    };
                    _allButtons.Add(btn);
                }

                Profiles.Add(newProfile);
                SelectedProfile = newProfile;
                EditingProfileName = newProfile.Name;
            });

            StatusMessage = $"✓ Perfil '{defaultName}' creado. Puedes editar su nombre en el panel izquierdo.";
            // Sincronizar el workspace completo para incluir el nuevo perfil y sus botones
            await SyncFullWorkspace();
        }

        [RelayCommand]
        public async Task SaveProfileName()
        {
            if (SelectedProfile == null) return;

            var trimmed = EditingProfileName?.Trim() ?? string.Empty;
            if (string.IsNullOrWhiteSpace(trimmed))
            {
                StatusMessage = "⚠️ El nombre del perfil no puede estar vacío";
                EditingProfileName = SelectedProfile.Name;
                return;
            }

            var autoProc = string.IsNullOrWhiteSpace(EditingProfileAutoProcess)
                ? null
                : EditingProfileAutoProcess.Trim();

            SelectedProfile.Name = trimmed;
            SelectedProfile.AutoSwitchProcess = autoProc;
            EditingProfileName = trimmed;
            EditingProfileAutoProcess = autoProc ?? string.Empty;
            StatusMessage = autoProc != null
                ? $"✓ Perfil '{trimmed}' vinculado automáticamente a '{autoProc}'"
                : $"✓ Nombre del perfil actualizado a '{trimmed}'";
            await SyncFullWorkspace();
        }

        [RelayCommand]
        public async Task DeleteProfile(ProfileModel? profileParameter = null)
        {
            var target = profileParameter ?? SelectedProfile;
            if (target == null) return;

            var deletedId = target.Id;
            var deletedName = target.Name;

            Application.Current?.Dispatcher?.Invoke(() =>
            {
                // Eliminar únicamente los botones que pertenecen a este perfil sin tocar los demás perfiles
                _allButtons.RemoveAll(b => b.ProfileId == deletedId);

                int removedIndex = Profiles.IndexOf(target);
                Profiles.Remove(target);

                // Si se borró el último perfil existente, crear uno limpio por defecto
                if (Profiles.Count == 0)
                {
                    var fallbackId = $"prof-{Guid.NewGuid()}";
                    var fallbackProfile = new ProfileModel(fallbackId, "Principal")
                    {
                        IsDefault = true,
                        IconName = "home",
                        BackgroundColor = "#1E1E2E"
                    };

                    for (int i = 0; i < 20; i++)
                    {
                        _allButtons.Add(new ButtonModel(i, "")
                        {
                            Id = $"btn-{fallbackId}-{i}",
                            ProfileId = fallbackId,
                            ActionType = "NONE",
                            ActionValue = "",
                            BackgroundColor = "#1E1E2E",
                            IconName = "none"
                        });
                    }

                    Profiles.Add(fallbackProfile);
                    SelectedProfile = fallbackProfile;
                }
                else
                {
                    if (!Profiles.Any(p => p.IsDefault))
                    {
                        Profiles[0].IsDefault = true;
                    }

                    if (SelectedProfile == null || SelectedProfile.Id == deletedId)
                    {
                        int nextIndex = Math.Clamp(removedIndex, 0, Profiles.Count - 1);
                        SelectedProfile = Profiles[nextIndex];
                    }
                }

                UpdateVisibleButtons();
            });

            StatusMessage = $"🗑️ Perfil '{deletedName}' y todos sus botones fueron eliminados";
            await SyncFullWorkspace();
        }

        private async Task SyncFullWorkspace()
        {
            _workspaceVersion++;
            var syncPayload = new
            {
                type = "db_sync",
                uuid = SelectedClientUuid,
                timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                payload = new
                {
                    workspace = new
                    {
                        client_uuid = SelectedClientUuid,
                        version = _workspaceVersion,
                        active_profile_id = ActiveProfileId,
                        grid_rows = 5,
                        grid_cols = 4,
                        profiles = Profiles.Select(p => new
                        {
                            id = p.Id,
                            name = p.Name,
                            auto_switch_process = p.AutoSwitchProcess,
                            is_default = p.IsDefault,
                            icon_name = p.IconName,
                            background_color = p.BackgroundColor
                        }),
                        buttons = _allButtons.Select(b => new
                        {
                            id = b.Id,
                            profile_id = b.ProfileId,
                            position = b.Position,
                            label = b.Label,
                            icon_name = b.IconName,
                            action_type = b.ActionType,
                            action_value = b.ActionValue,
                            background_color = b.BackgroundColor
                        })
                    }
                }
            };

            await _webSocketService.SendMessageAsync(syncPayload);
        }

        private async Task RequestClientListAsync()
        {
            try
            {
                await _webSocketService.SendMessageAsync(new { type = "get_client_list" });
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Error requesting client list: {ex.Message}");
            }
        }

        private void HandleWebSocketMessage(string json)
        {
            try
            {
                var env = JObject.Parse(json);
                var type = env["type"]?.ToString();

                if (type == "client_list")
                {
                    var payload = env["payload"] as JArray;
                    if (payload != null)
                    {
                        Application.Current?.Dispatcher?.Invoke(() =>
                        {
                            foreach (var item in payload)
                            {
                                var uuid = item["uuid"]?.ToString();
                                var name = item["display_name"]?.ToString() ?? "Android Client";
                                var status = item["status"]?.ToString() ?? "Online";

                                if (!string.IsNullOrEmpty(uuid))
                                {
                                    var existing = ConnectedClients.FirstOrDefault(c => c.Uuid == uuid);
                                    if (existing != null)
                                    {
                                        existing.DisplayName = name;
                                        existing.Status = status;
                                    }
                                    else
                                    {
                                        var newClient = new ClientDeviceItem(uuid, name, status);
                                        ConnectedClients.Add(newClient);
                                    }
                                }
                            }

                            if (SelectedClient == null || !ConnectedClients.Contains(SelectedClient))
                            {
                                SelectedClient = ConnectedClients.FirstOrDefault();
                            }
                        });
                    }
                }
                else if (type == "db_sync" || type == "register_ack")
                {
                    var msgUuid = env["uuid"]?.ToString();
                    if (msgUuid != SelectedClientUuid) return;

                    var payload = env["payload"] as JObject;
                    if (payload != null)
                    {
                        Application.Current?.Dispatcher?.Invoke(() =>
                        {
                                // Caso 1: Actualización de Workspace completo
                            if (payload.ContainsKey("workspace") || type == "register_ack")
                            {
                                var ws = payload["workspace"] ?? payload;
                                var incomingVersion = ws["version"]?.Value<long?>();
                                if (incomingVersion.HasValue && incomingVersion.Value > _workspaceVersion)
                                {
                                    _workspaceVersion = incomingVersion.Value;
                                }

                                // Cargar Perfiles
                                var profilesJson = ws["profiles"] as JArray;
                                if (profilesJson != null)
                                {
                                    Profiles.Clear();
                                    foreach (var pJson in profilesJson)
                                    {
                                        var p = new ProfileModel(pJson["id"]?.ToString() ?? "", pJson["name"]?.ToString() ?? "")
                                        {
                                            AutoSwitchProcess = pJson["auto_switch_process"]?.ToString(),
                                            IsDefault = pJson["is_default"]?.Value<bool>() ?? false,
                                            IconName = pJson["icon_name"]?.ToString() ?? "home",
                                            BackgroundColor = pJson["background_color"]?.ToString() ?? "#1E1E2E"
                                        };
                                        Profiles.Add(p);
                                    }
                                }

                                // Determinar Perfil Activo
                                ActiveProfileId = ws["active_profile_id"]?.ToString() ?? string.Empty;
                                SelectedProfile = Profiles.FirstOrDefault(p => p.Id == ActiveProfileId) ?? Profiles.FirstOrDefault();
                                EditingProfileName = SelectedProfile?.Name ?? string.Empty;

                                // Cargar Todos los Botones (garantizando las 20 celdas por perfil)
                                var buttonsJson = ws["buttons"] as JArray;
                                _allButtons.Clear();
                                var incomingButtons = new List<ButtonModel>();
                                bool anyIconUpgraded = false;
                                if (buttonsJson != null)
                                {
                                    foreach (var btnJson in buttonsJson)
                                    {
                                        var previousIcon = btnJson["icon_name"]?.ToString() ?? "none";
                                        var btn = new ButtonModel(btnJson["position"]?.Value<int>() ?? 0, btnJson["label"]?.ToString() ?? "")
                                        {
                                            Id = btnJson["id"]?.ToString() ?? "",
                                            ProfileId = btnJson["profile_id"]?.ToString() ?? "",
                                            ActionType = btnJson["action_type"]?.ToString() ?? "NONE",
                                            ActionValue = btnJson["action_value"]?.ToString() ?? "",
                                            BackgroundColor = btnJson["background_color"]?.ToString() ?? "#1E1E2E",
                                            IconName = previousIcon
                                        };
                                        btn.TryExtractIcon(btn.ActionValue);
                                        if (btn.IconName != previousIcon)
                                        {
                                            anyIconUpgraded = true;
                                        }
                                        incomingButtons.Add(btn);
                                    }
                                }

                                foreach (var prof in Profiles)
                                {
                                    for (int pos = 0; pos < 20; pos++)
                                    {
                                        var match = incomingButtons.FirstOrDefault(b => b.ProfileId == prof.Id && b.Position == pos);
                                        if (match != null)
                                        {
                                            _allButtons.Add(match);
                                        }
                                        else
                                        {
                                            _allButtons.Add(new ButtonModel(pos, "")
                                            {
                                                Id = $"btn-{prof.Id}-{pos}",
                                                ProfileId = prof.Id,
                                                ActionType = "NONE",
                                                ActionValue = "",
                                                BackgroundColor = "#1E1E2E",
                                                IconName = "none"
                                            });
                                        }
                                    }
                                }
                                UpdateVisibleButtons();
                                if (anyIconUpgraded)
                                {
                                    _ = SyncFullWorkspace();
                                }
                            }
                            // Caso 2: Actualización o eliminación de un solo botón
                            else if (payload.ContainsKey("button"))
                            {
                                var btnJson = payload["button"];
                                if (btnJson != null)
                                {
                                    var action = payload["action"]?.ToString();
                                    var pos = btnJson["position"]?.Value<int>() ?? -1;
                                    var profId = btnJson["profile_id"]?.ToString();

                                    var existing = _allButtons.FirstOrDefault(b => b.Position == pos && b.ProfileId == profId);
                                    if (existing != null)
                                    {
                                        existing.Label = btnJson["label"]?.ToString() ?? "";
                                        existing.ActionType = btnJson["action_type"]?.ToString() ?? "NONE";
                                        existing.ActionValue = btnJson["action_value"]?.ToString() ?? "";
                                        existing.BackgroundColor = btnJson["background_color"]?.ToString() ?? "#1E1E2E";
                                        existing.IconName = btnJson["icon_name"]?.ToString() ?? "none";

                                        if (action == "delete_button" || (existing.ActionType == "NONE" && existing.IconName == "none"))
                                        {
                                            existing.IconImage = null;
                                        }
                                        else
                                        {
                                            existing.TryExtractIcon(existing.ActionValue);
                                        }
                                    }

                                    if (profId == ActiveProfileId)
                                    {
                                        UpdateVisibleButtons();
                                    }
                                }
                            }
                        });
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Error parsing WebSocket message in ViewModel: {ex.Message}");
            }
        }

        private void InitializeGrid()
        {
            GridButtons.Clear();
            for (int i = 0; i < 20; i++)
            {
                var label = i switch
                {
                    0 => "Bloc de Notas",
                    1 => "Línea de Comandos",
                    2 => "Explorador",
                    3 => "Calculadora",
                    _ => $"Botón {i + 1}"
                };

                var actionType = i switch
                {
                    0 => "LAUNCH_APP",
                    1 => "LAUNCH_APP",
                    2 => "LAUNCH_APP",
                    3 => "LAUNCH_APP",
                    _ => "KEYBOARD_HOTKEY"
                };

                var actionVal = i switch
                {
                    0 => @"C:\Windows\System32\notepad.exe",
                    1 => @"C:\Windows\System32\cmd.exe",
                    2 => @"C:\Windows\explorer.exe",
                    3 => @"C:\Windows\System32\calc.exe",
                    _ => "ALT+F1"
                };

                var bgColor = i switch
                {
                    0 => "#2A2A3C",
                    1 => "#2A2A3C",
                    2 => "#2A2A3C",
                    3 => "#2A2A3C",
                    _ => "#1E1E2E"
                };

                var btn = new ButtonModel(i, label)
                {
                    ActionType = actionType,
                    ActionValue = actionVal,
                    BackgroundColor = bgColor
                };

                btn.TryExtractIcon(actionVal);
                GridButtons.Add(btn);
            }

            SelectButton(GridButtons.First());
        }

        public void StartMoveButton(ButtonModel? button)
        {
            if (button == null) return;

            foreach (var b in _allButtons)
            {
                b.IsMoving = false;
            }

            MovingButton = button;
            button.IsMoving = true;
            StatusMessage = $"🔄 Modo Mover activado para Botón #{button.Position + 1}: haz clic en otro botón para cambiarlo de posición";
        }

        [RelayCommand]
        public void SelectButton(ButtonModel? button)
        {
            if (button == null) return;

            if (MovingButton != null)
            {
                var source = MovingButton;
                source.IsMoving = false;
                MovingButton = null;

                if (source != button && source.ProfileId == button.ProfileId)
                {
                    int fromPos = source.Position;
                    int toPos = button.Position;

                    source.Position = toPos;
                    source.Id = $"btn-{source.ProfileId}-{toPos}";

                    button.Position = fromPos;
                    button.Id = $"btn-{button.ProfileId}-{fromPos}";

                    UpdateVisibleButtons();
                    _ = SyncFullWorkspace();
                    StatusMessage = $"✓ Botón movido de #{fromPos + 1} a #{toPos + 1}";
                    return;
                }
            }

            foreach (var b in GridButtons)
            {
                b.IsSelected = false;
            }

            button.IsSelected = true;
            SelectedButton = button;
            SelectedCategoryTab = InferCategoryTab(button);
            StatusMessage = $"Configurando Botón #{button.Position + 1}";
        }

        [RelayCommand]
        public void SelectActionType(string? categoryTab)
        {
            if (string.IsNullOrWhiteSpace(categoryTab)) return;

            SelectedCategoryTab = categoryTab;

            if (categoryTab == "SPOTIFY")
            {
                IsSpotifyInstalled = SpotifyController.IsSpotifyInstalled();
            }
            else if (categoryTab == "OBS_SCENE_SWITCH" && ObsScenes.Count == 0 && !IsObsScanning)
            {
                _ = ScanObsWebSocketAsync();
            }

            // Si el botón aún está vacío (NONE), asignamos el ActionType base sin sobrescribir etiquetas existentes
            if (SelectedButton != null && (SelectedButton.ActionType == "NONE" || string.IsNullOrWhiteSpace(SelectedButton.ActionType)))
            {
                SelectedButton.ActionType = categoryTab switch
                {
                    "AUDIO_PC" or "SPOTIFY" => "MEDIA_CONTROL",
                    "WINDOWS" => "LAUNCH_APP",
                    _ => categoryTab
                };
            }

            StatusMessage = $"Categoría abierta: {categoryTab}";
        }

        [RelayCommand]
        public void SelectColorPreset(string? hexColor)
        {
            if (SelectedButton == null || string.IsNullOrWhiteSpace(hexColor)) return;
            SelectedButton.BackgroundColor = hexColor;
            StatusMessage = $"🎨 Color del botón cambiado a {hexColor}";
        }

        [RelayCommand]
        public void SelectNavigateProfile(ProfileModel? targetProfile)
        {
            if (SelectedButton == null || targetProfile == null) return;
            SelectedButton.ActionType = "NAVIGATE";
            SelectedButton.ActionValue = targetProfile.Id;
            SelectedButton.Label = targetProfile.Name;
            SelectedButton.IconName = "folder";
            SelectedButton.IconImage = null;
            SelectedButton.BackgroundColor = "#4F46E5";
            StatusMessage = $"📄 Botón configurado para ir a la página '{targetProfile.Name}'";
        }

        [RelayCommand]
        public async Task ScanObsWebSocketAsync()
        {
            if (IsObsScanning) return;
            IsObsScanning = true;
            int portNumber = int.TryParse(ObsPort, out var p) ? p : 4455;
            ObsScanStatus = $"Escaneando escenas y fuentes de OBS en ws://{ObsHost}:{portNumber}...";

            try
            {
                // Guardar en disco y sincronizar configuración de OBS con el servidor Rust
                SaveAndSyncObsConfig();

                var scanResult = await ObsWebSocketClient.ScanAsync(ObsHost, portNumber, ObsPassword);

                Application.Current?.Dispatcher?.Invoke(() =>
                {
                    ObsScenes.Clear();
                    ObsSources.Clear();

                    if (scanResult.Success)
                    {
                        foreach (var scene in scanResult.Scenes)
                        {
                            ObsScenes.Add(scene);
                        }
                        foreach (var source in scanResult.Sources)
                        {
                            ObsSources.Add(source);
                        }

                        OnPropertyChanged(nameof(HasObsScenes));
                        OnPropertyChanged(nameof(HasObsSources));

                        ObsScanStatus = scanResult.Message;
                        StatusMessage = scanResult.Message;

                        if (SelectedButton != null &&
                            SelectedButton.ActionType == "OBS_SCENE_SWITCH" &&
                            ObsScenes.Count > 0 &&
                            (string.IsNullOrWhiteSpace(SelectedButton.ActionValue) || SelectedButton.ActionValue == "Escena Principal"))
                        {
                            SelectObsItem(ObsScenes.First());
                        }
                    }
                    else
                    {
                        OnPropertyChanged(nameof(HasObsScenes));
                        OnPropertyChanged(nameof(HasObsSources));
                        ObsScanStatus = scanResult.Message;
                        StatusMessage = scanResult.Message;
                    }
                });
            }
            catch (Exception ex)
            {
                ObsScanStatus = $"Error al escanear OBS: {ex.Message}";
                StatusMessage = ObsScanStatus;
            }
            finally
            {
                IsObsScanning = false;
            }
        }

        [RelayCommand]
        public void SelectObsItem(ObsPresetItem? item)
        {
            if (SelectedButton == null || item == null) return;

            SelectedButton.ActionType = "OBS_SCENE_SWITCH";
            SelectedButton.ActionValue = item.ActionValue;
            SelectedButton.Label = item.ButtonLabel;
            SelectedButton.IconName = item.ButtonIconName;
            SelectedButton.BackgroundColor = item.ButtonBgColor;

            StatusMessage = $"✓ OBS seleccionado: {item.Title} ({item.Subtitle})";
        }

        [RelayCommand]
        public void SelectActionPreset(string? presetValue)
        {
            if (SelectedButton == null || string.IsNullOrWhiteSpace(presetValue)) return;

            // Determinar automáticamente el ActionType del protocolo según el preset elegido
            if (presetValue is "PLAY_PAUSE" or "PREV_TRACK" or "NEXT_TRACK" or "VOLUME_UP" or "VOLUME_DOWN" or "VOLUME_MUTE" or "VOLUME_SLIDER_PC" or "VOLUME_SLIDER_SPOTIFY" ||
                presetValue.StartsWith("SPOTIFY_", StringComparison.OrdinalIgnoreCase))
            {
                SelectedButton.ActionType = "MEDIA_CONTROL";
                SelectedButton.IconImage = null;
            }
            else if (presetValue is "WIN_MINIMIZE" or "WIN_MAXIMIZE" or "WIN_CLOSE")
            {
                SelectedButton.ActionType = "LAUNCH_APP";
                SelectedButton.IconImage = null;
            }
            else if (presetValue is "LOCK_WORKSTATION" or "SLEEP" or "SHUTDOWN" or "REBOOT" or "SHOW_DESKTOP")
            {
                SelectedButton.ActionType = "SYSTEM_COMMAND";
                SelectedButton.IconImage = null;
            }
            else if (presetValue is "Escena Principal" or "START_RECORD" or "START_STREAM" or "TOGGLE_MUTE")
            {
                SelectedButton.ActionType = "OBS_SCENE_SWITCH";
                SelectedButton.IconImage = null;
            }
            else if (SelectedCategoryTab == "KEYBOARD_HOTKEY" ||
                     presetValue.Contains('+') ||
                     presetValue is "CTRL+SHIFT+M" or "WIN+SHIFT+S" or "ALT+F4" or "CTRL+C" or "CTRL+V" or "ALT+TAB")
            {
                SelectedButton.ActionType = "KEYBOARD_HOTKEY";
                SelectedButton.IconImage = null;
            }

            SelectedButton.ActionValue = presetValue;

            if (SelectedButton.ActionType == "MEDIA_CONTROL")
            {
                SelectedButton.Label = presetValue switch
                {
                    "PLAY_PAUSE" => "Play / Pausa",
                    "PREV_TRACK" => "Anterior",
                    "NEXT_TRACK" => "Siguiente",
                    "VOLUME_UP" => "Subir Vol.",
                    "VOLUME_DOWN" => "Bajar Vol.",
                    "VOLUME_MUTE" => "Silenciar Audio",
                    "VOLUME_SLIDER_PC" => "Barra Vol. PC",
                    "VOLUME_SLIDER_SPOTIFY" => "Barra Spotify",
                    "SPOTIFY_PLAY_PAUSE" => "Spotify Play",
                    "SPOTIFY_NEXT" => "Spotify Sig.",
                    "SPOTIFY_PREV" => "Spotify Ant.",
                    "SPOTIFY_VOL_UP" => "Spotify Vol+",
                    "SPOTIFY_VOL_DOWN" => "Spotify Vol-",
                    "SPOTIFY_MUTE" => "Spotify Mute",
                    "SPOTIFY_OPEN" => "Spotify",
                    _ => presetValue
                };
                SelectedButton.IconName = presetValue switch
                {
                    "PLAY_PAUSE" or "SPOTIFY_PLAY_PAUSE" => "play",
                    "PREV_TRACK" or "SPOTIFY_PREV" => "prev",
                    "NEXT_TRACK" or "SPOTIFY_NEXT" => "next",
                    "VOLUME_UP" or "SPOTIFY_VOL_UP" or "VOLUME_SLIDER_PC" or "VOLUME_SLIDER_SPOTIFY" => "vol_up",
                    "VOLUME_DOWN" or "SPOTIFY_VOL_DOWN" => "vol_down",
                    "VOLUME_MUTE" or "SPOTIFY_MUTE" => "mute",
                    "SPOTIFY_OPEN" => "play",
                    _ => "play"
                };
                SelectedButton.BackgroundColor = presetValue switch
                {
                    "VOLUME_MUTE" or "SPOTIFY_MUTE" => "#E74C3C",
                    "VOLUME_UP" => "#2ECC71",
                    "VOLUME_DOWN" => "#E67E22",
                    "VOLUME_SLIDER_PC" => "#7C3AED",
                    "VOLUME_SLIDER_SPOTIFY" => "#1DB954",
                    var s when s.StartsWith("SPOTIFY_") => "#1DB954",
                    _ => "#9B59B6"
                };

                if (presetValue == "SPOTIFY_OPEN")
                {
                    var spotifyExe = SpotifyController.FindSpotifyExecutablePath();
                    if (!string.IsNullOrWhiteSpace(spotifyExe))
                    {
                        SelectedButton.TryExtractIcon(spotifyExe);
                    }
                }
            }
            else if (SelectedButton.ActionType == "SYSTEM_COMMAND")
            {
                SelectedButton.Label = presetValue switch
                {
                    "LOCK_WORKSTATION" => "Bloquear PC",
                    "SLEEP" => "Suspender PC",
                    "SHUTDOWN" => "Apagar PC",
                    "REBOOT" => "Reiniciar PC",
                    "SHOW_DESKTOP" => "Escritorio",
                    _ => presetValue
                };
                SelectedButton.IconName = presetValue switch
                {
                    "LOCK_WORKSTATION" => "lock",
                    "SHOW_DESKTOP" => "computer",
                    _ => "power"
                };
                SelectedButton.BackgroundColor = presetValue == "SHOW_DESKTOP" ? "#3498DB" : "#E74C3C";
            }
            else if (SelectedButton.ActionType == "KEYBOARD_HOTKEY")
            {
                SelectedButton.Label = presetValue switch
                {
                    "CTRL+SHIFT+M" => "Mute Mic",
                    "WIN+SHIFT+S" => "Captura",
                    "ALT+F4" => "Cerrar App",
                    "CTRL+C" => "Copiar",
                    "CTRL+V" => "Pegar",
                    "ALT+TAB" => "Cambiar App",
                    _ => presetValue
                };
                SelectedButton.IconName = presetValue switch
                {
                    "CTRL+SHIFT+M" => "mic_off",
                    "WIN+SHIFT+S" => "computer",
                    "ALT+F4" => "error",
                    _ => "keyboard"
                };
                SelectedButton.BackgroundColor = "#34495E";
            }
            else if (SelectedButton.ActionType == "OBS_SCENE_SWITCH")
            {
                SelectedButton.Label = presetValue switch
                {
                    "Escena Principal" => "OBS",
                    "START_RECORD" => "Grabar",
                    "START_STREAM" => "Stream",
                    "TOGGLE_MUTE" => "Mute Mic",
                    _ => presetValue
                };
                SelectedButton.IconName = presetValue switch
                {
                    "START_RECORD" => "record",
                    "START_STREAM" => "stream",
                    "TOGGLE_MUTE" => "mic_off",
                    _ => "tv"
                };
                SelectedButton.BackgroundColor = presetValue switch
                {
                    "START_RECORD" => "#F38BA8",
                    "START_STREAM" => "#A6E3A1",
                    "TOGGLE_MUTE" => "#FAB387",
                    _ => "#3498DB"
                };
            }
            else if (SelectedButton.ActionType == "LAUNCH_APP")
            {
                SelectedButton.IconImage = null;
                SelectedButton.Label = presetValue switch
                {
                    "WIN_MINIMIZE" => "Minimizar",
                    "WIN_MAXIMIZE" => "Maximizar",
                    "WIN_CLOSE" => "Cerrar Ventana",
                    _ => presetValue
                };
                SelectedButton.IconName = presetValue switch
                {
                    "WIN_MINIMIZE" => "computer",
                    "WIN_MAXIMIZE" => "apps",
                    "WIN_CLOSE" => "error",
                    _ => "launch"
                };
                SelectedButton.BackgroundColor = presetValue switch
                {
                    "WIN_MINIMIZE" => "#3498DB",
                    "WIN_MAXIMIZE" => "#2ECC71",
                    "WIN_CLOSE" => "#E74C3C",
                    _ => "#2A2A3C"
                };
            }

            RefreshProfileVolumeBarsState();
            if (presetValue is "VOLUME_SLIDER_PC" or "VOLUME_SLIDER_SPOTIFY")
            {
                _ = SaveButtonConfiguration();
                StatusMessage = $"🎚️ Barra deslizante '{SelectedButton.Label}' agregada al espacio de volumen del perfil";
            }
            else
            {
                StatusMessage = $"✓ Acción seleccionada: {SelectedButton.Label} ({presetValue})";
            }
        }

        [RelayCommand]
        public void BrowseFile()
        {
            if (SelectedButton == null) return;

            var dialog = new OpenFileDialog
            {
                Title = "Seleccionar Ejecutable o Acceso Directo",
                Filter = "Aplicaciones y Accesos Directos (*.exe;*.lnk;*.bat)|*.exe;*.lnk;*.bat|Todos los Archivos (*.*)|*.*"
            };

            if (dialog.ShowDialog() == true)
            {
                var filePath = dialog.FileName;
                var fileName = Path.GetFileNameWithoutExtension(filePath);

                SelectedButton.ActionType = "LAUNCH_APP";
                SelectedButton.ActionValue = filePath;
                SelectedButton.Label = fileName;
                SelectedButton.BackgroundColor = "#2A2A3C";
                SelectedButton.TryExtractIcon(filePath);

                StatusMessage = $"✓ Archivo seleccionado: {fileName}";
            }
        }

        [RelayCommand]
        public async Task ExecuteSelectedAction()
        {
            if (SelectedButton == null || string.IsNullOrWhiteSpace(SelectedButton.ActionValue)) return;

            try
            {
                if (SelectedButton.ActionType == "MEDIA_CONTROL" &&
                    SpotifyController.IsSpotifyAction(SelectedButton.ActionValue))
                {
                    var (ok, msg) = SpotifyController.ExecuteAction(SelectedButton.ActionValue);
                    StatusMessage = ok ? $"▶ Spotify: {msg}" : $"⚠️ Spotify: {msg}";
                    return;
                }

                if ((SelectedButton.ActionType == "LAUNCH_APP" || SelectedButton.ActionType == "SYSTEM_COMMAND") &&
                    WindowController.IsWindowAction(SelectedButton.ActionValue))
                {
                    var (ok, msg) = WindowController.ExecuteWindowAction(SelectedButton.ActionValue);
                    StatusMessage = ok ? $"▶ Ventana: {msg}" : $"⚠️ Ventana: {msg}";
                    return;
                }

                if (SelectedButton.ActionType == "OBS_SCENE_SWITCH")
                {
                    int portNumber = int.TryParse(ObsPort, out var p) ? p : 4455;
                    var (ok, msg) = await ObsWebSocketClient.ExecuteActionAsync(
                        ObsHost,
                        portNumber,
                        ObsPassword,
                        SelectedButton.ActionValue
                    );
                    StatusMessage = ok ? $"▶ OBS: {msg}" : $"⚠️ OBS: {msg}";
                    return;
                }

                var actionPayload = new
                {
                    type = "action",
                    uuid = SelectedClientUuid,
                    timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    payload = new
                    {
                        action_type = SelectedButton.ActionType,
                        action_value = SelectedButton.ActionValue
                    }
                };

                await _webSocketService.SendMessageAsync(actionPayload);

                if (SelectedButton.ActionType == "MEDIA_CONTROL")
                {
                    byte vk = SelectedButton.ActionValue switch
                    {
                        "PLAY_PAUSE" => 0xB3,
                        "NEXT_TRACK" => 0xB0,
                        "PREV_TRACK" => 0xB1,
                        "VOLUME_MUTE" => 0xAD,
                        "VOLUME_DOWN" => 0xAE,
                        "VOLUME_UP" => 0xAF,
                        _ => 0
                    };

                    if (vk != 0)
                    {
                        keybd_event(vk, 0, 0, UIntPtr.Zero);
                        keybd_event(vk, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    }
                }
                else if (SelectedButton.ActionType == "SYSTEM_COMMAND" && SelectedButton.ActionValue == "LOCK_WORKSTATION")
                {
                    LockWorkStation();
                }
                else if (SelectedButton.ActionType == "LAUNCH_APP")
                {
                    var processInfo = new ProcessStartInfo
                    {
                        FileName = "cmd.exe",
                        Arguments = $"/C start \"\" \"{SelectedButton.ActionValue}\"",
                        CreateNoWindow = true,
                        UseShellExecute = false
                    };
                    Process.Start(processInfo);
                }

                StatusMessage = $"▶ Ejecutando acción: {SelectedButton.Label} ({SelectedButton.ActionValue})";
            }
            catch (Exception ex)
            {
                StatusMessage = $"Error al ejecutar acción: {ex.Message}";
            }
        }

        [RelayCommand]
        public async Task SaveButtonConfiguration()
        {
            if (SelectedButton == null) return;

            var syncPayload = new
            {
                type = "db_sync",
                uuid = SelectedClientUuid,
                timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                payload = new
                {
                    action = "update_button",
                    button = new
                    {
                        id = SelectedButton.Id,
                        profile_id = SelectedButton.ProfileId,
                        position = SelectedButton.Position,
                        label = SelectedButton.Label,
                        action_type = SelectedButton.ActionType,
                        action_value = SelectedButton.ActionValue,
                        background_color = SelectedButton.BackgroundColor,
                        icon_name = SelectedButton.IconName
                    }
                }
            };

            try
            {
                RefreshProfileVolumeBarsState();
                await _webSocketService.SendMessageAsync(syncPayload);
                StatusMessage = $"✓ Botón #{SelectedButton.Position + 1} guardado y sincronizado";
            }
            catch (Exception ex)
            {
                StatusMessage = $"Error al sincronizar: {ex.Message}";
            }
        }

        [RelayCommand]
        public async Task DeleteButtonConfiguration()
        {
            if (SelectedButton == null) return;

            var profileId = SelectedButton.ProfileId;
            var removedPosition = SelectedButton.Position;

            // Compactar todos los botones configurados del perfil para que no queden huecos
            var configuredInProfile = _allButtons
                .Where(b => b.ProfileId == profileId && b.Position != removedPosition && b.IsConfigured)
                .OrderBy(b => b.Position)
                .ToList();

            _allButtons.RemoveAll(b => b.ProfileId == profileId);

            for (int pos = 0; pos < 20; pos++)
            {
                if (pos < configuredInProfile.Count)
                {
                    var item = configuredInProfile[pos];
                    item.Position = pos;
                    item.Id = $"btn-{profileId}-{pos}";
                    _allButtons.Add(item);
                }
                else
                {
                    _allButtons.Add(new ButtonModel(pos, "")
                    {
                        Id = $"btn-{profileId}-{pos}",
                        ProfileId = profileId,
                        ActionType = "NONE",
                        ActionValue = "",
                        BackgroundColor = "#1E1E2E",
                        IconName = "none",
                        IconImage = null
                    });
                }
            }

            UpdateVisibleButtons();

            try
            {
                await SyncFullWorkspace();
                StatusMessage = $"🗑️ Botón #{removedPosition + 1} eliminado y cuadrícula reacomodada";
            }
            catch (Exception ex)
            {
                StatusMessage = $"Error al eliminar botón: {ex.Message}";
            }
        }
    }
}
