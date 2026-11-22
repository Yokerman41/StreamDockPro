use serde::{Deserialize, Serialize};
use std::process::Command;
use tracing::{error, info};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProcessItem {
    pub pid: u32,
    pub name: String,
    pub window_title: String,
    pub memory_mb: u64,
    pub is_app: bool,
}

#[cfg(windows)]
#[link(name = "user32")]
extern "system" {
    fn EnumWindows(lpEnumFunc: unsafe extern "system" fn(isize, isize) -> i32, lParam: isize) -> i32;
    fn GetWindowThreadProcessId(hWnd: isize, lpdwProcessId: *mut u32) -> u32;
    fn GetWindowTextLengthW(hWnd: isize) -> i32;
    fn GetWindowTextW(hWnd: isize, lpString: *mut u16, nMaxCount: i32) -> i32;
    fn IsWindowVisible(hWnd: isize) -> i32;
}

#[cfg(windows)]
#[link(name = "kernel32")]
extern "system" {
    fn CreateToolhelp32Snapshot(dwFlags: u32, th32ProcessID: u32) -> isize;
    fn Process32FirstW(hSnapshot: isize, lppe: *mut PROCESSENTRY32W) -> i32;
    fn Process32NextW(hSnapshot: isize, lppe: *mut PROCESSENTRY32W) -> i32;
    fn OpenProcess(dwDesiredAccess: u32, bInheritHandle: i32, dwProcessId: u32) -> isize;
    fn TerminateProcess(hProcess: isize, uExitCode: u32) -> i32;
    fn CloseHandle(hObject: isize) -> i32;
}

#[cfg(windows)]
#[link(name = "psapi")]
extern "system" {
    fn GetProcessMemoryInfo(
        hProcess: isize,
        ppmc: *mut PROCESS_MEMORY_COUNTERS,
        cb: u32,
    ) -> i32;
}

#[cfg(windows)]
const TH32CS_SNAPPROCESS: u32 = 0x00000002;
#[cfg(windows)]
const PROCESS_TERMINATE: u32 = 0x0001;
#[cfg(windows)]
const PROCESS_QUERY_LIMITED_INFORMATION: u32 = 0x1000;

#[cfg(windows)]
#[allow(non_snake_case)]
#[repr(C)]
struct PROCESSENTRY32W {
    dwSize: u32,
    cntUsage: u32,
    th32ProcessID: u32,
    th32DefaultHeapID: usize,
    th32ModuleID: u32,
    cntThreads: u32,
    th32ParentProcessID: u32,
    pcPriClassBase: i32,
    dwFlags: u32,
    szExeFile: [u16; 260],
}

#[cfg(windows)]
#[allow(non_snake_case)]
#[repr(C)]
struct PROCESS_MEMORY_COUNTERS {
    cb: u32,
    PageFaultCount: u32,
    PeakWorkingSetSize: usize,
    WorkingSetSize: usize,
    QuotaPeakPagedPoolUsage: usize,
    QuotaPagedPoolUsage: usize,
    QuotaPeakNonPagedPoolUsage: usize,
    QuotaNonPagedPoolUsage: usize,
    PagefileUsage: usize,
    PeakPagefileUsage: usize,
}

#[cfg(windows)]
struct WindowEnumContext {
    windows: std::collections::HashMap<u32, String>,
}

#[cfg(windows)]
unsafe extern "system" fn enum_windows_proc(hwnd: isize, lparam: isize) -> i32 {
    let ctx = &mut *(lparam as *mut WindowEnumContext);
    if IsWindowVisible(hwnd) != 0 {
        let mut pid: u32 = 0;
        GetWindowThreadProcessId(hwnd, &mut pid);
        if pid != 0 {
            let length = GetWindowTextLengthW(hwnd);
            if length > 0 {
                let mut buffer: Vec<u16> = vec![0; (length + 1) as usize];
                let copied = GetWindowTextW(hwnd, buffer.as_mut_ptr(), length + 1);
                if copied > 0 {
                    let title = String::from_utf16_lossy(&buffer[..copied as usize])
                        .trim()
                        .to_string();
                    if !title.is_empty()
                        && title != "Default IME"
                        && title != "MSCTFIME UI"
                        && title != "Program Manager"
                    {
                        ctx.windows.entry(pid).or_insert(title);
                    }
                }
            }
        }
    }
    1
}

