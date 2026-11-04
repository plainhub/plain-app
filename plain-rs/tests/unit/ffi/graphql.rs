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
