package com.ismartcoding.plain.platform

expect fun feedWorkerOneTimeRequest(feedId: String)

expect fun feedWorkerStartRepeat()

expect fun feedWorkerCancelRepeat()
