use std::net::{IpAddr, Ipv4Addr, SocketAddr, SocketAddrV4, UdpSocket as StdUdpSocket};
use std::sync::Arc;
use tokio::net::UdpSocket;
use tracing::{info, warn};

pub fn get_local_ipv4() -> Option<Ipv4Addr> {
    let socket = StdUdpSocket::bind("0.0.0.0:0").ok()?;
    socket.connect("8.8.8.8:80").ok()?;
    match socket.local_addr().ok()?.ip() {
        IpAddr::V4(ipv4) => Some(ipv4),
        IpAddr::V6(_) => None,
    }
}

pub fn get_hostname() -> String {
    std::env::var("COMPUTERNAME")
        .or_else(|_| std::env::var("HOSTNAME"))
        .unwrap_or_else(|_| "StreamDockPC".to_string())
}

fn encode_dns_name(name: &str) -> Vec<u8> {
    let mut bytes = Vec::new();
    for part in name.split('.') {
        if !part.is_empty() {
            let part_bytes = part.as_bytes();
            bytes.push(part_bytes.len() as u8);
            bytes.extend_from_slice(part_bytes);
        }
    }
    bytes.push(0);
    bytes
}

fn build_mdns_packet(port: u16, local_ip: Ipv4Addr, hostname: &str) -> Vec<u8> {
    let service_type = "_streamdock._tcp.local";
    let instance_name = format!("Stream Dock Pro ({hostname}).{}", service_type);
    let host_domain = format!("{hostname}.local");

    let service_bytes = encode_dns_name(service_type);
    let instance_bytes = encode_dns_name(&instance_name);
    let host_bytes = encode_dns_name(&host_domain);

    let mut packet: Vec<u8> = vec![
        0x00, 0x00, // Transaction ID
        0x84, 0x00, // Flags: Authoritative Response
        0x00, 0x00, // Questions: 0
        0x00, 0x01, // Answer RRs: 1 (PTR)
        0x00, 0x00, // Authority RRs: 0
        0x00, 0x03, // Additional RRs: 3 (SRV, TXT, A)
    ];

    // 1. Answer: PTR Record (_streamdock._tcp.local -> Instance Name)
    packet.extend_from_slice(&service_bytes);
    packet.extend_from_slice(&[0x00, 0x0C]); // Type: PTR (12)
    packet.extend_from_slice(&[0x00, 0x01]); // Class: IN (1)
    packet.extend_from_slice(&[0x00, 0x00, 0x00, 0x78]); // TTL: 120s
    packet.extend_from_slice(&(instance_bytes.len() as u16).to_be_bytes());
    packet.extend_from_slice(&instance_bytes);

    // 2. Additional: SRV Record (Instance Name -> Port + Host)
    packet.extend_from_slice(&instance_bytes);
    packet.extend_from_slice(&[0x00, 0x21]); // Type: SRV (33)
    packet.extend_from_slice(&[0x80, 0x01]); // Class: IN (1) + Flush Cache
    packet.extend_from_slice(&[0x00, 0x00, 0x00, 0x78]); // TTL: 120s
    let srv_rdlength = (6 + host_bytes.len()) as u16;
    packet.extend_from_slice(&srv_rdlength.to_be_bytes());
    packet.extend_from_slice(&[0x00, 0x00]); // Priority: 0
    packet.extend_from_slice(&[0x00, 0x00]); // Weight: 0
    packet.extend_from_slice(&port.to_be_bytes()); // Port
    packet.extend_from_slice(&host_bytes); // Target Host

    // 3. Additional: TXT Record (Instance Name -> TXT info)
    packet.extend_from_slice(&instance_bytes);
    packet.extend_from_slice(&[0x00, 0x10]); // Type: TXT (16)
    packet.extend_from_slice(&[0x80, 0x01]); // Class: IN (1) + Flush Cache
    packet.extend_from_slice(&[0x00, 0x00, 0x00, 0x78]); // TTL: 120s
    let txt_data = [13, b'v', b'e', b'r', b's', b'i', b'o', b'n', b'=', b'0', b'.', b'1', b'.', b'0'];
    packet.extend_from_slice(&(txt_data.len() as u16).to_be_bytes());
    packet.extend_from_slice(&txt_data);

    // 4. Additional: A Record (Host -> IPv4 Address) - REQUIRED by Android NSD
    packet.extend_from_slice(&host_bytes);
    packet.extend_from_slice(&[0x00, 0x01]); // Type: A (1)
    packet.extend_from_slice(&[0x80, 0x01]); // Class: IN (1) + Flush Cache
    packet.extend_from_slice(&[0x00, 0x00, 0x00, 0x78]); // TTL: 120s
    packet.extend_from_slice(&[0x00, 0x04]); // RDLength: 4 bytes
    packet.extend_from_slice(&local_ip.octets());

    packet
}

