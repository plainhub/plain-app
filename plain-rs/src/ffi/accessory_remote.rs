// Connection-local HTTP response streams and WebSockets. Product policy is applied before dialing.
use plain_libs::{
    protocols::http_client,
    tungstenite::{self, Message, WebSocket},
};
use std::{collections::HashMap, net::TcpStream, time::Duration};

type Result<T> = std::result::Result<T, u16>;
pub(super) struct Route {
    pub id: String,
    pub path: String,
    pub post: bool,
    pub headers: Vec<(&'static str, String)>,
}
pub(super) struct Policy {
    pub route: fn(u8, &[u8]) -> Result<Route>,
    pub endpoint: fn(&str) -> Result<(tokio::runtime::Handle, u16)>,
}
enum Channel {
    Socket(WebSocket<TcpStream>),
    Body(http_client::Response),
}
#[derive(Default)]
pub(super) struct Session {
    channels: HashMap<u8, (String, Channel)>,
}

impl Session {
    pub(super) fn exchange(&mut self, packet: &[u8], policy: &Policy) -> Vec<u8> {
        match self.dispatch(packet, policy) {
            Ok((status, data)) => [status.to_le_bytes().as_slice(), &data].concat(),
            Err(status) => status.to_le_bytes().to_vec(),
        }
    }
    fn dispatch(&mut self, packet: &[u8], policy: &Policy) -> Result<(u16, Vec<u8>)> {
        let (&[op, slot], rest) = packet.split_first_chunk::<2>().ok_or(400u16)?;
        // A browser document owns one logical session even when Android keeps
        // serving the same accessory descriptor across a browser refresh.
        if op == 9 && slot == 0 {
            if rest.len() != 16 {
                return Err(400);
            }
            self.channels.clear();
            return Ok((200, rest.to_vec()));
        }
        if op == 8 {
            self.channels.remove(&slot);
            return Ok((200, vec![]));
        }
        if matches!(op, 1 | 2) {
            if self.channels.contains_key(&slot) || self.channels.len() >= 64 {
                return Err(429);
            }
            let (len, data) = rest.split_first_chunk::<2>().ok_or(400u16)?;
            let (meta, body) = data
                .split_at_checked(u16::from_le_bytes(*len) as usize)
                .ok_or(400u16)?;
            let route = (policy.route)(op, meta)?;
            let (runtime, port) = (policy.endpoint)(&route.id)?;
            let uri = format!("127.0.0.1:{port}{}", route.path);
            if op == 2 {
                let timeout = Duration::from_secs(8);
                let stream = TcpStream::connect_timeout(&([127, 0, 0, 1], port).into(), timeout)
                    .map_err(|_| 502u16)?;
                stream.set_read_timeout(Some(timeout)).map_err(|_| 502u16)?;
                stream
                    .set_write_timeout(Some(timeout))
                    .map_err(|_| 502u16)?;
                let (mut ws, _) = tungstenite::client(format!("ws://{uri}").as_str(), stream)
                    .map_err(|_| 502u16)?;
                ws.get_mut()
                    .set_read_timeout(Some(Duration::from_millis(2)))
                    .map_err(|_| 502u16)?;
                self.channels.insert(slot, (route.id, Channel::Socket(ws)));
                return Ok((200, vec![]));
            }
            let client = http_client::builder()
                .no_proxy()
                .redirect(http_client::redirect::Policy::none())
                .build()
                .map_err(|_| 502u16)?;
            let url = format!("http://{uri}");
            let mut request = if route.post {
                client.post(&url).body(body.to_vec())
            } else {
                client.get(&url)
            };
            request = request.header("c-id", &route.id);
            for (name, value) in route.headers {
                request = request.header(name, value);
            }
            let res = runtime
                .block_on(async {
                    tokio::time::timeout(Duration::from_secs(15), request.send()).await
                })
                .map_err(|_| 504u16)?
                .map_err(|_| 502u16)?;
            let status = res.status().as_u16();
            let mut headers = plain_libs::serde_json::Map::new();
            for name in [
                "content-type",
                "content-length",
                "content-range",
                "accept-ranges",
                "content-disposition",
            ] {
                if let Some(value) = res.headers().get(name).and_then(|h| h.to_str().ok()) {
                    headers.insert(name.into(), value.into());
                }
            }
            self.channels.insert(slot, (route.id, Channel::Body(res)));
            return Ok((
                status,
                plain_libs::serde_json::to_vec(&headers).map_err(|_| 502u16)?,
            ));
        }
        let (id, channel) = self.channels.get_mut(&slot).ok_or(410u16)?;
        let (rt, _) = (policy.endpoint)(id)?;
        match (op, channel) {
            (6, Channel::Socket(ws)) => {
                let (&kind, data) = rest.split_first().ok_or(400u16)?;
                let msg = match kind {
                    1 => Message::Text(
                        std::str::from_utf8(data)
                            .map_err(|_| 400u16)?
                            .to_owned()
                            .into(),
                    ),
                    2 => Message::Binary(data.to_vec().into()),
                    _ => return Err(400),
                };
                ws.send(msg).map_err(|_| 502u16)?;
                Ok((200, vec![]))
            }
            (7, Channel::Socket(ws)) => match ws.read() {
                Ok(Message::Text(text)) => Ok((200, [&[1u8][..], text.as_bytes()].concat())),
                Ok(Message::Binary(bytes)) => Ok((200, [&[2u8][..], bytes.as_ref()].concat())),
                Ok(Message::Close(reason)) => Ok((
                    200,
                    [
                        &[3u8][..],
                        reason.as_ref().map_or(&[][..], |r| r.reason.as_bytes()),
                    ]
                    .concat(),
                )),
                Ok(_) => Ok((200, vec![])),
                Err(tungstenite::Error::Io(e))
                    if matches!(
                        e.kind(),
                        std::io::ErrorKind::WouldBlock | std::io::ErrorKind::TimedOut
                    ) =>
                {
                    Ok((200, vec![]))
                }
                Err(_) => Err(502),
            },
            (7, Channel::Body(res)) => Ok((
                200,
                rt.block_on(async {
                    tokio::time::timeout(Duration::from_secs(15), res.chunk()).await
                })
                .map_err(|_| 504u16)?
                .map_err(|_| 502u16)?
                .map_or_else(Vec::new, |b| b.to_vec()),
            )),
            _ => Err(400),
        }
    }
}
