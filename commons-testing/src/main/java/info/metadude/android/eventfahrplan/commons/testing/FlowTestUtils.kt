@file:JvmName("FlowTestUtils")

package info.metadude.android.eventfahrplan.commons.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn

/**
 * Creates a [SharedFlow] starting [lazily][SharingStarted.Lazily]
 * which should only be used for testing purposes!
 */
fun <T> CoroutineScope.sharedFlowOf(value: T? = null): SharedFlow<T> =
    when (value) {
        null -> emptyFlow()
        else -> flowOf(value)
    }.shareIn(scope = this, started = SharingStarted.Lazily)
