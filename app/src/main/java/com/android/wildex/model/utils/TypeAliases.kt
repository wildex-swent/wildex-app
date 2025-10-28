package com.android.wildex.model.utils

import com.android.wildex.model.achievement.InputKey

/** Type alias for IDs. */
typealias Id = String

/** Type alias for picture URLs. */
typealias URL = String

/** Type alias for input map used in achievements. */
typealias Input = Map<InputKey, List<Id>>

/** Type alias for condition functions used in achievements. */
typealias Condition = suspend (Input) -> Boolean
