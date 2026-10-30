package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.rust.graphql.GraphqlApi

internal object RustDlnaSender : DlnaSenderClient(GraphqlApi)
