using System;
using System.Windows.Media;
using CommunityToolkit.Mvvm.ComponentModel;
using StreamDockWinUI.Services;

namespace StreamDockWinUI.ViewModels
{
    public partial class ButtonModel : ObservableObject
    {
        [ObservableProperty]
        private string _id = string.Empty;

        [ObservableProperty]
        private string _profileId = "prof-default";

        [ObservableProperty]
        private int _position;

        [ObservableProperty]
        private string _label = string.Empty;

        [ObservableProperty]
        private string _iconName = "default";

        [ObservableProperty]
        private ImageSource? _iconImage;

        [ObservableProperty]
        private string _backgroundColor = "#1E1E2E";

        [ObservableProperty]
        private string _actionType = "KEYBOARD_HOTKEY";

        [ObservableProperty]
        private string _actionValue = "CTRL+SHIFT+M";

        [ObservableProperty]
        private bool _isSelected;

        [ObservableProperty]
        private bool _isMoving;

        public bool IsConfigured =>
            (!string.IsNullOrWhiteSpace(ActionType) && ActionType != "NONE") ||
            !string.IsNullOrWhiteSpace(Label) ||
            (!string.IsNullOrWhiteSpace(IconName) && IconName != "none" && IconName != "default") ||
            (!string.IsNullOrWhiteSpace(BackgroundColor) &&
             !BackgroundColor.Equals("#1E1E2E", StringComparison.OrdinalIgnoreCase) &&
             !BackgroundColor.Equals("#1A1B26", StringComparison.OrdinalIgnoreCase));

        public ButtonModel(int position, string label)
        {
            _id = $"btn-{position}";
            _position = position;
            _label = label;
        }

        partial void OnActionValueChanged(string value)
        {
            TryExtractIcon(value);
        }

        partial void OnIconNameChanged(string value)
        {
            if (!string.IsNullOrWhiteSpace(value) &&
                value.StartsWith(IconExtractor.Base64PngPrefix, StringComparison.OrdinalIgnoreCase))
            {
                var decoded = IconExtractor.Base64PngToImageSource(value);
                if (decoded != null)
                {
                    IconImage = decoded;
                }
            }
        }

        public void TryExtractIcon(string path)
        {
            if (!string.IsNullOrWhiteSpace(path))
            {
                var bitmap = IconExtractor.ExtractBitmapFromFile(path);
                if (bitmap != null)
                {
                    IconImage = bitmap;
                    var base64Png = IconExtractor.BitmapSourceToBase64Png(bitmap);
                    if (!string.IsNullOrEmpty(base64Png))
                    {
                        IconName = base64Png;
                    }
                    return;
                }
            }

            if (!string.IsNullOrWhiteSpace(IconName) &&
                IconName.StartsWith(IconExtractor.Base64PngPrefix, StringComparison.OrdinalIgnoreCase))
            {
                IconImage = IconExtractor.Base64PngToImageSource(IconName);
            }
        }
    }
}
