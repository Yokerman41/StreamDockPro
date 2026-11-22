use std::ffi::CString;
use std::os::windows::ffi::OsStrExt;
use std::path::PathBuf;
use std::sync::{Mutex, OnceLock};
use tracing::{info, warn};

#[cfg(windows)]
extern "system" {
    fn LoadLibraryW(lpLibFileName: *const u16) -> isize;
    fn GetProcAddress(hModule: isize, lpProcName: *const u8) -> usize;
    fn FreeLibrary(hLibModule: isize) -> i32;
}

type FnLogin = unsafe extern "system" fn() -> i32;
type FnLogout = unsafe extern "system" fn() -> i32;
type FnGetVoicemeeterType = unsafe extern "system" fn(*mut i32) -> i32;
type FnGetVoicemeeterVersion = unsafe extern "system" fn(*mut i32) -> i32;
type FnIsParametersDirty = unsafe extern "system" fn() -> i32;
type FnGetParameterFloat = unsafe extern "system" fn(*const i8, *mut f32) -> i32;
type FnSetParameterFloat = unsafe extern "system" fn(*const i8, f32) -> i32;
type FnSetParameterStringA = unsafe extern "system" fn(*const i8, *const i8) -> i32;
type FnSetParameters = unsafe extern "system" fn(*const i8) -> i32;

struct VmrApi {
    _h_module: isize,
    _login: FnLogin,
    _logout: FnLogout,
    get_type: FnGetVoicemeeterType,
    _get_version: FnGetVoicemeeterVersion,
    _is_dirty: FnIsParametersDirty,
    get_param_float: FnGetParameterFloat,
    set_param_float: FnSetParameterFloat,
    _set_param_string_a: FnSetParameterStringA,
    set_parameters: FnSetParameters,
}

unsafe impl Send for VmrApi {}
unsafe impl Sync for VmrApi {}

impl VmrApi {
    fn load() -> Option<Self> {
        #[cfg(not(windows))]
        return None;

        #[cfg(windows)]
        {
            let candidates = [
                r"C:\Program Files (x86)\VB\Voicemeeter\VoicemeeterRemote64.dll",
                r"C:\Program Files\VB\Voicemeeter\VoicemeeterRemote64.dll",
                r"C:\Program Files (x86)\VB\Voicemeeter\VoicemeeterRemote.dll",
                r"C:\Program Files\VB\Voicemeeter\VoicemeeterRemote.dll",
            ];

            let mut chosen_path: Option<PathBuf> = None;
            for cand in candidates {
                let p = PathBuf::from(cand);
                if p.exists() {
                    chosen_path = Some(p);
                    break;
                }
            }

            let path = chosen_path?;
            let path_wide: Vec<u16> = path.as_os_str().encode_wide().chain(std::iter::once(0)).collect();

            let h_module = unsafe { LoadLibraryW(path_wide.as_ptr()) };
            if h_module == 0 {
                warn!("Failed to LoadLibraryW VoicemeeterRemote from {:?}", path);
                return None;
            }

            unsafe fn get_sym<T>(h: isize, name: &[u8]) -> Option<T> {
                let proc = GetProcAddress(h, name.as_ptr());
                if proc == 0 {
                    None
                } else {
                    Some(std::mem::transmute_copy(&proc))
                }
            }

            unsafe {
                let login: FnLogin = get_sym(h_module, b"VBVMR_Login\0")?;
                let logout: FnLogout = get_sym(h_module, b"VBVMR_Logout\0")?;
                let get_type: FnGetVoicemeeterType = get_sym(h_module, b"VBVMR_GetVoicemeeterType\0")?;
                let get_version: FnGetVoicemeeterVersion = get_sym(h_module, b"VBVMR_GetVoicemeeterVersion\0")?;
                let is_dirty: FnIsParametersDirty = get_sym(h_module, b"VBVMR_IsParametersDirty\0")?;
                let get_param_float: FnGetParameterFloat = get_sym(h_module, b"VBVMR_GetParameterFloat\0")?;
                let set_param_float: FnSetParameterFloat = get_sym(h_module, b"VBVMR_SetParameterFloat\0")?;
                let set_param_string_a: FnSetParameterStringA = get_sym(h_module, b"VBVMR_SetParameterStringA\0")?;
                let set_parameters: FnSetParameters = get_sym(h_module, b"VBVMR_SetParameters\0")?;

                let res_login = login();
                info!("Voicemeeter Remote API initialized successfully from {:?}. Login code: {}", path, res_login);

                Some(VmrApi {
                    _h_module: h_module,
                    _login: login,
                    _logout: logout,
                    get_type,
                    _get_version: get_version,
                    _is_dirty: is_dirty,
                    get_param_float,
                    set_param_float,
                    _set_param_string_a: set_param_string_a,
                    set_parameters,
                })
            }
        }
    }

