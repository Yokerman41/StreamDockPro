use std::ffi::c_void;
use std::path::PathBuf;
use std::process::Command;
use tracing::{info, warn};

#[cfg(windows)]
#[repr(C)]
#[derive(Clone, Copy)]
pub struct GUID {
    pub data1: u32,
    pub data2: u16,
    pub data3: u16,
    pub data4: [u8; 8],
}

#[cfg(windows)]
#[link(name = "user32")]
extern "system" {
    pub fn keybd_event(bVk: u8, bScan: u8, dwFlags: u32, dwExtraInfo: usize);
    pub fn LockWorkStation() -> i32;
    pub fn EnumWindows(lpEnumFunc: unsafe extern "system" fn(isize, isize) -> i32, lParam: isize) -> i32;
    pub fn GetWindowThreadProcessId(hWnd: isize, lpdwProcessId: *mut u32) -> u32;
    pub fn GetClassNameW(hWnd: isize, lpClassName: *mut u16, nMaxCount: i32) -> i32;
    pub fn GetWindowTextLengthW(hWnd: isize) -> i32;
    pub fn SendMessageW(hWnd: isize, Msg: u32, wParam: usize, lParam: isize) -> isize;
    pub fn PostMessageW(hWnd: isize, Msg: u32, wParam: usize, lParam: isize) -> i32;
    pub fn ShowWindow(hWnd: isize, nCmdShow: i32) -> i32;
    pub fn ShowWindowAsync(hWnd: isize, nCmdShow: i32) -> i32;
    pub fn SetForegroundWindow(hWnd: isize) -> i32;
    pub fn GetForegroundWindow() -> isize;
    pub fn IsWindowVisible(hWnd: isize) -> i32;
    pub fn IsZoomed(hWnd: isize) -> i32;
    pub fn IsIconic(hWnd: isize) -> i32;
}

#[cfg(windows)]
#[link(name = "kernel32")]
extern "system" {
    pub fn OpenProcess(dwDesiredAccess: u32, bInheritHandle: i32, dwProcessId: u32) -> isize;
    pub fn QueryFullProcessImageNameW(
        hProcess: isize,
        dwFlags: u32,
        lpExeName: *mut u16,
        lpdwSize: *mut u32,
    ) -> i32;
    pub fn CloseHandle(hObject: isize) -> i32;
}

#[cfg(windows)]
#[link(name = "ole32")]
extern "system" {
    pub fn CoInitializeEx(pvReserved: *mut c_void, dwCoInit: u32) -> i32;
    pub fn CoCreateInstance(
        rclsid: *const GUID,
        pUnkOuter: *mut c_void,
        dwClsContext: u32,
        riid: *const GUID,
        ppv: *mut *mut c_void,
    ) -> i32;
}

const WM_APPCOMMAND: u32 = 0x0319;
const APPCOMMAND_MEDIA_NEXTTRACK: isize = 11;
const APPCOMMAND_MEDIA_PREVIOUSTRACK: isize = 12;
const APPCOMMAND_MEDIA_STOP: isize = 13;
const APPCOMMAND_MEDIA_PLAY_PAUSE: isize = 14;
const SW_RESTORE: i32 = 9;
const PROCESS_QUERY_LIMITED_INFORMATION: u32 = 0x1000;

pub struct Win32InputPlugin;

impl Win32InputPlugin {
    pub fn send_hotkey(hotkey: &str) -> Result<(), String> {
        info!("Win32 SendInput hotkey simulated: {hotkey}");
        Ok(())
    }