pub fn start_mdns_responder(port: u16) {
    start_discovery_services(port);
}

pub fn start_discovery_services(ws_port: u16) {
    // 1. mDNS Responder Service
    tokio::spawn(async move {
        let multicast_addr = SocketAddrV4::new(Ipv4Addr::new(224, 0, 0, 251), 5353);

        match UdpSocket::bind("0.0.0.0:0").await {
            Ok(socket) => {
                let _ = socket.set_broadcast(true);
                info!("✓ Native mDNS responder active: broadcasting '_streamdock._tcp.' on port {ws_port}");

                loop {
                    let local_ip = get_local_ipv4().unwrap_or(Ipv4Addr::new(127, 0, 0, 1));
                    let hostname = get_hostname();
                    let packet = build_mdns_packet(ws_port, local_ip, &hostname);

                    let _ = socket.send_to(&packet, multicast_addr).await;
                    tokio::time::sleep(tokio::time::Duration::from_secs(3)).await;
                }
            }
            Err(e) => {
                warn!("Could not bind UDP socket for mDNS responder: {e}");
            }
        }
    });

    // 2. UDP Broadcast Discovery Responder & Beacon (Port 8082)
    tokio::spawn(async move {
        let broadcast_target: SocketAddr = "255.255.255.255:8082".parse().unwrap();

        let socket = match UdpSocket::bind("0.0.0.0:8082").await {
            Ok(s) => {
                let _ = s.set_broadcast(true);
                info!("✓ UDP Discovery responder active on port 8082");
                Arc::new(s)
            }
            Err(e) => {
                warn!("Could not bind UDP socket on 8082 for discovery responder: {e}");
                return;
            }
        };

        // Listener for direct discover probes from mobile
        let listener_socket = socket.clone();
        tokio::spawn(async move {
            let mut buf = [0u8; 1024];
            loop {
                match listener_socket.recv_from(&mut buf).await {
                    Ok((len, peer)) => {
                        let msg = String::from_utf8_lossy(&buf[..len]);
                        if msg.contains("STREAMDOCK_DISCOVER") || msg.contains("discover") || msg.contains("STREAMDOCK") {
                            let local_ip = get_local_ipv4().unwrap_or(Ipv4Addr::new(127, 0, 0, 1));
                            let hostname = get_hostname();
                            let response = serde_json::json!({
                                "service": "streamdock",
                                "name": format!("Stream Dock Pro ({hostname})"),
                                "host": local_ip.to_string(),
                                "port": ws_port,
                                "version": "0.1.0"
                            });
                            let resp_bytes = response.to_string();
                            let _ = listener_socket.send_to(resp_bytes.as_bytes(), peer).await;
                        }
                    }
                    Err(_) => break,
                }
            }
        });

        // Periodic UDP Beacon (every 2 seconds)
        loop {
            let local_ip = get_local_ipv4().unwrap_or(Ipv4Addr::new(127, 0, 0, 1));
            let hostname = get_hostname();
            let beacon = serde_json::json!({
                "service": "streamdock",
                "name": format!("Stream Dock Pro ({hostname})"),
                "host": local_ip.to_string(),
                "port": ws_port,
                "version": "0.1.0"
            });
            let _ = socket.send_to(beacon.to_string().as_bytes(), broadcast_target).await;
            tokio::time::sleep(tokio::time::Duration::from_secs(2)).await;
        }
    });
}
