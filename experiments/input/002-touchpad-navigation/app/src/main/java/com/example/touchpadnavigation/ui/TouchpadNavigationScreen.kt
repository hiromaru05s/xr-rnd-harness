package com.example.touchpadnavigation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.list.items
import androidx.xr.glimmer.list.VerticalStack
import androidx.xr.glimmer.list.rememberStackState
import androidx.xr.glimmer.onIndirectPointerGesture
import kotlinx.coroutines.launch

@Composable
fun TouchpadNavigationScreen(modifier: Modifier = Modifier) {
    var currentMode by remember { mutableStateOf(NavigationMode.CARD_STACK) }
    val stackState = rememberStackState()
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf("Waiting...") }
    val coroutineScope = rememberCoroutineScope()
    val totalCards = cardPages.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .onIndirectPointerGesture(
                onSwipeForward = {
                    when (currentMode) {
                        NavigationMode.CARD_STACK -> {
                            val nextIndex = (currentCardIndex + 1).coerceAtMost(totalCards - 1)
                            if (nextIndex != currentCardIndex) {
                                currentCardIndex = nextIndex
                                coroutineScope.launch {
                                    stackState.animateScrollToItem(currentCardIndex)
                                }
                                lastAction = "Swipe Fwd -> Card ${currentCardIndex + 1}/$totalCards"
                            }
                        }
                        NavigationMode.LIST_VIEW -> {
                            lastAction = "Swipe Fwd -> List focus down"
                        }
                    }
                },
                onSwipeBackward = {
                    when (currentMode) {
                        NavigationMode.CARD_STACK -> {
                            val prevIndex = (currentCardIndex - 1).coerceAtLeast(0)
                            if (prevIndex != currentCardIndex) {
                                currentCardIndex = prevIndex
                                coroutineScope.launch {
                                    stackState.animateScrollToItem(currentCardIndex)
                                }
                                lastAction = "Swipe Back -> Card ${currentCardIndex + 1}/$totalCards"
                            }
                        }
                        NavigationMode.LIST_VIEW -> {
                            lastAction = "Swipe Back -> List focus up"
                        }
                    }
                },
                onClick = {
                    currentMode = when (currentMode) {
                        NavigationMode.CARD_STACK -> NavigationMode.LIST_VIEW
                        NavigationMode.LIST_VIEW -> NavigationMode.CARD_STACK
                    }
                    lastAction = "Click -> ${currentMode.displayName}"
                },
            )
            .focusTarget(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TitleChip(
                leadingIcon = {
                    Icon(imageVector = Icons.Default.SwipeUp, contentDescription = null)
                }
            ) {
                Text(
                    when (currentMode) {
                        NavigationMode.CARD_STACK -> "Card ${currentCardIndex + 1}/$totalCards"
                        NavigationMode.LIST_VIEW -> "List View"
                    }
                )
            }

            when (currentMode) {
                NavigationMode.CARD_STACK -> {
                    CardStackContent(
                        stackState = stackState,
                        onCardAction = { action -> lastAction = action },
                    )
                }
                NavigationMode.LIST_VIEW -> {
                    ListContent(
                        onItemSelected = { label -> lastAction = "Selected: $label" },
                    )
                }
            }

            TitleChip(
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                }
            ) {
                Text(lastAction)
            }
        }
    }
}

@Composable
private fun CardStackContent(
    stackState: androidx.xr.glimmer.list.StackState,
    onCardAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    VerticalStack(
        state = stackState,
        modifier = modifier.fillMaxWidth(),
    ) {
        items(cardPages.size, key = { cardPages[it].title }) { index ->
            val page = cardPages[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                title = { Text(page.title) },
                action = {
                    Button(
                        onClick = { onCardAction("${page.title}: Action") },
                        buttonSize = ButtonSize.Medium,
                    ) { Text(page.actionLabel) }
                }
            ) {
                Button(
                    onClick = { onCardAction("${page.title}: Main") },
                    buttonSize = ButtonSize.Large,
                    leadingIcon = {
                        Icon(imageVector = page.icon, contentDescription = null)
                    }
                ) { Text(page.description) }
            }
        }
    }
}

@Composable
private fun ListContent(
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    VerticalList(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(listItems) { item ->
            ListItem(
                onClick = { onItemSelected(item.label) },
                leadingIcon = {
                    Icon(imageVector = item.icon, contentDescription = null)
                },
                supportingLabel = { Text("Tap to select") },
            ) { Text(item.label) }
        }
    }
}

private enum class NavigationMode(val displayName: String) {
    CARD_STACK("Card Stack"),
    LIST_VIEW("List View"),
}

private data class CardPage(
    val title: String,
    val description: String,
    val actionLabel: String,
    val icon: ImageVector,
)

private val cardPages = listOf(
    CardPage("Page 1", "Play", "Confirm", Icons.Default.PlayArrow),
    CardPage("Page 2", "Settings", "Change", Icons.Default.Settings),
    CardPage("Page 3", "Notifications", "Close", Icons.Default.Notifications),
)

private data class ListMenuItem(val label: String, val icon: ImageVector)

private val listItems = listOf(
    ListMenuItem("Notifications", Icons.Default.Notifications),
    ListMenuItem("Settings", Icons.Default.Settings),
    ListMenuItem("Help", Icons.Default.Info),
)
