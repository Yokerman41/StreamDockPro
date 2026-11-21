using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace StreamDockWinUI.Services
{
    public static class IconExtractor
    {
        public const string Base64PngPrefix = "data:image/png;base64,";

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
        private struct SHFILEINFO
        {
            public IntPtr hIcon;
            public int iIcon;
            public uint dwAttributes;
            [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 260)]
            public string szDisplayName;
            [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 80)]
            public string szTypeName;
        }

        private const uint SHGFI_ICON = 0x000000100;
        private const uint SHGFI_LARGEICON = 0x000000000;
        private const uint SHGFI_SYSICONINDEX = 0x000004000;

        private const int SHIL_EXTRALARGE = 0x02; // 48x48
        private const int SHIL_JUMBO = 0x04;      // 256x256

        private const int ILD_TRANSPARENT = 0x00000001;

        [ComImport]
        [Guid("46EB5926-582E-4017-9FDF-E8998DAA0950")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IImageList
        {
            [PreserveSig] int Add(IntPtr hbmImage, IntPtr hbmMask, ref int pi);
            [PreserveSig] int ReplaceIcon(int i, IntPtr hicon, ref int pi);
            [PreserveSig] int SetOverlayImage(int iImage, int iOverlay);
            [PreserveSig] int Replace(int i, IntPtr hbmImage, IntPtr hbmMask);
            [PreserveSig] int AddMasked(IntPtr hbmImage, int crMask, ref int pi);
            [PreserveSig] int Draw(IntPtr pimldp);
            [PreserveSig] int Remove(int i);
            [PreserveSig] int GetIcon(int i, int flags, out IntPtr picon);
        }

        [DllImport("shell32.dll", CharSet = CharSet.Auto)]
        private static extern IntPtr SHGetFileInfo(
            string pszPath,
            uint dwFileAttributes,
            ref SHFILEINFO psfi,
            uint cbFileInfo,
            uint uFlags);

        [DllImport("shell32.dll", EntryPoint = "#727")]
        private static extern int SHGetImageList(
            int iImageList,
            ref Guid riid,
            out IImageList ppv);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern bool DestroyIcon(IntPtr hIcon);

        public static BitmapSource? ExtractBitmapFromFile(string filePath)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(filePath)) return null;

                var cleanPath = ResolveShortcutIconTarget(filePath.Trim('"'));
                if (!File.Exists(cleanPath) && !Directory.Exists(cleanPath))
                {
                    cleanPath = filePath.Trim('"');
                }

                if (!File.Exists(cleanPath) && !Directory.Exists(cleanPath))
                {
                    return null;
                }

                // 1. Si es directamente un archivo .ico o imagen, decodificar el frame de mayor resolución (hasta 256x256)
                var ext = Path.GetExtension(cleanPath).ToLowerInvariant();
                if (ext == ".ico" || ext == ".png")
                {
                    var direct = LoadHighestResIconFile(cleanPath);
                    if (direct != null) return TrimTransparentBounds(direct);
                }

                // 2. Obtener el índice del icono en el Shell y extraer el icono JUMBO (256x256) sin el límite de 32x32
                var shfi = new SHFILEINFO();
                IntPtr infoRes = SHGetFileInfo(cleanPath, 0, ref shfi, (uint)Marshal.SizeOf(shfi), SHGFI_SYSICONINDEX);
                if (infoRes != IntPtr.Zero)
                {
                    var jumbo = ExtractFromShellImageList(shfi.iIcon, SHIL_JUMBO)
                             ?? ExtractFromShellImageList(shfi.iIcon, SHIL_EXTRALARGE);
                    if (jumbo != null)
                    {
                        return TrimTransparentBounds(jumbo);
                    }
                }

                // 3. Fallback a SHGFI_LARGEICON
                var fallbackInfo = new SHFILEINFO();
                IntPtr result = SHGetFileInfo(cleanPath, 0, ref fallbackInfo, (uint)Marshal.SizeOf(fallbackInfo), SHGFI_ICON | SHGFI_LARGEICON);
                if (result != IntPtr.Zero && fallbackInfo.hIcon != IntPtr.Zero)
                {
                    var bitmapSource = Imaging.CreateBitmapSourceFromHIcon(
                        fallbackInfo.hIcon,
                        Int32Rect.Empty,
                        BitmapSizeOptions.FromEmptyOptions());

                    bitmapSource.Freeze();
                    DestroyIcon(fallbackInfo.hIcon);
                    return TrimTransparentBounds(bitmapSource);
                }
            }
            catch (Exception)
            {
            }
            return null;
        }

        private static BitmapSource? ExtractFromShellImageList(int iconIndex, int sizeFlag)
        {
            try
            {
                Guid iidImageList = new Guid("46EB5926-582E-4017-9FDF-E8998DAA0950");
                int hr = SHGetImageList(sizeFlag, ref iidImageList, out IImageList imageList);
                if (hr == 0 && imageList != null)
                {
                    // ILD_TRANSPARENT sin overlay de flecha de acceso directo
                    int getHr = imageList.GetIcon(iconIndex, ILD_TRANSPARENT, out IntPtr hIcon);
                    if (getHr == 0 && hIcon != IntPtr.Zero)
                    {
                        var bmp = Imaging.CreateBitmapSourceFromHIcon(
                            hIcon,
                            Int32Rect.Empty,
                            BitmapSizeOptions.FromEmptyOptions());
                        bmp.Freeze();
                        DestroyIcon(hIcon);
                        return bmp;
                    }
                }
            }
            catch (Exception)
            {
            }
            return null;
        }

        private static BitmapSource? LoadHighestResIconFile(string path)
        {
            try
            {
                using var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
                var decoder = BitmapDecoder.Create(stream, BitmapCreateOptions.PreservePixelFormat, BitmapCacheOption.OnLoad);
                BitmapFrame? bestFrame = null;
                int bestArea = 0;
                foreach (var frame in decoder.Frames)
                {
                    int area = frame.PixelWidth * frame.PixelHeight;
                    if (area > bestArea)
                    {
                        bestArea = area;
                        bestFrame = frame;
                    }
                }
                if (bestFrame != null)
                {
                    bestFrame.Freeze();
                    return bestFrame;
                }
            }
            catch (Exception)
            {
            }
            return null;
        }

        /// <summary>
        /// Si el archivo es un acceso directo (.url de Steam/Epic/EA o .lnk), obtiene la ruta del
        /// icono o ejecutable original para extraerlo en 256x256 y sin la flecha de acceso directo.
        /// </summary>
        private static string ResolveShortcutIconTarget(string path)
        {
            try
            {
                var ext = Path.GetExtension(path).ToLowerInvariant();
                if (ext == ".url" && File.Exists(path))
                {
                    foreach (var line in File.ReadAllLines(path))
                    {
                        if (line.StartsWith("IconFile=", StringComparison.OrdinalIgnoreCase))
                        {
                            var iconFile = line.Substring("IconFile=".Length).Trim().Trim('"');
                            if (File.Exists(iconFile)) return iconFile;
                        }
                    }
                }
                else if (ext == ".lnk" && File.Exists(path))
                {
                    Type? shellType = Type.GetTypeFromProgID("WScript.Shell");
                    if (shellType != null)
                    {
                        dynamic? shell = Activator.CreateInstance(shellType);
                        if (shell != null)
                        {
                            dynamic shortcut = shell.CreateShortcut(path);
                            string? iconLoc = shortcut.IconLocation as string;
                            if (!string.IsNullOrWhiteSpace(iconLoc))
                            {
                                var iconPath = iconLoc.Split(',')[0].Trim().Trim('"');
                                if (File.Exists(iconPath)) return iconPath;
                            }
                            string? targetPath = shortcut.TargetPath as string;
                            if (!string.IsNullOrWhiteSpace(targetPath) && File.Exists(targetPath))
                            {
                                return targetPath;
                            }
                        }
                    }
                }
            }
            catch (Exception)
            {
            }
            return path;
        }

        /// <summary>
        /// Recorta los márgenes transparentes sobrantes (por ejemplo, cuando Windows coloca un icono
        /// de 48x48 en la esquina de un lienzo Jumbo de 256x256) para que el icono ocupe el 100%.
        /// </summary>
        private static BitmapSource TrimTransparentBounds(BitmapSource source)
        {
            try
            {
                var formatted = source.Format == PixelFormats.Bgra32
                    ? source
                    : new FormatConvertedBitmap(source, PixelFormats.Bgra32, null, 0);

                int width = formatted.PixelWidth;
                int height = formatted.PixelHeight;
                if (width <= 16 || height <= 16) return source;

                int stride = width * 4;
                byte[] pixels = new byte[height * stride];
                formatted.CopyPixels(pixels, stride, 0);

                int top = height, left = width, right = -1, bottom = -1;
                for (int y = 0; y < height; y++)
                {
                    int row = y * stride;
                    for (int x = 0; x < width; x++)
                    {
                        byte alpha = pixels[row + x * 4 + 3];
                        if (alpha > 12)
                        {
                            if (x < left) left = x;
                            if (x > right) right = x;
                            if (y < top) top = y;
                            if (y > bottom) bottom = y;
                        }
                    }
                }

                if (right < left || bottom < top) return source;

                int cropW = right - left + 1;
                int cropH = bottom - top + 1;
                if (cropW >= width * 0.94 && cropH >= height * 0.94) return source;

                var cropped = new CroppedBitmap(formatted, new Int32Rect(left, top, cropW, cropH));
                cropped.Freeze();
                return cropped;
            }
            catch (Exception)
            {
                return source;
            }
        }

        public static ImageSource? ExtractIconFromFile(string filePath) => ExtractBitmapFromFile(filePath);

        public static string? BitmapSourceToBase64Png(BitmapSource? bitmap)
        {
            if (bitmap == null) return null;
            try
            {
                using var ms = new MemoryStream();
                var encoder = new PngBitmapEncoder();
                encoder.Frames.Add(BitmapFrame.Create(bitmap));
                encoder.Save(ms);
                return Base64PngPrefix + Convert.ToBase64String(ms.ToArray());
            }
            catch (Exception)
            {
                return null;
            }
        }

        public static ImageSource? Base64PngToImageSource(string? iconName)
        {
            if (string.IsNullOrWhiteSpace(iconName)) return null;
            try
            {
                string rawBase64 = iconName.StartsWith(Base64PngPrefix, StringComparison.OrdinalIgnoreCase)
                    ? iconName.Substring(Base64PngPrefix.Length)
                    : iconName;

                byte[] bytes = Convert.FromBase64String(rawBase64);
                using var ms = new MemoryStream(bytes);
                var bitmap = new BitmapImage();
                bitmap.BeginInit();
                bitmap.CacheOption = BitmapCacheOption.OnLoad;
                bitmap.StreamSource = ms;
                bitmap.EndInit();
                bitmap.Freeze();
                return bitmap;
            }
            catch (Exception)
            {
                return null;
            }
        }
    }
}
