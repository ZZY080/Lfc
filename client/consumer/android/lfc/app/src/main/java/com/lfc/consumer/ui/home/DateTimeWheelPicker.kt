package com.lfc.consumer.ui.home

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private val isoDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
private val displayDateTimeFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())

fun formatActivityDateTimeForDisplay(value: String): String {
    if (value.isBlank()) return ""
    return runCatching {
        displayDateTimeFormat.format(isoDateTimeFormat.parse(value.trim())!!)
    }.getOrElse { value }
}

fun formatActivityDateTimeForApi(calendar: Calendar): String = isoDateTimeFormat.format(calendar.time)

fun parseActivityDateTime(value: String): Calendar? {
    if (value.isBlank()) return null
    val normalized = value.trim().replace(" ", "T").take(16)
    return runCatching {
        Calendar.getInstance().apply { time = isoDateTimeFormat.parse(normalized)!! }
    }.getOrNull()
}

fun defaultActivityStartCalendar(): Calendar = Calendar.getInstance().apply {
    add(Calendar.HOUR_OF_DAY, 1)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDateTimePickerSheet(
    visible: Boolean,
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialCalendar = remember(initialValue) {
        parseActivityDateTime(initialValue) ?: defaultActivityStartCalendar()
    }
    var year by remember(initialValue) { mutableIntStateOf(initialCalendar.get(Calendar.YEAR)) }
    var month by remember(initialValue) { mutableIntStateOf(initialCalendar.get(Calendar.MONTH) + 1) }
    var day by remember(initialValue) { mutableIntStateOf(initialCalendar.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember(initialValue) { mutableIntStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember(initialValue) { mutableIntStateOf(initialCalendar.get(Calendar.MINUTE)) }

    val years = remember { (Calendar.getInstance().get(Calendar.YEAR)..Calendar.getInstance().get(Calendar.YEAR) + 2).toList() }
    val months = remember { (1..12).toList() }
    val daysInMonth = remember(year, month) { daysInMonth(year, month) }
    val days = remember(daysInMonth) { (1..daysInMonth).toList() }
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }

    LaunchedEffect(daysInMonth) {
        if (day > daysInMonth) day = daysInMonth
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = XhsTextSecondary)
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = XhsTextPrimary,
                )
                TextButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month - 1)
                            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, daysInMonth))
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onConfirm(formatActivityDateTimeForApi(calendar))
                    },
                ) {
                    Text("确定", color = XhsRed, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WheelColumn(
                    label = "年",
                    items = years.map { "${it}年" },
                    selectedIndex = years.indexOf(year).coerceAtLeast(0),
                    onIndexSelected = { year = years[it] },
                    modifier = Modifier.weight(1.2f),
                )
                WheelColumn(
                    label = "月",
                    items = months.map { "${it}月" },
                    selectedIndex = month - 1,
                    onIndexSelected = { month = months[it] },
                    modifier = Modifier.weight(1f),
                )
                WheelColumn(
                    label = "日",
                    items = days.map { "${it}日" },
                    selectedIndex = (day - 1).coerceIn(0, (days.size - 1).coerceAtLeast(0)),
                    onIndexSelected = { day = days[it] },
                    modifier = Modifier.weight(1f),
                )
                WheelColumn(
                    label = "时",
                    items = hours.map { it.toString().padStart(2, '0') },
                    selectedIndex = hour,
                    onIndexSelected = { hour = hours[it] },
                    modifier = Modifier.weight(1f),
                )
                WheelColumn(
                    label = "分",
                    items = minutes.map { it.toString().padStart(2, '0') },
                    selectedIndex = minute,
                    onIndexSelected = { minute = minutes[it] },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WheelColumn(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val itemHeight = 44.dp
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)

    LaunchedEffect(selectedIndex, items.size) {
        val target = selectedIndex.coerceIn(0, items.lastIndex)
        if (listState.firstVisibleItemIndex != target) {
            listState.animateScrollToItem(target)
        }
    }

    LaunchedEffect(listState, items.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@snapshotFlow null
            val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2) - center)
            }?.index
        }.collect { index ->
            if (index != null && index in items.indices && index != selectedIndex) {
                onIndexSelected(index)
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = XhsTextSecondary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 5),
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(listState),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = itemHeight * 2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(items) { index, item ->
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item,
                            fontSize = if (index == safeIndex) 18.sp else 16.sp,
                            fontWeight = if (index == safeIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == safeIndex) XhsTextPrimary else XhsTextSecondary,
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -itemHeight / 2),
                color = XhsTextSecondary.copy(alpha = 0.25f),
            )
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = itemHeight / 2),
                color = XhsTextSecondary.copy(alpha = 0.25f),
            )
        }
    }
}

private fun daysInMonth(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}
