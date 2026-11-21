using CommunityToolkit.Mvvm.ComponentModel;

namespace StreamDockWinUI.Models
{
    public partial class ClientDeviceItem : ObservableObject
    {
        [ObservableProperty]
        [NotifyPropertyChangedFor(nameof(FullLabel))]
        private string _uuid = string.Empty;

        [ObservableProperty]
        [NotifyPropertyChangedFor(nameof(FullLabel))]
        private string _displayName = string.Empty;

        [ObservableProperty]
        private string _status = "Online";

        public string FullLabel
        {
            get
            {
                if (string.IsNullOrWhiteSpace(DisplayName))
                    return $"📱 Dispositivo ({Uuid})";
                return $"📱 {DisplayName} ({Uuid})";
            }
        }

        public ClientDeviceItem() { }

        public ClientDeviceItem(string uuid, string displayName, string status = "Online")
        {
            _uuid = uuid;
            _displayName = displayName;
            _status = status;
        }

        public override bool Equals(object? obj)
        {
            if (obj is ClientDeviceItem other)
            {
                return Uuid == other.Uuid;
            }
            return false;
        }

        public override int GetHashCode()
        {
            return Uuid.GetHashCode();
        }
    }
}
