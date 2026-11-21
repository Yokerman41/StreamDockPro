using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.WebSockets;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace StreamDockWinUI.Services
{
    public class ObsConnectionSettings
    {
        [JsonProperty("host")]
        public string Host { get; set; } = "127.0.0.1";

        [JsonProperty("port")]
        public int Port { get; set; } = 4455;

        [JsonProperty("password")]
        public string Password { get; set; } = string.Empty;
    }

    public static class ObsSettingsStorage
    {
        private static string SettingsFilePath
        {
            get
            {
                var dir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "StreamDockPro"
                );
                Directory.CreateDirectory(dir);
                return Path.Combine(dir, "obs_settings.json");
            }
        }

        public static ObsConnectionSettings Load()
        {
            try
            {
                if (File.Exists(SettingsFilePath))
                {
                    var json = File.ReadAllText(SettingsFilePath, Encoding.UTF8);
                    var settings = JsonConvert.DeserializeObject<ObsConnectionSettings>(json);
                    if (settings != null)
                    {
                        if (string.IsNullOrWhiteSpace(settings.Host))
                            settings.Host = "127.0.0.1";
                        if (settings.Port <= 0)
                            settings.Port = 4455;
                        settings.Password ??= string.Empty;

                        if (string.IsNullOrEmpty(settings.Password))
                        {
                            TryFillFromObsStudioConfig(settings);
                        }
                        return settings;
                    }
                }
            }
            catch
            {
                // Ignore read errors and fall back to defaults
            }

            var defaultSettings = new ObsConnectionSettings();
            if (TryFillFromObsStudioConfig(defaultSettings))
            {
                Save(defaultSettings);
            }
            return defaultSettings;
        }

        public static void Save(ObsConnectionSettings settings)
        {
            try
            {
                var json = JsonConvert.SerializeObject(settings, Formatting.Indented);
                File.WriteAllText(SettingsFilePath, json, Encoding.UTF8);
            }
            catch
            {
                // Ignore write errors
            }
        }

        private static bool TryFillFromObsStudioConfig(ObsConnectionSettings target)
        {
            try
            {
                var obsConfigPath = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                    "obs-studio",
                    "plugin_config",
                    "obs-websocket",
                    "config.json"
                );

                if (File.Exists(obsConfigPath))
                {
                    var obj = JObject.Parse(File.ReadAllText(obsConfigPath, Encoding.UTF8));
                    var pwd = obj["server_password"]?.ToString();
                    var port = obj["server_port"]?.Value<int?>();

                    if (port.HasValue && port.Value > 0 && target.Port == 4455)
                        target.Port = port.Value;

                    if (!string.IsNullOrEmpty(pwd))
                    {
                        target.Password = pwd;
                        return true;
                    }
                }
            }
            catch
            {
                // Ignore if OBS config is inaccessible
            }
            return false;
        }
    }

    public class ObsPresetItem
    {
        public string Title { get; set; } = string.Empty;
        public string Subtitle { get; set; } = string.Empty;
        public string ActionValue { get; set; } = string.Empty;
        public string IconEmoji { get; set; } = "🎬";
        public string AccentColor { get; set; } = "#89B4FA";
        public string ButtonLabel { get; set; } = string.Empty;
        public string ButtonIconName { get; set; } = "tv";
        public string ButtonBgColor { get; set; } = "#3498DB";
    }

    public class ObsScanResult
    {
        public bool Success { get; set; }
        public string Message { get; set; } = string.Empty;
        public string CurrentScene { get; set; } = string.Empty;
        public List<ObsPresetItem> Scenes { get; set; } = new();
        public List<ObsPresetItem> Sources { get; set; } = new();
    }

    public static class ObsWebSocketClient
    {
        public static async Task<ObsScanResult> ScanAsync(string host, int port, string password)
        {
            var result = new ObsScanResult();
            using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(5));
            using var ws = new ClientWebSocket();

            try
            {
                var uri = new Uri($"ws://{host.Trim()}:{port}");
                await ws.ConnectAsync(uri, cts.Token);

                var (authenticated, authError) = await AuthenticateHandshakeAsync(ws, password, cts.Token);
                if (!authenticated)
                {
                    result.Success = false;
                    result.Message = authError;
                    return result;
                }

                // 1. Obtener lista de escenas (GetSceneList)
                var sceneResp = await SendRequestAsync(ws, "GetSceneList", "req-scenes", null, cts.Token);
                var sceneNames = new List<string>();
                if (sceneResp?["requestStatus"]?["result"]?.Value<bool>() == true)
                {
                    result.CurrentScene = sceneResp["responseData"]?["currentProgramSceneName"]?.ToString() ?? string.Empty;
                    var scenesArr = sceneResp["responseData"]?["scenes"] as JArray;
                    if (scenesArr != null)
                    {
                        foreach (var s in scenesArr.Reverse())
                        {
                            var name = s["sceneName"]?.ToString();
                            if (!string.IsNullOrWhiteSpace(name))
                            {
                                sceneNames.Add(name);
                                result.Scenes.Add(new ObsPresetItem
                                {
                                    Title = name,
                                    Subtitle = name == result.CurrentScene ? "Escena activa actual" : "Cambiar a esta escena",
                                    ActionValue = name,
                                    IconEmoji = "🎬",
                                    AccentColor = "#89B4FA",
                                    ButtonLabel = name,
                                    ButtonIconName = "tv",
                                    ButtonBgColor = "#3498DB"
                                });
                            }
                        }
                    }
                }

                // 2. Obtener elementos/fuentes de cada escena (GetSceneItemList)
                var seenSceneItems = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
                foreach (var sceneName in sceneNames)
                {
                    var itemsResp = await SendRequestAsync(
                        ws,
                        "GetSceneItemList",
                        $"req-items-{sceneName}",
                        new { sceneName },
                        cts.Token
                    );

                    if (itemsResp?["requestStatus"]?["result"]?.Value<bool>() == true)
                    {
                        var itemsArr = itemsResp["responseData"]?["sceneItems"] as JArray;
                        if (itemsArr != null)
                        {
                            foreach (var item in itemsArr.Reverse())
                            {
                                var sourceName = item["sourceName"]?.ToString();
                                if (!string.IsNullOrWhiteSpace(sourceName))
                                {
                                    var key = $"{sceneName}|{sourceName}";
                                    if (seenSceneItems.Add(key))
                                    {
                                        result.Sources.Add(new ObsPresetItem
                                        {
                                            Title = $"{sourceName}",
                                            Subtitle = $"Mostrar/Ocultar en escena '{sceneName}'",
                                            ActionValue = $"SOURCE_TOGGLE:{sceneName}|{sourceName}",
                                            IconEmoji = "👁️",
                                            AccentColor = "#A6E3A1",
                                            ButtonLabel = sourceName,
                                            ButtonIconName = "tv",
                                            ButtonBgColor = "#16A085"
                                        });
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Obtener entradas globales / fuentes de audio (GetInputList)
                var inputsResp = await SendRequestAsync(ws, "GetInputList", "req-inputs", null, cts.Token);
                if (inputsResp?["requestStatus"]?["result"]?.Value<bool>() == true)
                {
                    var inputsArr = inputsResp["responseData"]?["inputs"] as JArray;
                    if (inputsArr != null)
                    {
                        foreach (var inp in inputsArr)
                        {
                            var inputName = inp["inputName"]?.ToString();
                            var inputKind = inp["unversionedInputKind"]?.ToString() ?? inp["inputKind"]?.ToString() ?? "";
                            if (!string.IsNullOrWhiteSpace(inputName))
                            {
                                bool isAudio = inputKind.Contains("wasapi", StringComparison.OrdinalIgnoreCase)
                                            || inputKind.Contains("audio", StringComparison.OrdinalIgnoreCase)
                                            || inputKind.Contains("pulse", StringComparison.OrdinalIgnoreCase)
                                            || inputKind.Contains("coreaudio", StringComparison.OrdinalIgnoreCase);

                                if (isAudio)
                                {
                                    result.Sources.Add(new ObsPresetItem
                                    {
                                        Title = $"{inputName} (Audio)",
                                        Subtitle = "Silenciar / Activar audio (Toggle Mute)",
                                        ActionValue = $"MUTE_INPUT:{inputName}",
                                        IconEmoji = "🎙️",
                                        AccentColor = "#FAB387",
                                        ButtonLabel = inputName,
                                        ButtonIconName = "mic_off",
                                        ButtonBgColor = "#D35400"
                                    });
                                }
                                else if (!result.Sources.Any(s => s.ButtonLabel.Equals(inputName, StringComparison.OrdinalIgnoreCase)))
                                {
                                    var targetScene = !string.IsNullOrWhiteSpace(result.CurrentScene)
                                        ? result.CurrentScene
                                        : sceneNames.FirstOrDefault() ?? "";
                                    if (!string.IsNullOrWhiteSpace(targetScene))
                                    {
                                        result.Sources.Add(new ObsPresetItem
                                        {
                                            Title = inputName,
                                            Subtitle = $"Fuente ({inputKind})",
                                            ActionValue = $"SOURCE_TOGGLE:{targetScene}|{inputName}",
                                            IconEmoji = "🧩",
                                            AccentColor = "#CBA6F7",
                                            ButtonLabel = inputName,
                                            ButtonIconName = "tv",
                                            ButtonBgColor = "#8E44AD"
                                        });
                                    }
                                }
                            }
                        }
                    }
                }

                await CloseQuietlyAsync(ws);

                result.Success = true;
                result.Message = $"✓ OBS conectado: {result.Scenes.Count} escena(s) y {result.Sources.Count} fuente(s) detectadas";
                return result;
            }
            catch (Exception ex)
            {
                result.Success = false;
                result.Message = $"⚠️ No se pudo conectar a OBS en {host}:{port} ({ex.Message})";
                return result;
            }
        }

        public static async Task<(bool Success, string Message)> ExecuteActionAsync(
            string host,
            int port,
            string password,
            string actionValue)
        {
            if (string.IsNullOrWhiteSpace(actionValue))
                return (false, "Valor de acción vacío");

            using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(4));
            using var ws = new ClientWebSocket();

            try
            {
                var uri = new Uri($"ws://{host.Trim()}:{port}");
                await ws.ConnectAsync(uri, cts.Token);

                var (authenticated, authError) = await AuthenticateHandshakeAsync(ws, password, cts.Token);
                if (!authenticated)
                    return (false, authError);

                if (actionValue.Equals("START_RECORD", StringComparison.OrdinalIgnoreCase) ||
                    actionValue.Equals("TOGGLE_RECORD", StringComparison.OrdinalIgnoreCase))
                {
                    await SendRequestAsync(ws, "ToggleRecord", "req-rec", null, cts.Token);
                    await CloseQuietlyAsync(ws);
                    return (true, "Grabación alternada en OBS");
                }

                if (actionValue.Equals("STOP_RECORD", StringComparison.OrdinalIgnoreCase))
                {
                    await SendRequestAsync(ws, "StopRecord", "req-stop-rec", null, cts.Token);
                    await CloseQuietlyAsync(ws);
                    return (true, "Grabación detenida en OBS");
                }

                if (actionValue.Equals("START_STREAM", StringComparison.OrdinalIgnoreCase) ||
                    actionValue.Equals("TOGGLE_STREAM", StringComparison.OrdinalIgnoreCase))
                {
                    await SendRequestAsync(ws, "ToggleStream", "req-stream", null, cts.Token);
                    await CloseQuietlyAsync(ws);
                    return (true, "Stream alternado en OBS");
                }

                if (actionValue.Equals("STOP_STREAM", StringComparison.OrdinalIgnoreCase))
                {
                    await SendRequestAsync(ws, "StopStream", "req-stop-stream", null, cts.Token);
                    await CloseQuietlyAsync(ws);
                    return (true, "Stream detenido en OBS");
                }

                if (actionValue.StartsWith("MUTE_INPUT:", StringComparison.OrdinalIgnoreCase))
                {
                    var inputName = actionValue.Substring("MUTE_INPUT:".Length).Trim();
                    await SendRequestAsync(ws, "ToggleInputMute", "req-mute", new { inputName }, cts.Token);
                    await CloseQuietlyAsync(ws);
                    return (true, $"Mute alternado para '{inputName}'");
                }

                if (actionValue.Equals("TOGGLE_MUTE", StringComparison.OrdinalIgnoreCase))
                {
                    var inputsResp = await SendRequestAsync(ws, "GetInputList", "req-inputs-mute", null, cts.Token);
                    var inputsArr = inputsResp?["responseData"]?["inputs"] as JArray;
                    var firstAudio = inputsArr?.FirstOrDefault(i =>
                    {
                        var kind = i["unversionedInputKind"]?.ToString() ?? i["inputKind"]?.ToString() ?? "";
                        return kind.Contains("wasapi", StringComparison.OrdinalIgnoreCase) ||
                               kind.Contains("audio", StringComparison.OrdinalIgnoreCase);
                    }) ?? inputsArr?.FirstOrDefault();

                    var inputName = firstAudio?["inputName"]?.ToString();
                    if (!string.IsNullOrWhiteSpace(inputName))
                    {
                        await SendRequestAsync(ws, "ToggleInputMute", "req-mute-default", new { inputName }, cts.Token);
                        await CloseQuietlyAsync(ws);
                        return (true, $"Mute alternado para '{inputName}'");
                    }
                    await CloseQuietlyAsync(ws);
                    return (false, "No se encontraron entradas de audio en OBS");
                }

                if (actionValue.StartsWith("SOURCE_TOGGLE:", StringComparison.OrdinalIgnoreCase))
                {
                    var payload = actionValue.Substring("SOURCE_TOGGLE:".Length);
                    var parts = payload.Split('|', 2);
                    if (parts.Length == 2)
                    {
                        var sceneName = parts[0].Trim();
                        var sourceName = parts[1].Trim();

                        var idResp = await SendRequestAsync(
                            ws,
                            "GetSceneItemId",
                            "req-item-id",
                            new { sceneName, sourceName },
                            cts.Token
                        );

                        var sceneItemId = idResp?["responseData"]?["sceneItemId"]?.Value<int>();
                        if (sceneItemId.HasValue)
                        {
                            var enabledResp = await SendRequestAsync(
                                ws,
                                "GetSceneItemEnabled",
                                "req-item-enabled",
                                new { sceneName, sceneItemId = sceneItemId.Value },
                                cts.Token
                            );
                            bool currentEnabled = enabledResp?["responseData"]?["sceneItemEnabled"]?.Value<bool>() ?? true;

                            await SendRequestAsync(
                                ws,
                                "SetSceneItemEnabled",
                                "req-set-enabled",
                                new { sceneName, sceneItemId = sceneItemId.Value, sceneItemEnabled = !currentEnabled },
                                cts.Token
                            );
                            await CloseQuietlyAsync(ws);
                            return (true, $"Fuente '{sourceName}' {(currentEnabled ? "ocultada" : "mostrada")} en '{sceneName}'");
                        }
                    }
                    await CloseQuietlyAsync(ws);
                    return (false, $"No se encontró la fuente '{actionValue}'");
                }

                // Por defecto: cambiar a la escena indicada
                var targetSceneName = actionValue.StartsWith("SCENE:", StringComparison.OrdinalIgnoreCase)
                    ? actionValue.Substring("SCENE:".Length).Trim()
                    : actionValue.Trim();

                var switchResp = await SendRequestAsync(
                    ws,
                    "SetCurrentProgramScene",
                    "req-switch-scene",
                    new { sceneName = targetSceneName },
                    cts.Token
                );

                await CloseQuietlyAsync(ws);
                bool ok = switchResp?["requestStatus"]?["result"]?.Value<bool>() ?? false;
                return ok
                    ? (true, $"Escena cambiada a '{targetSceneName}'")
                    : (false, $"OBS no encontró la escena '{targetSceneName}'");
            }
            catch (Exception ex)
            {
                return (false, $"Error OBS WebSocket: {ex.Message}");
            }
        }

        private static async Task<(bool Authenticated, string ErrorMessage)> AuthenticateHandshakeAsync(
            ClientWebSocket ws,
            string password,
            CancellationToken ct)
        {
            var helloJson = await ReceiveMessageAsync(ws, ct);
            if (string.IsNullOrWhiteSpace(helloJson))
                return (false, "OBS no respondió con el mensaje Hello (Verifica que sea OBS Studio 28+ con WebSocket v5)");

            var hello = JObject.Parse(helloJson);
            var d = hello["d"] as JObject;
            var authObj = d?["authentication"] as JObject;

            string? authResponse = null;
            if (authObj != null)
            {
                var challenge = authObj["challenge"]?.ToString() ?? string.Empty;
                var salt = authObj["salt"]?.ToString() ?? string.Empty;

                if (string.IsNullOrEmpty(password))
                {
                    return (false, "⚠️ OBS WebSocket requiere contraseña. Ingrésala en el campo 'Contraseña WebSocket'.");
                }

                authResponse = ComputeObsAuthResponse(password, salt, challenge);
            }

            var identifyPayload = new JObject
            {
                ["op"] = 1,
                ["d"] = new JObject
                {
                    ["rpcVersion"] = 1,
                    ["eventSubscriptions"] = 0
                }
            };

            if (!string.IsNullOrEmpty(authResponse))
            {
                identifyPayload["d"]!["authentication"] = authResponse;
            }

            await SendTextAsync(ws, identifyPayload.ToString(Formatting.None), ct);

            var identifiedJson = await ReceiveMessageAsync(ws, ct);
            if (string.IsNullOrWhiteSpace(identifiedJson))
            {
                return (false, "⚠️ Autenticación rechazada por OBS WebSocket (Verifica la contraseña)");
            }

            var identified = JObject.Parse(identifiedJson);
            if (identified["op"]?.Value<int>() != 2)
            {
                return (false, "⚠️ Respuesta inesperada durante la identificación con OBS WebSocket");
            }

            return (true, string.Empty);
        }

        private static string ComputeObsAuthResponse(string password, string salt, string challenge)
        {
            using var sha256 = SHA256.Create();
            var secretBytes = sha256.ComputeHash(Encoding.UTF8.GetBytes(password + salt));
            var secretBase64 = Convert.ToBase64String(secretBytes);

            var authBytes = sha256.ComputeHash(Encoding.UTF8.GetBytes(secretBase64 + challenge));
            return Convert.ToBase64String(authBytes);
        }

        private static async Task<JObject?> SendRequestAsync(
            ClientWebSocket ws,
            string requestType,
            string requestId,
            object? requestData,
            CancellationToken ct)
        {
            var d = new JObject
            {
                ["requestType"] = requestType,
                ["requestId"] = requestId
            };

            if (requestData != null)
            {
                d["requestData"] = JObject.FromObject(requestData);
            }

            var req = new JObject
            {
                ["op"] = 6,
                ["d"] = d
            };

            await SendTextAsync(ws, req.ToString(Formatting.None), ct);

            while (ws.State == WebSocketState.Open && !ct.IsCancellationRequested)
            {
                var msg = await ReceiveMessageAsync(ws, ct);
                if (string.IsNullOrWhiteSpace(msg))
                    return null;

                var obj = JObject.Parse(msg);
                if (obj["op"]?.Value<int>() == 7 &&
                    obj["d"]?["requestId"]?.ToString() == requestId)
                {
                    return obj["d"] as JObject;
                }
            }

            return null;
        }

        private static async Task SendTextAsync(ClientWebSocket ws, string text, CancellationToken ct)
        {
            var bytes = Encoding.UTF8.GetBytes(text);
            await ws.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, ct);
        }

        private static async Task<string?> ReceiveMessageAsync(ClientWebSocket ws, CancellationToken ct)
        {
            var buffer = new byte[8192];
            using var ms = new System.IO.MemoryStream();

            while (true)
            {
                var result = await ws.ReceiveAsync(new ArraySegment<byte>(buffer), ct);
                if (result.MessageType == WebSocketMessageType.Close)
                    return null;

                ms.Write(buffer, 0, result.Count);
                if (result.EndOfMessage)
                    break;
            }

            return Encoding.UTF8.GetString(ms.ToArray());
        }

        private static async Task CloseQuietlyAsync(ClientWebSocket ws)
        {
            try
            {
                if (ws.State == WebSocketState.Open)
                {
                    using var cts = new CancellationTokenSource(TimeSpan.FromMilliseconds(500));
                    await ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "Done", cts.Token);
                }
            }
            catch
            {
                // Ignore close errors
            }
        }
    }
}
