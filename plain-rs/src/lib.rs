// `PublicSchema` merges ~25 roots into one `QueryRoot`, and async-graphql's
// field-set resolver builds a nested future per field. Layout computation for
// that type recurses deeper than rustc's default 128. The limit has to live
// here rather than in `plain-rs`: the generic is monomorphized in whichever
// crate instantiates the schema, which for the mobile build is this one.
#![recursion_limit = "512"]

mod bootstrap;
mod ffi;
pub use plain_rs::libs::ffi::ble;