    pub fn send_media_key(action: &str) -> Result<(), String> {
        let normalized = action.trim().to_uppercase();
        if normalized.starts_with("SPOTIFY_") || normalized.starts_with("VOLUME_SLIDER_SPOTIFY") {
            return Self::execute_spotify_action(&normalized);
        }

        if let Some(level_str) = normalized
            .strip_prefix("VOLUME_SET:")
            .or_else(|| normalized.strip_prefix("PC_SET_VOL:"))
            .or_else(|| normalized.strip_prefix("VOLUME_SLIDER_PC:"))
        {
            if let Ok(pct) = level_str.trim().parse::<f32>() {
                #[cfg(windows)]
                return Self::set_master_volume_scalar((pct / 100.0).clamp(0.0, 1.0));
                #[cfg(not(windows))]
                return Ok(());
            }
        }

        info!("Win32 Media Key simulating: {normalized}");
        let vk: u8 = match normalized.as_str() {
            "PLAY_PAUSE" => 0xB3,
            "NEXT_TRACK" => 0xB0,
            "PREV_TRACK" => 0xB1,
            "VOLUME_MUTE" | "VOLUME_SLIDER_PC" => 0xAD,
            "VOLUME_DOWN" => 0xAE,
            "VOLUME_UP" => 0xAF,
            _ => 0,
        };

        if vk != 0 {
            #[cfg(windows)]
            unsafe {
                keybd_event(vk, 0, 0, 0);
                keybd_event(vk, 0, 0x0002, 0);
            }
        }
        Ok(())
    }

    pub fn execute_spotify_action(action: &str) -> Result<(), String> {
        info!("Executing exclusive Spotify action: {action}");
        #[cfg(windows)]
        {
            if let Some(level_str) = action
                .strip_prefix("SPOTIFY_SET_VOL:")
                .or_else(|| action.strip_prefix("VOLUME_SLIDER_SPOTIFY:"))
            {
                if let Ok(pct) = level_str.trim().parse::<f32>() {
                    return Self::set_spotify_volume_scalar((pct / 100.0).clamp(0.0, 1.0));
                }
            }

            match action {
                "SPOTIFY_OPEN" => {
                    let hwnds = Self::find_spotify_windows();
                    if let Some(&hwnd) = hwnds.first() {
                        unsafe {
                            ShowWindow(hwnd, SW_RESTORE);
                            SetForegroundWindow(hwnd);
                        }
                        return Ok(());
                    }
                    return Self::launch_spotify();
                }
                "SPOTIFY_VOL_UP" => return Self::adjust_spotify_volume(0.08),
                "SPOTIFY_VOL_DOWN" => return Self::adjust_spotify_volume(-0.08),
                "SPOTIFY_MUTE" | "VOLUME_SLIDER_SPOTIFY" => return Self::toggle_spotify_mute(),
                _ => {
                    let app_cmd = match action {
                        "SPOTIFY_PLAY_PAUSE" => APPCOMMAND_MEDIA_PLAY_PAUSE,
                        "SPOTIFY_NEXT" => APPCOMMAND_MEDIA_NEXTTRACK,
                        "SPOTIFY_PREV" => APPCOMMAND_MEDIA_PREVIOUSTRACK,
                        "SPOTIFY_STOP" => APPCOMMAND_MEDIA_STOP,
                        _ => return Err(format!("Unsupported Spotify command: {action}")),
                    };

                    let hwnds = Self::find_spotify_windows();
                    if hwnds.is_empty() {
                        info!("Spotify window not found; launching Spotify automatically");
                        return Self::launch_spotify();
                    }

                    let lparam = app_cmd << 16;
                    for hwnd in hwnds {
                        unsafe {
                            SendMessageW(hwnd, WM_APPCOMMAND, hwnd as usize, lparam);
                        }
                    }
                }
            }
        }
        Ok(())
    }

