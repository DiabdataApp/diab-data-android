package com.diabdata.core.ui.components.cardsList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.diabdata.core.utils.ui.ColoredIconCircle
import com.diabdata.core.utils.ui.ColoredIconCircleProps
import com.diabdata.core.utils.ui.SvgIcon
import com.diabdata.core.utils.ui.getItemShape

/**
 * A composable that renders a single card within a [CardsList] stack.
 *
 * The card layout follows a horizontal structure with up to four optional sections,
 * arranged from start to end:
 *
 * ```
 * ┌─────────────────────────────────────────────────────────┐
 * │  [Leading Icon]       [Content]       [Trailing Icon]   │
 * └─────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │  [Leading Icon]         [Content]            [Switch]   │
 * └─────────────────────────────────────────────────────────┘
 * ```
 *
 * ## Leading icon behavior
 * The leading section supports two mutually exclusive modes, resolved in priority order:
 * 1. **Colored circle icon** ([CardItem.leadingColoredCircleIcon]): an icon rendered inside
 *    a tinted circular background via [ColoredIconCircle].
 * 2. **Simple SVG icon** ([CardItem.leadingIcon]): a flat icon rendered via [SvgIcon]
 *    with [MaterialTheme.colorScheme.onSurface] tint.
 * 3. If neither is provided, no leading section is displayed.
 *
 * ## Trailing section
 * - If [CardItem.trailingIcon] is set, an [IconButton] is displayed on the trailing side.
 *   Clicking it invokes [CardItem.onTrailingIconClick] if provided.
 * - If [CardItem.switchState] and [CardItem.onSwitchChange] are both set,
 *   a [Switch] is displayed after the trailing icon.
 * - If [CardItem.switchState], a [CardItem.onSwitchChange] and [CardItem.trailingIcon] are set,
 *   a [Switch] with the trailing icon is displayed
 * - Both trailing icon and switch can coexist on the same card.
 *
 * ## Styling
 * - The card uses [MaterialTheme.colorScheme.surface] as its background color.
 * - The [shape] parameter controls the corner rounding, typically computed by [getItemShape]
 *   based on the card's position within the parent [CardsList] stack.
 *
 * ## Usage example
 * ```kotlin
 * CardListItem(
 *     cardItem = CardItem(
 *         leadingColoredCircleIcon = ColoredIconCircleProps(
 *             iconRes = R.drawable.ic_sensor,
 *             baseColor = Color(0xFF4CAF50)
 *         ),
 *         content = {
 *             Column {
 *                 Text("Glucose sensor", style = MaterialTheme.typography.bodyLarge)
 *                 Text("Expires in 3 days", style = MaterialTheme.typography.bodySmall)
 *             }
 *         },
 *         trailingIcon = R.drawable.ic_chevron_right,
 *         onTrailingIconClick = { /* navigate */ }
 *     ),
 *     shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
 * )
 * ```
 *
 * @param cardItem  The [CardItem] data model describing the content and behavior of this card.
 * @param shape     The [Shape] applied to the card's corners. Typically provided by [getItemShape]
 *                  to ensure correct rounding based on position within the stack.
 * @param modifier  [Modifier] applied to the root [Card] container.
 * @param alignTop  If true, the content will be aligned to the top of the card,
 *
 * @see CardItem                Data model describing the content of a single card.
 * @see CardsList               Parent composable that orchestrates the full card stack.
 * @see getItemShape            Utility that computes corner shapes based on position within the stack.
 * @see ColoredIconCircle       Composable rendering an icon inside a tinted circular background.
 * @see ColoredIconCircleProps  Data model for the colored circle icon configuration.
 * @see SvgIcon                 Composable rendering an SVG resource icon.
 */
@Composable
fun CardListItem(
    cardItem: CardItem,
    shape: Shape,
    modifier: Modifier = Modifier,
    alignTop: Boolean = false
) {
    val surfaceModifier = modifier.fillMaxWidth()

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                cardItem.leadingColoredCircleIcon != null -> {
                    ColoredIconCircle(
                        baseColor = cardItem.leadingColoredCircleIcon.baseColor,
                        iconRes = cardItem.leadingColoredCircleIcon.iconRes,
                        size = cardItem.leadingColoredCircleIcon.size,
                        iconSize = cardItem.leadingColoredCircleIcon.iconSize
                    )
                }
                cardItem.leadingIcon != null -> {
                    SvgIcon(
                        resId = cardItem.leadingIcon,
                        color = cardItem.leadingIconColor ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                cardItem.content()
            }

            if (cardItem.trailingIcon != null && cardItem.switchState == null) {
                val trailingOnClick = cardItem.onTrailingIconClick ?: cardItem.onClick

                if (trailingOnClick != null) {
                    IconButton(
                        onClick = trailingOnClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        SvgIcon(
                            resId = cardItem.trailingIcon,
                            color = if (cardItem.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    SvgIcon(
                        resId = cardItem.trailingIcon,
                        color = if (cardItem.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (cardItem.switchState != null && cardItem.onSwitchChange != null) {
                if (cardItem.trailingIcon != null) {
                    Switch(
                        checked = cardItem.switchState,
                        onCheckedChange = cardItem.onSwitchChange,
                        thumbContent = {
                            if (cardItem.switchState) {
                                SvgIcon(
                                    resId = cardItem.trailingIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                } else {
                    Switch(
                        checked = cardItem.switchState,
                        onCheckedChange = cardItem.onSwitchChange
                    )
                }
            }
        }
    }

    if (cardItem.onClick != null) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = if (cardItem.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = surfaceModifier,
            onClick = cardItem.onClick
        ) {
            content()
        }
    } else {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            modifier = surfaceModifier
        ) {
            content()
        }
    }
}