    fn get_float(&self, param: &str) -> Option<f32> {
        let _ = unsafe { (self._is_dirty)() };
        let c_str = CString::new(param).ok()?;
        let mut val = 0.0f32;
        let res = unsafe { (self.get_param_float)(c_str.as_ptr(), &mut val) };
        if res == 0 {
            Some(val)
        } else {
            None
        }
    }

    fn set_float(&self, param: &str, val: f32) -> Result<(), String> {
        let c_str = CString::new(param).map_err(|e| e.to_string())?;
        let res = unsafe { (self.set_param_float)(c_str.as_ptr(), val) };
        if res == 0 {
            Ok(())
        } else {
            Err(format!("Voicemeeter set_float error: code {res}"))
        }
    }

    fn toggle_bool(&self, param: &str) -> Result<bool, String> {
        let _ = unsafe { (self._is_dirty)() };
        let current = self.get_float(param).unwrap_or(0.0);
        let new_val = if current > 0.5 { 0.0 } else { 1.0 };
        self.set_float(param, new_val)?;
        info!("Voicemeeter toggled '{}': {} -> {}", param, current, new_val);
        Ok(new_val > 0.5)
    }

    fn get_vm_type(&self) -> i32 {
        let mut t = 0i32;
        unsafe {
            (self.get_type)(&mut t);
        }
        t
    }

    fn run_script(&self, script: &str) -> Result<(), String> {
        let c_str = CString::new(script).map_err(|e| e.to_string())?;
        let res = unsafe { (self.set_parameters)(c_str.as_ptr()) };
        if res == 0 {
            Ok(())
        } else {
            Err(format!("Voicemeeter script error: code {res}"))
        }
    }
}

static VMR_INSTANCE: OnceLock<Mutex<Option<VmrApi>>> = OnceLock::new();

fn get_vmr() -> &'static Mutex<Option<VmrApi>> {
    VMR_INSTANCE.get_or_init(|| {
        let api = VmrApi::load();
        Mutex::new(api)
    })
}

pub struct VoicemeeterPlugin;

impl VoicemeeterPlugin {
    pub fn is_available() -> bool {
        let lock = get_vmr().lock().unwrap();
        lock.is_some()
    }

