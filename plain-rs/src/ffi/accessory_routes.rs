// PlainApp-specific policy for the generic USB streams.
use plain_libs::{protocols::http_client::Url, serde_json::Value};
type Result<T> = std::result::Result<T, u16>;
use super::remote::Route;
pub(super) fn resolve(op: u8, metadata: &[u8]) -> Result<Route> {
    let meta: Value = plain_libs::serde_json::from_slice(metadata).map_err(|_| 400u16)?;
    let id = meta["clientId"].as_str().ok_or(400u16)?;
    if !plain_rs::features::client_identity::is_valid(id) {
        return Err(400);
    }
    let path = meta["path"].as_str().ok_or(400u16)?;
    let (name, query) = path.split_once('?').unwrap_or((path, ""));
    let method = meta["method"].as_str().ok_or(400u16)?;
    let mut headers = vec![];
    let path = match (op, method, name) {
        (2, "GET", "/auth" | "/graphql") if query.is_empty() => format!("{name}?cid={id}"),
        (1, "POST", "/init" | "/graphql") if query.is_empty() => name.to_owned(),
        (1, "POST", "/upload" | "/upload_chunk") if query.is_empty() => {
            let mime = meta["contentType"].as_str().ok_or(400u16)?;
            let boundary = mime
                .strip_prefix("multipart/form-data; boundary=")
                .ok_or(400u16)?;
            if boundary.is_empty()
                || boundary.len() > 70
                || !boundary
                    .bytes()
                    .all(|b| b.is_ascii_alphanumeric() || b == b'-')
            {
                return Err(400);
            }
            headers.push(("content-type", mime.to_owned()));
            name.to_owned()
        }
        (1, "GET", "/fs") => {
            let url = Url::parse(&format!("http://localhost/fs?{query}")).map_err(|_| 400u16)?;
            let mut ids = url.query_pairs().filter(|(k, _)| k == "id");
            if !ids
                .next()
                .is_some_and(|(_, v)| v.starts_with(&format!("c.{id}.")))
                || ids.next().is_some()
            {
                return Err(403);
            }
            if let Some(range) = meta["range"].as_str() {
                headers.push(("range", range.to_owned()));
            }
            format!("/fs?{}", url.query().unwrap_or(""))
        }
        _ => return Err(403),
    };
    Ok(Route {
        id: id.to_owned(),
        path,
        post: method == "POST",
        headers,
    })
}

pub(super) fn endpoint(id: &str) -> Result<(tokio::runtime::Handle, u16)> {
    super::super::server::with_core(|core| {
        let state = core.server.runtime();
        if !state.prefs.get_or("remote_access", true)
            || !matches!(state.native_client_id(),Ok(native) if native!=id)
        {
            return Err(403);
        }
        let port = u16::try_from(state.http_server.snapshot().http_port)
            .ok()
            .filter(|p| *p != 0)
            .ok_or(503u16)?;
        Ok((core.runtime.handle().clone(), port))
    })
    .map_err(|_| 503u16)?
}