    #[cfg(windows)]
    fn is_spotify_pid(pid: u32) -> bool {
        if pid == 0 {
            return false;
        }
        unsafe {
            let handle = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, 0, pid);
            if handle == 0 {
                return false;
            }
            let mut buf = [0u16; 512];
            let mut size = buf.len() as u32;
            let ok = QueryFullProcessImageNameW(handle, 0, buf.as_mut_ptr(), &mut size);
            CloseHandle(handle);
            if ok == 0 {
                return false;
            }
            let path = String::from_utf16_lossy(&buf[..size as usize]).to_lowercase();
            path.ends_with("\\spotify.exe") || path.ends_with("/spotify.exe")
        }
    }

    #[cfg(windows)]
    fn find_spotify_windows() -> Vec<isize> {
        unsafe extern "system" fn enum_proc(hwnd: isize, lparam: isize) -> i32 {
            let list = &mut *(lparam as *mut Vec<isize>);
            let mut pid: u32 = 0;
            GetWindowThreadProcessId(hwnd, &mut pid);
            if Win32InputPlugin::is_spotify_pid(pid) {
                let mut class_buf = [0u16; 128];
                let len = GetClassNameW(hwnd, class_buf.as_mut_ptr(), class_buf.len() as i32);
                if len > 0 {
                    let class_name = String::from_utf16_lossy(&class_buf[..len as usize]);
                    if class_name.starts_with("Chrome_WidgetWin_") && GetWindowTextLengthW(hwnd) > 0 {
                        list.push(hwnd);
                    }
                }
            }
            1
        }

        let mut windows: Vec<isize> = Vec::new();
        unsafe {
            EnumWindows(enum_proc, (&mut windows as *mut Vec<isize>) as isize);
        }
        windows
    }

    #[cfg(windows)]
    fn find_spotify_exe_path() -> Option<PathBuf> {
        if let Ok(appdata) = std::env::var("APPDATA") {
            let p = PathBuf::from(appdata).join("Spotify").join("Spotify.exe");
            if p.exists() {
                return Some(p);
            }
        }
        if let Ok(local) = std::env::var("LOCALAPPDATA") {
            let p1 = PathBuf::from(&local).join("Spotify").join("Spotify.exe");
            if p1.exists() {
                return Some(p1);
            }
            let p2 = PathBuf::from(&local)
                .join("Microsoft")
                .join("WindowsApps")
                .join("Spotify.exe");
            if p2.exists() {
                return Some(p2);
            }
        }
        None
    }

    #[cfg(windows)]
    fn launch_spotify() -> Result<(), String> {
        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x08000000;

        if let Some(exe) = Self::find_spotify_exe_path() {
            Command::new(exe)
                .spawn()
                .map_err(|e| format!("Failed to start Spotify.exe: {e}"))?;
            return Ok(());
        }
        Command::new("cmd")
            .args(["/C", "start", "", "spotify:"])
            .creation_flags(CREATE_NO_WINDOW)
            .spawn()
            .map_err(|e| format!("Failed to launch spotify URI: {e}"))?;
        Ok(())
    }

    #[cfg(windows)]
    fn adjust_spotify_volume(delta: f32) -> Result<(), String> {
        Self::with_spotify_audio_volumes(|vol_ptr, set_vol, get_vol, set_mute, _| unsafe {
            let mut current: f32 = 1.0;
            if get_vol(vol_ptr, &mut current) >= 0 {
                let target = (current + delta).clamp(0.0, 1.0);
                let empty_guid = GUID {
                    data1: 0,
                    data2: 0,
                    data3: 0,
                    data4: [0; 8],
                };
                let _ = set_vol(vol_ptr, target, &empty_guid);
                let _ = set_mute(vol_ptr, 0, &empty_guid);
            }
        })
    }

    #[cfg(windows)]
    fn toggle_spotify_mute() -> Result<(), String> {
        let mut target_mute: Option<i32> = None;
        Self::with_spotify_audio_volumes(|vol_ptr, _, _, set_mute, get_mute| unsafe {
            let next = match target_mute {
                Some(m) => m,
                None => {
                    let mut cur: i32 = 0;
                    let _ = get_mute(vol_ptr, &mut cur);
                    let m = if cur != 0 { 0 } else { 1 };
                    target_mute = Some(m);
                    m
                }
            };
            let empty_guid = GUID {
                data1: 0,
                data2: 0,
                data3: 0,
                data4: [0; 8],
            };
            let _ = set_mute(vol_ptr, next, &empty_guid);
        })
    }

    #[cfg(windows)]
    fn set_spotify_volume_scalar(level: f32) -> Result<(), String> {
        let target = level.clamp(0.0, 1.0);
        Self::with_spotify_audio_volumes(|vol_ptr, set_vol, _, set_mute, _| unsafe {
            let empty_guid = GUID {
                data1: 0,
                data2: 0,
                data3: 0,
                data4: [0; 8],
            };
            let _ = set_vol(vol_ptr, target, &empty_guid);
            if target > 0.0 {
                let _ = set_mute(vol_ptr, 0, &empty_guid);
            }
        })
    }

    #[cfg(windows)]
    fn set_master_volume_scalar(level: f32) -> Result<(), String> {
        const CLSID_MMDEVICE_ENUMERATOR: GUID = GUID {
            data1: 0xBCDE0395,
            data2: 0xE52F,
            data3: 0x467C,
            data4: [0x8E, 0x3D, 0xC4, 0x57, 0x92, 0x91, 0x69, 0x2E],
        };
        const IID_IMMDEVICE_ENUMERATOR: GUID = GUID {
            data1: 0xA95664D2,
            data2: 0x9614,
            data3: 0x4F35,
            data4: [0xA7, 0x46, 0xDE, 0x8D, 0xB6, 0x36, 0x17, 0xE6],
        };
        const IID_IAUDIO_ENDPOINT_VOLUME: GUID = GUID {
            data1: 0x5CDF2C82,
            data2: 0x841E,
            data3: 0x4546,
            data4: [0x97, 0x22, 0x0C, 0xF7, 0x40, 0x78, 0x22, 0x9A],
        };

        let target = level.clamp(0.0, 1.0);
        unsafe {
            let _ = CoInitializeEx(std::ptr::null_mut(), 0);

            let mut enumerator: *mut c_void = std::ptr::null_mut();
            let hr = CoCreateInstance(
                &CLSID_MMDEVICE_ENUMERATOR,
                std::ptr::null_mut(),
                23,
                &IID_IMMDEVICE_ENUMERATOR,
                &mut enumerator,
            );
            if hr < 0 || enumerator.is_null() {
                return Err("No se pudo abrir el controlador maestro de audio".to_string());
            }

            let enum_vtbl = *(enumerator as *mut *mut usize);
            let get_default_endpoint: unsafe extern "system" fn(
                *mut c_void,
                u32,
                u32,
                *mut *mut c_void,
            ) -> i32 = std::mem::transmute(*enum_vtbl.add(4));
            let release_enum: unsafe extern "system" fn(*mut c_void) -> u32 =
                std::mem::transmute(*enum_vtbl.add(2));

            let mut device: *mut c_void = std::ptr::null_mut();
            let hr_dev = get_default_endpoint(enumerator, 0, 1, &mut device);
            if hr_dev < 0 || device.is_null() {
                release_enum(enumerator);
                return Err("No se encontró dispositivo de salida de audio maestro".to_string());
            }

            let dev_vtbl = *(device as *mut *mut usize);
            let activate: unsafe extern "system" fn(
                *mut c_void,
                *const GUID,
                u32,
                *const usize,
                *mut *mut c_void,
            ) -> i32 = std::mem::transmute(*dev_vtbl.add(3));
            let release_dev: unsafe extern "system" fn(*mut c_void) -> u32 =
                std::mem::transmute(*dev_vtbl.add(2));

            let mut ep_vol: *mut c_void = std::ptr::null_mut();
            let hr_ep = activate(
                device,
                &IID_IAUDIO_ENDPOINT_VOLUME,
                23,
                std::ptr::null(),
                &mut ep_vol,
            );
            release_dev(device);
            release_enum(enumerator);

            if hr_ep < 0 || ep_vol.is_null() {
                return Err("No se pudo activar IAudioEndpointVolume".to_string());
            }

            let ep_vtbl = *(ep_vol as *mut *mut usize);
            let release_ep: unsafe extern "system" fn(*mut c_void) -> u32 =
                std::mem::transmute(*ep_vtbl.add(2));
            // IAudioEndpointVolume::SetMasterVolumeLevelScalar (slot 7)
            let set_master_scalar: unsafe extern "system" fn(
                *mut c_void,
                f32,
                *const GUID,
            ) -> i32 = std::mem::transmute(*ep_vtbl.add(7));
            // IAudioEndpointVolume::SetMute (slot 14)
            let set_mute: unsafe extern "system" fn(
                *mut c_void,
                i32,
                *const GUID,
            ) -> i32 = std::mem::transmute(*ep_vtbl.add(14));

            let empty_guid = GUID {
                data1: 0,
                data2: 0,
                data3: 0,
                data4: [0; 8],
            };
            let _ = set_master_scalar(ep_vol, target, &empty_guid);
            if target > 0.0 {
                let _ = set_mute(ep_vol, 0, &empty_guid);
            }
            release_ep(ep_vol);
        }
        Ok(())
    }

    #[cfg(windows)]
    fn with_spotify_audio_volumes<F>(mut callback: F) -> Result<(), String>
    where
        F: FnMut(
            *mut c_void,
            unsafe extern "system" fn(*mut c_void, f32, *const GUID) -> i32,
            unsafe extern "system" fn(*mut c_void, *mut f32) -> i32,
            unsafe extern "system" fn(*mut c_void, i32, *const GUID) -> i32,
            unsafe extern "system" fn(*mut c_void, *mut i32) -> i32,
        ),
    {
        const CLSID_MMDEVICE_ENUMERATOR: GUID = GUID {
            data1: 0xBCDE0395,
            data2: 0xE52F,
            data3: 0x467C,
            data4: [0x8E, 0x3D, 0xC4, 0x57, 0x92, 0x91, 0x69, 0x2E],
        };
        const IID_IMMDEVICE_ENUMERATOR: GUID = GUID {
            data1: 0xA95664D2,
            data2: 0x9614,
            data3: 0x4F35,
            data4: [0xA7, 0x46, 0xDE, 0x8D, 0xB6, 0x36, 0x17, 0xE6],
        };
        const IID_IAUDIO_SESSION_MANAGER2: GUID = GUID {
            data1: 0x77AA99A0,
            data2: 0x1BD6,
            data3: 0x484F,
            data4: [0x8B, 0xC7, 0x2C, 0x65, 0x4C, 0x9A, 0x9B, 0x6F],
        };
        const IID_IAUDIO_SESSION_CONTROL2: GUID = GUID {
            data1: 0xBFB7FF88,
            data2: 0x7239,
            data3: 0x4FC9,
            data4: [0x8F, 0xA2, 0x07, 0xC9, 0x50, 0xBE, 0x9C, 0x6D],
        };
        const IID_ISIMPLE_AUDIO_VOLUME: GUID = GUID {
            data1: 0x87CE5498,
            data2: 0x68D6,
            data3: 0x44E5,
            data4: [0x92, 0x15, 0x6D, 0xA4, 0x7E, 0xF8, 0x83, 0xD8],
        };

        unsafe {
            let _ = CoInitializeEx(std::ptr::null_mut(), 0);

            let mut enumerator: *mut c_void = std::ptr::null_mut();
            let hr = CoCreateInstance(
                &CLSID_MMDEVICE_ENUMERATOR,
                std::ptr::null_mut(),
                23, // CLSCTX_ALL
                &IID_IMMDEVICE_ENUMERATOR,
                &mut enumerator,
            );
            if hr < 0 || enumerator.is_null() {
                warn!("CoCreateInstance(MMDeviceEnumerator) failed: 0x{hr:08X}");
                return Err("No se pudo abrir el mezclador de audio de Windows".to_string());
            }

            // IMMDeviceEnumerator::GetDefaultAudioEndpoint (slot 4)
            let enum_vtbl = *(enumerator as *mut *mut usize);
            let get_default_endpoint: unsafe extern "system" fn(
                *mut c_void,
                u32,
                u32,
                *mut *mut c_void,
            ) -> i32 = std::mem::transmute(*enum_vtbl.add(4));
            let release_enum: unsafe extern "system" fn(*mut c_void) -> u32 =
                std::mem::transmute(*enum_vtbl.add(2));

            let mut device: *mut c_void = std::ptr::null_mut();
            let hr_dev = get_default_endpoint(enumerator, 0, 1, &mut device);
            if hr_dev < 0 || device.is_null() {
                release_enum(enumerator);
                return Err("No se encontró dispositivo de salida de audio".to_string());
            }

            // IMMDevice::Activate (slot 3)
            let dev_vtbl = *(device as *mut *mut usize);
            let activate: unsafe extern "system" fn(
                *mut c_void,
                *const GUID,
                u32,
                *const usize,
                *mut *mut c_void,
            ) -> i32 = std::mem::transmute(*dev_vtbl.add(3));
            let release_dev: unsafe extern "system" fn(*mut c_void) -> u32 =
                std::mem::transmute(*dev_vtbl.add(2));

            let mut mgr2: *mut c_void = std::ptr::null_mut();
            let hr_mgr = activate(
                device,
                &IID_IAUDIO_SESSION_MANAGER2,
                23,
                std::ptr::null(),
                &mut mgr2,
            );
            release_dev(device);
            release_enum(enumerator);

            if hr_mgr < 0 || mgr2.is_null() {
                return Err("No se pudo activar IAudioSessionManager2".to_string());
            }

            // IAudioSessionManager2::GetSessionEnumerator (slot 5)
            let mgr_vtbl = *(mgr2 as *mut *mut usize);
            let get_session_enum: unsafe extern "system" fn(*mut c_void, *mut *mut c_void) -> i32 =
                std::mem::transmute(*mgr_vtbl.add(5));
            let release_mgr: unsafe extern "system" fn(*mut c_void) -> u32 =
                std::mem::transmute(*mgr_vtbl.add(2));

            let mut session_enum: *mut c_void = std::ptr::null_mut();
            let hr_se = get_session_enum(mgr2, &mut session_enum);
            release_mgr(mgr2);

            if hr_se < 0 || session_enum.is_null() {
                return Err("No se pudo obtener el enumerador de sesiones de audio".to_string());
            }

            let se_vtbl = *(session_enum as *mut *mut usize);
            let get_count: unsafe extern "system" fn(*mut c_void, *mut i32) -> i32 =
                std::mem::transmute(*se_vtbl.add(3));
            let get_session: unsafe extern "system" fn(*mut c_void, i32, *mut *mut c_void) -> i32 =
                std::mem::transmute(*se_vtbl.add(4));
            let release_se: unsafe extern "system" fn(*mut c_void) -> u32 =
                std::mem::transmute(*se_vtbl.add(2));

            let mut count: i32 = 0;
            let _ = get_count(session_enum, &mut count);

            for i in 0..count {
                let mut session_ctl: *mut c_void = std::ptr::null_mut();
                if get_session(session_enum, i, &mut session_ctl) >= 0 && !session_ctl.is_null() {
                    let ctl_vtbl = *(session_ctl as *mut *mut usize);
                    let qi: unsafe extern "system" fn(
                        *mut c_void,
                        *const GUID,
                        *mut *mut c_void,
                    ) -> i32 = std::mem::transmute(*ctl_vtbl.add(0));
                    let release_ctl: unsafe extern "system" fn(*mut c_void) -> u32 =
                        std::mem::transmute(*ctl_vtbl.add(2));

                    let mut ctl2: *mut c_void = std::ptr::null_mut();
                    if qi(session_ctl, &IID_IAUDIO_SESSION_CONTROL2, &mut ctl2) >= 0
                        && !ctl2.is_null()
                    {
                        let ctl2_vtbl = *(ctl2 as *mut *mut usize);
                        let get_pid: unsafe extern "system" fn(*mut c_void, *mut u32) -> i32 =
                            std::mem::transmute(*ctl2_vtbl.add(14));
                        let release_ctl2: unsafe extern "system" fn(*mut c_void) -> u32 =
                            std::mem::transmute(*ctl2_vtbl.add(2));

                        let mut pid: u32 = 0;
                        if get_pid(ctl2, &mut pid) >= 0 && Self::is_spotify_pid(pid) {
                            let mut simple_vol: *mut c_void = std::ptr::null_mut();
                            if qi(session_ctl, &IID_ISIMPLE_AUDIO_VOLUME, &mut simple_vol) >= 0
                                && !simple_vol.is_null()
                            {
                                let vol_vtbl = *(simple_vol as *mut *mut usize);
                                let release_vol: unsafe extern "system" fn(*mut c_void) -> u32 =
                                    std::mem::transmute(*vol_vtbl.add(2));
                                let set_vol: unsafe extern "system" fn(
                                    *mut c_void,
                                    f32,
                                    *const GUID,
                                ) -> i32 = std::mem::transmute(*vol_vtbl.add(3));
                                let get_vol: unsafe extern "system" fn(
                                    *mut c_void,
                                    *mut f32,
                                ) -> i32 = std::mem::transmute(*vol_vtbl.add(4));
                                let set_mute: unsafe extern "system" fn(
                                    *mut c_void,
                                    i32,
                                    *const GUID,
                                ) -> i32 = std::mem::transmute(*vol_vtbl.add(5));
                                let get_mute: unsafe extern "system" fn(
                                    *mut c_void,
                                    *mut i32,
                                ) -> i32 = std::mem::transmute(*vol_vtbl.add(6));

                                callback(simple_vol, set_vol, get_vol, set_mute, get_mute);
                                release_vol(simple_vol);
                            }
                        }
                        release_ctl2(ctl2);
                    }
                    release_ctl(session_ctl);
                }
            }

            release_se(session_enum);
        }
        Ok(())
    }

    pub fn execute_window_action(action: &str) -> Result<(), String> {
        info!("Executing Window Action: {action}");
        #[cfg(windows)]
        {
            const SW_MAXIMIZE: i32 = 3;
            const SW_MINIMIZE: i32 = 6;
            const WM_CLOSE: u32 = 0x0010;

            let (cmd_part, target_app) = match action.split_once(':') {
                Some((c, app)) => (c.trim().to_uppercase(), Some(app.trim())),
                None => (action.trim().to_uppercase(), None),
            };

            let target_hwnd = if let Some(app) = target_app.filter(|s| !s.is_empty()) {
                Self::find_window_for_app(app).unwrap_or_else(Self::find_target_active_window)
            } else {
                Self::find_target_active_window()
            };

            if target_hwnd == 0 {
                return Err("No se encontró ninguna ventana activa para controlar".to_string());
            }

            unsafe {
                match cmd_part.as_str() {
                    "WIN_MINIMIZE" => {
                        ShowWindowAsync(target_hwnd, SW_MINIMIZE);
                    }
                    "WIN_MAXIMIZE" => {
                        if IsZoomed(target_hwnd) != 0 {
                            ShowWindowAsync(target_hwnd, SW_RESTORE);
                        } else {
                            ShowWindowAsync(target_hwnd, SW_MAXIMIZE);
                        }
                    }
                    "WIN_CLOSE" => {
                        PostMessageW(target_hwnd, WM_CLOSE, 0, 0);
                    }
                    _ => return Err(format!("Acción de ventana no reconocida: {cmd_part}")),
                }
            }
        }
        Ok(())
    }

    #[cfg(windows)]
    fn is_controllable_user_window(hwnd: isize) -> bool {
        if hwnd == 0 {
            return false;
        }
        unsafe {
            if IsWindowVisible(hwnd) == 0 || GetWindowTextLengthW(hwnd) <= 0 {
                return false;
            }
            let mut class_buf = [0u16; 128];
            let len = GetClassNameW(hwnd, class_buf.as_mut_ptr(), class_buf.len() as i32);
            if len > 0 {
                let cls = String::from_utf16_lossy(&class_buf[..len as usize]);
                let lower = cls.to_lowercase();
                if lower == "progman"
                    || lower == "workerw"
                    || lower == "shell_traywnd"
                    || lower == "shell_secondarytraywnd"
                    || lower == "windows.ui.core.corewindow"
                {
                    return false;
                }
            }
            true
        }
    }

    #[cfg(windows)]
    fn find_target_active_window() -> isize {
        unsafe {
            let fg = GetForegroundWindow();
            if Self::is_controllable_user_window(fg) {
                return fg;
            }

            unsafe extern "system" fn enum_active_proc(hwnd: isize, lparam: isize) -> i32 {
                let out = &mut *(lparam as *mut isize);
                if Win32InputPlugin::is_controllable_user_window(hwnd) && IsIconic(hwnd) == 0 {
                    *out = hwnd;
                    return 0;
                }
                1
            }

            let mut found: isize = 0;
            EnumWindows(enum_active_proc, (&mut found as *mut isize) as isize);
            found
        }
    }

    #[cfg(windows)]
    fn find_window_for_app(app_path_or_name: &str) -> Option<isize> {
        let stem = std::path::Path::new(app_path_or_name.trim_matches('"'))
            .file_stem()
            .and_then(|s| s.to_str())?
            .to_lowercase();
        let target_exe = format!("{stem}.exe");

        struct SearchCtx {
            target_exe: String,
            found: isize,
        }

        unsafe extern "system" fn enum_app_proc(hwnd: isize, lparam: isize) -> i32 {
            let ctx = &mut *(lparam as *mut SearchCtx);
            if !Win32InputPlugin::is_controllable_user_window(hwnd) {
                return 1;
            }
            let mut pid: u32 = 0;
            GetWindowThreadProcessId(hwnd, &mut pid);
            if pid != 0 {
                let handle = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, 0, pid);
                if handle != 0 {
                    let mut buf = [0u16; 512];
                    let mut size = buf.len() as u32;
                    let ok = QueryFullProcessImageNameW(handle, 0, buf.as_mut_ptr(), &mut size);
                    CloseHandle(handle);
                    if ok != 0 {
                        let path = String::from_utf16_lossy(&buf[..size as usize]).to_lowercase();
                        if path.ends_with(&format!("\\{}", ctx.target_exe))
                            || path.ends_with(&format!("/{}", ctx.target_exe))
                        {
                            ctx.found = hwnd;
                            return 0;
                        }
                    }
                }
            }
            1
        }

        let mut ctx = SearchCtx {
            target_exe,
            found: 0,
        };
        unsafe {
            EnumWindows(enum_app_proc, (&mut ctx as *mut SearchCtx) as isize);
        }
        if ctx.found != 0 {
            Some(ctx.found)
        } else {
            None
        }
    }

    pub fn execute_system_command(command: &str) -> Result<(), String> {
        let upper = command.trim().to_uppercase();
        if upper.starts_with("WIN_MINIMIZE")
            || upper.starts_with("WIN_MAXIMIZE")
            || upper.starts_with("WIN_CLOSE")
        {
            return Self::execute_window_action(command);
        }

        info!("Win32 System Command executing: {command}");
        match command {
            "LOCK_WORKSTATION" => {
                #[cfg(windows)]
                unsafe {
                    LockWorkStation();
                }
            }
            _ => {}
        }
        Ok(())
    }
}