    pub fn get_state() -> serde_json::Value {
        let lock = get_vmr().lock().unwrap();
        let api = match lock.as_ref() {
            Some(a) => a,
            None => {
                return serde_json::json!({
                    "available": false,
                    "connected": false,
                    "error": "VoicemeeterRemote64.dll no disponible"
                });
            }
        };

        let vm_type = api.get_vm_type(); // 1=Standard, 2=Banana, 3=Potato
        let type_name = match vm_type {
            1 => "Voicemeeter",
            2 => "Voicemeeter Banana",
            3 => "Voicemeeter Potato",
            _ => "Desconocido",
        };

        // En Banana hay 5 tiras (3 Hardware + 2 Virtuales) y 5 buses (3 Hardware + 2 Virtuales)
        let num_strips = if vm_type == 1 { 3 } else if vm_type == 2 { 5 } else { 8 };
        let num_buses = if vm_type == 1 { 2 } else if vm_type == 2 { 5 } else { 8 };

        let mut strips = Vec::new();
        for i in 0..num_strips {
            let prefix = format!("Strip[{i}]");
            let gain = api.get_float(&format!("{prefix}.Gain")).unwrap_or(0.0);
            let mute = api.get_float(&format!("{prefix}.Mute")).unwrap_or(0.0) > 0.5;
            let solo = api.get_float(&format!("{prefix}.Solo")).unwrap_or(0.0) > 0.5;
            let mono = api.get_float(&format!("{prefix}.Mono")).unwrap_or(0.0) > 0.5;
            let a1 = api.get_float(&format!("{prefix}.A1")).unwrap_or(0.0) > 0.5;
            let a2 = api.get_float(&format!("{prefix}.A2")).unwrap_or(0.0) > 0.5;
            let a3 = api.get_float(&format!("{prefix}.A3")).unwrap_or(0.0) > 0.5;
            let b1 = api.get_float(&format!("{prefix}.B1")).unwrap_or(0.0) > 0.5;
            let b2 = api.get_float(&format!("{prefix}.B2")).unwrap_or(0.0) > 0.5;

            let default_name = match i {
                0 => "Micrófono (Input 1)",
                1 => "Hardware Input 2",
                2 => "Hardware Input 3",
                3 => "Voicemeeter Input (VAIO)",
                4 => "Voicemeeter AUX Input",
                _ => "Input",
            };

            strips.push(serde_json::json!({
                "index": i,
                "name": default_name,
                "is_virtual": i >= 3,
                "gain_db": gain,
                "mute": mute,
                "solo": solo,
                "mono": mono,
                "a1": a1,
                "a2": a2,
                "a3": a3,
                "b1": b1,
                "b2": b2,
            }));
        }

        let mut buses = Vec::new();
        for i in 0..num_buses {
            let prefix = format!("Bus[{i}]");
            let gain = api.get_float(&format!("{prefix}.Gain")).unwrap_or(0.0);
            let mute = api.get_float(&format!("{prefix}.Mute")).unwrap_or(0.0) > 0.5;
            let mono = api.get_float(&format!("{prefix}.Mono")).unwrap_or(0.0) > 0.5;
            let eq = api.get_float(&format!("{prefix}.EQ.on")).unwrap_or(0.0) > 0.5;

            let default_name = match i {
                0 => "A1 (Hardware Out)",
                1 => "A2 (Hardware Out)",
                2 => "A3 (Hardware Out)",
                3 => "B1 (Voicemeeter Out)",
                4 => "B2 (Voicemeeter AUX Out)",
                _ => "Bus Out",
            };

            buses.push(serde_json::json!({
                "index": i,
                "name": default_name,
                "is_virtual": i >= 3,
                "gain_db": gain,
                "mute": mute,
                "mono": mono,
                "eq": eq,
            }));
        }

        serde_json::json!({
            "available": true,
            "connected": true,
            "type_id": vm_type,
            "type_name": type_name,
            "strips": strips,
            "buses": buses,
        })
    }

    pub fn set_parameter_float(param: &str, val: f32) -> Result<(), String> {
        let lock = get_vmr().lock().unwrap();
        let api = lock.as_ref().ok_or_else(|| "Voicemeeter API no conectada".to_string())?;
        api.set_float(param, val)
    }

    pub fn toggle_parameter(param: &str) -> Result<bool, String> {
        let lock = get_vmr().lock().unwrap();
        let api = lock.as_ref().ok_or_else(|| "Voicemeeter API no conectada".to_string())?;
        api.toggle_bool(param)
    }

