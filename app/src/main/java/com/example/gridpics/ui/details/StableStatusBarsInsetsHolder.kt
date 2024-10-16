package com.example.gridpics.ui.details

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class StableStatusBarsInsetsHolder {
	private var stableStatusBarsInsets: WindowInsets = WindowInsets(0.dp)

	val stableStatusBars: WindowInsets
		@Composable
		get() {
			val density = LocalDensity.current
			val layoutDirection = LocalLayoutDirection.current
			val statusBars = WindowInsets.statusBars
			return remember {
				derivedStateOf {
					if (statusBars.exclude(stableStatusBarsInsets).getTop(density) > 0) {
						stableStatusBarsInsets = statusBars.deepCopy(density, layoutDirection)
					}
					stableStatusBarsInsets
				}
			}.value
		}

}

private fun WindowInsets.deepCopy(density: Density, layoutDirection: LayoutDirection): WindowInsets {
	return WindowInsets(
		left = getLeft(density, layoutDirection),
		top = getTop(density),
		right = getRight(density, layoutDirection),
		bottom = getBottom(density)
	)
}