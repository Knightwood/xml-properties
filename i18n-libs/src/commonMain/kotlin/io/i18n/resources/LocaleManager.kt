/*
 * Copyright [2023-2024] [KnightWood]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.i18n.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.LifecycleOwner

data class LocaleOwner(val locale: Locale)

object LocaleManager {
    private val LocalLocaleOwner = compositionLocalOf<LocaleOwner?> { null }

    public val current: LocaleOwner
        @Composable get() = LocalLocaleOwner.current ?: throw IllegalStateException(
            "CompositionLocal LifecycleOwner not present"
        )

    public infix fun provides(
        viewModelStoreOwner: LocaleOwner,
    ): ProvidedValue<LocaleOwner?> {
        return LocalLocaleOwner.provides(viewModelStoreOwner)
    }
}
