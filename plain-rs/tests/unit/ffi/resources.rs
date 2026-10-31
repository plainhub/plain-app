use super::*;

#[test]
fn unknown_resource_does_not_replace_the_active_host() {
    let directory = std::env::temp_dir().join(plain_rs::utils::short_uuid::short_uuid());
    std::fs::create_dir(&directory).unwrap();
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .unwrap();
    let core = runtime.block_on(async {
        plain_server::server::ContentServer::open_with_cache(
            &directory.join("data"),
            &directory.join("cache"),
        )
        .unwrap()
    });
    super::super::events::install(&runtime, &core).unwrap();
    let handle = runtime.handle().clone();
    *super::super::server::CORE.lock().unwrap() = Some(super::super::server::Core {
        token: "test".into(),
        server: core,
        runtime,
    });
    let (_, host) = super::super::events::bridge().unwrap();
    let entered = handle.enter();
    assert_eq!(
        open("missing-stream").unwrap_err(),
        "resource stream unavailable"
    );
    host.publish_binary("SCREEN_MIRROR_VIDEO", vec![1]).unwrap();
    super::super::events::clear();
    drop(host);
    drop(entered);
    let core = super::super::server::CORE.lock().unwrap().take().unwrap();
    core.runtime.block_on(core.server.shutdown());
    drop(core.runtime);
    std::fs::remove_dir_all(directory).unwrap();
}
