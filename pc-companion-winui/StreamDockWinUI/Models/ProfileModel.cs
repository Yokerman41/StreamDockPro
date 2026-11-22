using CommunityToolkit.Mvvm.ComponentModel;

namespace StreamDockWinUI.Models
{
    public partial class ProfileModel : ObservableObject
    {
        [ObservableProperty]
        private string _id = string.Empty;

        [ObservableProperty]
        private string _name = string.Empty;

        [ObservableProperty]
        private string? _autoSwitchProcess;

        [ObservableProperty]
        private bool _isDefault;

        [ObservableProperty]
        private string _iconName = "home";

        [ObservableProperty]
        private string _backgroundColor = "#1E1E2E";

        public ProfileModel() { }

        public ProfileModel(string id, string name)
        {
            _id = id;
            _name = name;
        }
    }
}
