use super::*;

#[test]
fn native_graphql_dispatch_runs_on_the_rust_worker() {
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .worker_threads(1)
        .thread_name("native-graphql-owner")
        .enable_all()
        .build()
        .unwrap();
    let caller = std::thread::current().id();
    let (worker, name) = execute_on_runtime(runtime.handle(), async {
        (
            std::thread::current().id(),
            std::thread::current().name().map(str::to_owned),
        )
    })
    .unwrap();
    assert_ne!(worker, caller);
    assert_eq!(name.as_deref(), Some("native-graphql-owner"));
}

#[test]
fn native_graphql_worker_failure_is_returned_to_the_caller() {
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .worker_threads(1)
        .enable_all()
        .build()
        .unwrap();
    let result = execute_on_runtime(runtime.handle(), async {
        panic!("native worker fixture failure");
    });
    assert!(
        result
            .unwrap_err()
            .contains("native worker fixture failure")
    );
}

#[test]
fn application_chat_fragment_works_on_the_bundled_native_schema() {
    use plain_libs::serde_json::{Value, json};
    use plain_rs::infrastructure::host::request_scope::NativeRequestScope;
    use plain_server::server::ContentServer;
    use std::time::Duration;

    let fragment = include_str!(
        "../../../../shared/src/commonMain/kotlin/com/ismartcoding/plain/graphql/Fragments.kt"
    )
    .lines()
    .find_map(|line| {
        line.trim_start()
            .strip_prefix("const val CHAT_ITEM = \"")
            .and_then(|fragment| fragment.strip_suffix('"'))
    })
    .expect("KMP chat item selection");
    let directory = plain_libs::temporary::tempdir().unwrap();
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .worker_threads(2)
        .enable_all()
        .build()
        .unwrap();
    let server = runtime.block_on(async {
        ContentServer::open_with_cache(
            &directory.path().join("data"),
            &directory.path().join("cache"),
        )
        .unwrap()
    });
    let executor = server.native_graphql().unwrap();
    let request = |query: String, variables: Value| -> Value {
        let body = json!({"query": query, "variables": variables}).to_string();
        let response = execute_graphql(&body, runtime.handle(), executor.clone()).unwrap();
        let parsed: Value = plain_libs::serde_json::from_str(&response).unwrap();
        assert!(parsed.get("errors").is_none(), "{parsed}");
        parsed["data"].clone()
    };

    assert_eq!(
        request(
            format!("query {{ chatConversations {{ id chat {{ {fragment} }} }} }}"),
            json!({}),
        )["chatConversations"],
        json!([]),
    );
    let scope = NativeRequestScope::default();
    let subscription = {
        let _entered = runtime.enter();
        let id = executor.open_subscriptions(scope.clone()).unwrap();
        let subscription = executor.subscription_session(id).unwrap();
        subscription
            .send(r#"{"type":"subscribe","id":"created","payload":{"query":"subscription { messagesCreated { id } }"}}"#)
            .unwrap();
        subscription
    };
    let read_event = || -> Value {
        let result = runtime.block_on(async {
            tokio::time::timeout(Duration::from_secs(5), subscription.read())
                .await
                .unwrap()
                .unwrap()
        });
        plain_libs::serde_json::from_str(&result).unwrap()
    };
    assert_eq!(
        read_event()["payload"]["extensions"]["subscriptionReady"],
        true
    );
    let text = "Native **chat** contract";
    let sent = request(
        format!(
            "mutation($targets: [String!]!, $content: ChatContentInput!) {{ sendChatItems(targets: $targets, content: $content) {{ {fragment} }} }}"
        ),
        json!({"targets": ["peer:local"], "content": {"message": {"text": text, "format": "MARKDOWN", "items": []}}}),
    );
    let sent = &sent["sendChatItems"][0];
    let id = sent["id"].as_str().unwrap();
    assert_eq!(sent["content"]["__typename"], "ChatMessageContent");
    assert_eq!(sent["content"]["text"], text);
    assert_eq!(sent["content"]["format"], "MARKDOWN");
    assert_eq!(
        read_event()["payload"]["data"]["messagesCreated"],
        json!([{"id": id}])
    );
    assert_eq!(
        request(
            format!("query($id: ID!) {{ chatItem(id: $id) {{ {fragment} }} }}"),
            json!({"id": id}),
        )["chatItem"]["content"],
        sent["content"]
    );
    scope.close();
    runtime.block_on(server.shutdown());
}
