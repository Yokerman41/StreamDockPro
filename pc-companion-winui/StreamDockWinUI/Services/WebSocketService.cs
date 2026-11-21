using System;
using System.IO;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace StreamDockWinUI.Services
{
    public class WebSocketService
    {
        private ClientWebSocket _webSocket;
        private readonly Uri _serverUri;

        public event Action<string>? OnMessageReceived;

        public WebSocketService(string url = "ws://127.0.0.1:8081/ws")
        {
            _serverUri = new Uri(url);
            _webSocket = new ClientWebSocket();
        }

        public async Task ConnectAsync(CancellationToken cancellationToken = default)
        {
            if (_webSocket.State != WebSocketState.Open)
            {
                try
                {
                    _webSocket.Dispose();
                    _webSocket = new ClientWebSocket();
                    await _webSocket.ConnectAsync(_serverUri, cancellationToken);
                    _ = ListenLoopAsync(cancellationToken);
                }
                catch (Exception)
                {
                }
            }
        }

        public async Task SendMessageAsync(object payload, CancellationToken cancellationToken = default)
        {
            if (_webSocket.State != WebSocketState.Open)
            {
                await ConnectAsync(cancellationToken);
            }

            if (_webSocket.State == WebSocketState.Open)
            {
                var json = JsonConvert.SerializeObject(payload);
                var bytes = Encoding.UTF8.GetBytes(json);
                await _webSocket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, cancellationToken);
            }
        }

        private async Task ListenLoopAsync(CancellationToken cancellationToken)
        {
            var buffer = new byte[8192];
            while (_webSocket.State == WebSocketState.Open && !cancellationToken.IsCancellationRequested)
            {
                try
                {
                    using var ms = new MemoryStream();
                    WebSocketReceiveResult result;
                    do
                    {
                        result = await _webSocket.ReceiveAsync(new ArraySegment<byte>(buffer), cancellationToken);
                        if (result.MessageType == WebSocketMessageType.Close)
                        {
                            await _webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closing", cancellationToken);
                            return;
                        }
                        ms.Write(buffer, 0, result.Count);
                    }
                    while (!result.EndOfMessage);

                    ms.Seek(0, SeekOrigin.Begin);
                    if (result.MessageType == WebSocketMessageType.Text)
                    {
                        using var reader = new StreamReader(ms, Encoding.UTF8);
                        var message = await reader.ReadToEndAsync();
                        OnMessageReceived?.Invoke(message);
                    }
                }
                catch (Exception)
                {
                    break;
                }
            }
        }
    }
}