    pub fn set_strip_gain_percent(strip_idx: usize, pct: f32) -> Result<(), String> {
        // En Voicemeeter, el rango del fader va de -60 dB (silencio) a +12 dB
        // 0% -> -60 dB
        // 80% (o 100%) -> 0 dB
        // Si el usuario desliza de 0 a 100:
        // 0% = -60.0 dB
        // 100% = 0.0 dB (o +12dB si mapeamos proporcional)
        // Mapeo intuitivo: 0% = -60.0 dB, 100% = 0.0 dB
        let clamped = pct.clamp(0.0, 100.0);
        let db = if clamped <= 0.0 {
            -60.0
        } else if clamped >= 100.0 {
            0.0
        } else {
            -60.0 + (clamped / 100.0) * 60.0
        };

        let param = format!("Strip[{strip_idx}].Gain");
        Self::set_parameter_float(&param, db)
    }

    pub fn set_bus_gain_percent(bus_idx: usize, pct: f32) -> Result<(), String> {
        let clamped = pct.clamp(0.0, 100.0);
        let db = if clamped <= 0.0 {
            -60.0
        } else if clamped >= 100.0 {
            0.0
        } else {
            -60.0 + (clamped / 100.0) * 60.0
        };

        let param = format!("Bus[{bus_idx}].Gain");
        Self::set_parameter_float(&param, db)
    }

    pub fn restart_audio_engine() -> Result<(), String> {
        Self::set_parameter_float("Command.Restart", 1.0)
    }

    pub fn show_ui() -> Result<(), String> {
        Self::set_parameter_float("Command.Show", 1.0)
    }