pub struct TaskManagerPlugin;

impl TaskManagerPlugin {
    pub fn get_process_list() -> Vec<ProcessItem> {
        #[cfg(windows)]
        {
            Self::get_process_list_windows()
        }
        #[cfg(not(windows))]
        {
            Vec::new()
        }
    }

    #[cfg(windows)]
    fn get_process_list_windows() -> Vec<ProcessItem> {
        let mut window_ctx = WindowEnumContext {
            windows: std::collections::HashMap::new(),
        };
        unsafe {
            EnumWindows(enum_windows_proc, &mut window_ctx as *mut _ as isize);
        }

        let mut processes = Vec::new();
        unsafe {
            let snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
            if snapshot == -1 || snapshot == 0 {
                error!("Failed to create process snapshot");
                return processes;
            }

            let mut pe: PROCESSENTRY32W = std::mem::zeroed();
            pe.dwSize = std::mem::size_of::<PROCESSENTRY32W>() as u32;

            if Process32FirstW(snapshot, &mut pe) != 0 {
                loop {
                    let pid = pe.th32ProcessID;
                    if pid != 0 {
                        let len = pe.szExeFile.iter().position(|&c| c == 0).unwrap_or(pe.szExeFile.len());
                        let name = String::from_utf16_lossy(&pe.szExeFile[..len]);

                        let window_title = window_ctx.windows.get(&pid).cloned().unwrap_or_default();
                        let is_app = !window_title.is_empty();

                        let mut memory_mb: u64 = 0;
                        let h_proc = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, 0, pid);
                        if h_proc != 0 && h_proc != -1 {
                            let mut pmc: PROCESS_MEMORY_COUNTERS = std::mem::zeroed();
                            pmc.cb = std::mem::size_of::<PROCESS_MEMORY_COUNTERS>() as u32;
                            if GetProcessMemoryInfo(h_proc, &mut pmc, pmc.cb) != 0 {
                                memory_mb = (pmc.WorkingSetSize / (1024 * 1024)) as u64;
                            }
                            CloseHandle(h_proc);
                        }

                        processes.push(ProcessItem {
                            pid,
                            name,
                            window_title,
                            memory_mb,
                            is_app,
                        });
                    }

                    if Process32NextW(snapshot, &mut pe) == 0 {
                        break;
                    }
                }
            }
            CloseHandle(snapshot);
        }

        // Ordenar: primero las aplicaciones con ventana visible (ordenadas por consumo de RAM descendente),
        // luego los procesos de fondo (ordenados por consumo de RAM descendente).
        processes.sort_by(|a, b| {
            b.is_app
                .cmp(&a.is_app)
                .then_with(|| b.memory_mb.cmp(&a.memory_mb))
                .then_with(|| a.name.to_lowercase().cmp(&b.name.to_lowercase()))
        });

        processes
    }

    pub fn kill_process(pid: u32) -> Result<(), String> {
        info!("Terminating process PID: {pid}");
        #[cfg(windows)]
        unsafe {
            let h_proc = OpenProcess(PROCESS_TERMINATE, 0, pid);
            if h_proc != 0 && h_proc != -1 {
                let res = TerminateProcess(h_proc, 1);
                CloseHandle(h_proc);
                if res != 0 {
                    info!("Successfully terminated process PID {pid} via TerminateProcess");
                    return Ok(());
                }
            }

            // Fallback usando taskkill /F /PID para subárboles de procesos o procesos elevados
            let status = Command::new("taskkill")
                .args(["/F", "/PID", &pid.to_string()])
                .status();

            match status {
                Ok(s) if s.success() => {
                    info!("Successfully terminated process PID {pid} via taskkill");
                    Ok(())
                }
                _ => {
                    error!("Failed to terminate process PID {pid}");
                    Err(format!("No se pudo finalizar el proceso con PID {pid}"))
                }
            }
        }
        #[cfg(not(windows))]
        {
            Ok(())
        }
    }
}