    pub fn execute_action(action: &str) -> Result<(), String> {
        let norm = action.trim();
        info!("Executing Voicemeeter action: '{norm}'");

        if norm.eq_ignore_ascii_case("VOICEMEETER_RESTART") || norm.eq_ignore_ascii_case("VM_RESTART") {
            return Self::restart_audio_engine();
        }

        if norm.eq_ignore_ascii_case("VOICEMEETER_SHOW") || norm.eq_ignore_ascii_case("VM_SHOW") {
            return Self::show_ui();
        }

        // Salidas Master: Activar / Desactivar (Mute toggle)
        if norm.eq_ignore_ascii_case("VOICEMEETER_OUTPUT_A1") || norm.eq_ignore_ascii_case("VM_OUT_A1") {
            let _ = Self::toggle_parameter("Bus[0].Mute")?;
            return Ok(());
        }
        if norm.eq_ignore_ascii_case("VOICEMEETER_OUTPUT_A2") || norm.eq_ignore_ascii_case("VM_OUT_A2") {
            let _ = Self::toggle_parameter("Bus[1].Mute")?;
            return Ok(());
        }
        if norm.eq_ignore_ascii_case("VOICEMEETER_OUTPUT_A3") || norm.eq_ignore_ascii_case("VM_OUT_A3") {
            let _ = Self::toggle_parameter("Bus[2].Mute")?;
            return Ok(());
        }
        if norm.eq_ignore_ascii_case("VOICEMEETER_OUTPUT_B1") || norm.eq_ignore_ascii_case("VM_OUT_B1") {
            let _ = Self::toggle_parameter("Bus[3].Mute")?;
            return Ok(());
        }
        if norm.eq_ignore_ascii_case("VOICEMEETER_OUTPUT_B2") || norm.eq_ignore_ascii_case("VM_OUT_B2") {
            let _ = Self::toggle_parameter("Bus[4].Mute")?;
            return Ok(());
        }

        if let Some(out_name) = norm.strip_prefix("VOICEMEETER_OUTPUT:")
            .or_else(|| norm.strip_prefix("VM_OUTPUT:"))
        {
            let bus_idx = match out_name.trim().to_uppercase().as_str() {
                "A1" => 0,
                "A2" => 1,
                "A3" => 2,
                "B1" => 3,
                "B2" => 4,
                _ => 0,
            };
            let param = format!("Bus[{bus_idx}].Mute");
            let _ = Self::toggle_parameter(&param)?;
            return Ok(());
        }

        // Entradas (Inputs): Activar / Desactivar (Mute toggle)
        if norm.eq_ignore_ascii_case("VOICEMEETER_INPUT_MIC") || norm.eq_ignore_ascii_case("VM_IN_MIC") {
            let _ = Self::toggle_parameter("Strip[0].Mute")?;
            return Ok(());
        }
        if norm.eq_ignore_ascii_case("VOICEMEETER_INPUT_VAIO") || norm.eq_ignore_ascii_case("VM_IN_VAIO") {
            let _ = Self::toggle_parameter("Strip[3].Mute")?;
            return Ok(());
        }
        if norm.eq_ignore_ascii_case("VOICEMEETER_INPUT_AUX") || norm.eq_ignore_ascii_case("VM_IN_AUX") {
            let _ = Self::toggle_parameter("Strip[4].Mute")?;
            return Ok(());
        }

        if let Some(in_name) = norm.strip_prefix("VOICEMEETER_INPUT:")
            .or_else(|| norm.strip_prefix("VM_INPUT:"))
        {
            let strip_idx = match in_name.trim().to_uppercase().as_str() {
                "MIC" | "0" => 0,
                "1" => 1,
                "2" => 2,
                "VAIO" | "3" => 3,
                "AUX" | "4" => 4,
                _ => 0,
            };
            let param = format!("Strip[{strip_idx}].Mute");
            let _ = Self::toggle_parameter(&param)?;
            return Ok(());
        }

        // Enrutamiento (Routing): VOICEMEETER_ROUTE:Strip[0].A1 o VM_ROUTE:0:A1
        if let Some(route_str) = norm.strip_prefix("VOICEMEETER_ROUTE:")
            .or_else(|| norm.strip_prefix("VM_ROUTE:"))
        {
            let param = if route_str.contains('.') {
                route_str.trim().to_string()
            } else if let Some((strip_str, bus_str)) = route_str.split_once(':') {
                let strip_idx: usize = strip_str.trim().trim_start_matches("Strip[").trim_end_matches(']').parse().unwrap_or(0);
                let bus = bus_str.trim().to_uppercase();
                format!("Strip[{strip_idx}].{bus}")
            } else {
                route_str.trim().to_string()
            };
            let _ = Self::toggle_parameter(&param)?;
            return Ok(());
        }

        // Toggle genérico: e.g. VOICEMEETER_TOGGLE:Strip[0].Mute o VOICEMEETER_MUTE:Strip[0]
        if let Some(param) = norm.strip_prefix("VOICEMEETER_TOGGLE:")
            .or_else(|| norm.strip_prefix("VM_TOGGLE:"))
        {
            let _ = Self::toggle_parameter(param.trim())?;
            return Ok(());
        }

        if let Some(target) = norm.strip_prefix("VOICEMEETER_MUTE:")
            .or_else(|| norm.strip_prefix("VM_MUTE:"))
        {
            let t = target.trim();
            let param = if t.contains('.') {
                t.to_string()
            } else {
                format!("{t}.Mute")
            };
            let _ = Self::toggle_parameter(&param)?;
            return Ok(());
        }

        if let Some(target) = norm.strip_prefix("VOICEMEETER_SOLO:")
            .or_else(|| norm.strip_prefix("VM_SOLO:"))
        {
            let t = target.trim();
            let param = format!("{t}.Solo");
            let _ = Self::toggle_parameter(&param)?;
            return Ok(());
        }

        // Volume slider / set volume:
        // e.g. VOLUME_SLIDER_VM_STRIP0:75 o VOICEMEETER_SET_VOL:Strip[0]:80
        if let Some(val_str) = norm.strip_prefix("VOLUME_SLIDER_VM_STRIP0:")
            .or_else(|| norm.strip_prefix("VM_SET_STRIP0:"))
        {
            if let Ok(pct) = val_str.trim().parse::<f32>() {
                return Self::set_strip_gain_percent(0, pct);
            }
        }

        if let Some(val_str) = norm.strip_prefix("VOLUME_SLIDER_VM_STRIP1:")
            .or_else(|| norm.strip_prefix("VM_SET_STRIP1:"))
        {
            if let Ok(pct) = val_str.trim().parse::<f32>() {
                return Self::set_strip_gain_percent(1, pct);
            }
        }

        if let Some(val_str) = norm.strip_prefix("VOLUME_SLIDER_VM_STRIP2:")
            .or_else(|| norm.strip_prefix("VM_SET_STRIP2:"))
        {
            if let Ok(pct) = val_str.trim().parse::<f32>() {
                return Self::set_strip_gain_percent(2, pct);
            }
        }

        if let Some(val_str) = norm.strip_prefix("VOLUME_SLIDER_VM_STRIP3:")
            .or_else(|| norm.strip_prefix("VM_SET_STRIP3:"))
            .or_else(|| norm.strip_prefix("VOLUME_SLIDER_VM_VAIO:"))
        {
            if let Ok(pct) = val_str.trim().parse::<f32>() {
                return Self::set_strip_gain_percent(3, pct);
            }
        }

        if let Some(val_str) = norm.strip_prefix("VOLUME_SLIDER_VM_STRIP4:")
            .or_else(|| norm.strip_prefix("VM_SET_STRIP4:"))
            .or_else(|| norm.strip_prefix("VOLUME_SLIDER_VM_AUX:"))
        {
            if let Ok(pct) = val_str.trim().parse::<f32>() {
                return Self::set_strip_gain_percent(4, pct);
            }
        }

        if let Some(val_str) = norm.strip_prefix("VOLUME_SLIDER_VM_BUSA1:")
            .or_else(|| norm.strip_prefix("VM_SET_BUSA1:"))
        {
            if let Ok(pct) = val_str.trim().parse::<f32>() {
                return Self::set_bus_gain_percent(0, pct);
            }
        }

        if let Some(val_str) = norm.strip_prefix("VOLUME_SLIDER_VM_BUSA2:")
            .or_else(|| norm.strip_prefix("VM_SET_BUSA2:"))
        {
            if let Ok(pct) = val_str.trim().parse::<f32>() {
                return Self::set_bus_gain_percent(1, pct);
            }
        }

        // Generic: VOICEMEETER_SET_VOL:Strip[0]:80
        if let Some(rest) = norm.strip_prefix("VOICEMEETER_SET_VOL:")
            .or_else(|| norm.strip_prefix("VM_SET_VOL:"))
        {
            let parts: Vec<&str> = rest.split(':').collect();
            if parts.len() == 2 {
                let target = parts[0].trim();
                let pct = parts[1].trim().parse::<f32>().unwrap_or(0.0);
                if target.starts_with("Strip[") {
                    let idx_str = target.trim_start_matches("Strip[").trim_end_matches(']');
                    if let Ok(idx) = idx_str.parse::<usize>() {
                        return Self::set_strip_gain_percent(idx, pct);
                    }
                } else if target.starts_with("Bus[") {
                    let idx_str = target.trim_start_matches("Bus[").trim_end_matches(']');
                    if let Ok(idx) = idx_str.parse::<usize>() {
                        return Self::set_bus_gain_percent(idx, pct);
                    }
                }
            }
        }

        // Direct parameter set: e.g. VOICEMEETER_PARAM:Strip[0].A1=1.0
        if let Some(rest) = norm.strip_prefix("VOICEMEETER_PARAM:")
            .or_else(|| norm.strip_prefix("VM_PARAM:"))
        {
            if let Some((param, val_str)) = rest.split_once('=') {
                if let Ok(val) = val_str.trim().parse::<f32>() {
                    return Self::set_parameter_float(param.trim(), val);
                }
            }
        }

        warn!("Unrecognized Voicemeeter action: '{norm}'");
        Ok(())
    }
}
