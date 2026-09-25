package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedLocationEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.LocationItem
import com.example.data.model.TerrainCategory
import com.example.data.repository.LocationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationBottomSheet(
    sheetState: SheetState,
    currentLocation: LocationItem?,
    searchQuery: String,
    isSearching: Boolean,
    searchResults: List<LocationItem>,
    savedLocations: List<SavedLocationEntity>,
    recentSearches: List<SearchHistoryEntity>,
    onQueryChanged: (String) -> Unit,
    onLocationSelected: (LocationItem) -> Unit,
    onRequestGps: () -> Unit,
    onSaveCurrentAsSlot: (String) -> Unit,
    onRemoveSlot: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131A2A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                placeholder = { Text("Search city, town, or region...", color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD54F),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFFD54F),
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("location_search_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Current GPS Location Action Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF81D4FA).copy(alpha = 0.12f))
                    .clickable {
                        onRequestGps()
                        onDismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Current GPS Location",
                    tint = Color(0xFF81D4FA),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Use Current Location",
                        color = Color(0xFF81D4FA),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Auto-detect with GPS & network",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // When user types a search query, show results
            if (searchQuery.trim().length >= 2) {
                Text(
                    text = "SEARCH RESULTS",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    if (searchResults.isEmpty() && !isSearching) {
                        item {
                            Text(
                                text = "No places found matching \"$searchQuery\"",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(searchResults) { loc ->
                            LocationResultItem(
                                location = loc,
                                onSelect = {
                                    onLocationSelected(loc)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            } else {
                // SAVED LOCATIONS: Home & Work slots
                Text(
                    text = "SAVED PLACES (HOME & WORK)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val homeLoc = savedLocations.find { it.slot == "HOME" }
                    val workLoc = savedLocations.find { it.slot == "WORK" }

                    SavedSlotCard(
                        slotName = "Home",
                        icon = Icons.Default.Home,
                        savedEntity = homeLoc,
                        currentLocation = currentLocation,
                        onSelect = {
                            if (homeLoc != null) {
                                onLocationSelected(mapEntityToItem(homeLoc))
                                onDismiss()
                            }
                        },
                        onSetCurrent = { onSaveCurrentAsSlot("HOME") },
                        onRemove = { onRemoveSlot("HOME") },
                        modifier = Modifier.weight(1f)
                    )

                    SavedSlotCard(
                        slotName = "Work",
                        icon = Icons.Default.Work,
                        savedEntity = workLoc,
                        currentLocation = currentLocation,
                        onSelect = {
                            if (workLoc != null) {
                                onLocationSelected(mapEntityToItem(workLoc))
                                onDismiss()
                            }
                        },
                        onSetCurrent = { onSaveCurrentAsSlot("WORK") },
                        onRemove = { onRemoveSlot("WORK") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // RECENT SEARCHES (Last 3)
                if (recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT SEARCHES (LAST 3)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                        TextButton(onClick = onClearHistory) {
                            Text(
                                text = "Clear",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        recentSearches.forEach { rec ->
                            val locItem = mapHistoryToItem(rec)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        onLocationSelected(locItem)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = locItem.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (locItem.subtitle.isNotEmpty()) {
                                    Text(
                                        text = ", ${locItem.subtitle}",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // "ELSEWHERE" CURATED DESTINATIONS
                Text(
                    text = "ELSEWHERE",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LocationHelper.CURATED_DESTINATIONS) { curLoc ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    onLocationSelected(curLoc)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = curLoc.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val destinationSubtitle = curLoc.country ?: curLoc.admin1 ?: ""
                                    if (destinationSubtitle.isNotEmpty()) {
                                        Text(
                                            text = destinationSubtitle,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                    val isCurIndia = curLoc.country?.equals("India", ignoreCase = true) == true ||
                                            (curLoc.latitude in 6.0..37.6 && curLoc.longitude in 68.0..97.6)
                                    if (isCurIndia) {
                                        if (destinationSubtitle.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "•",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 10.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "NAQI",
                                            color = Color(0xFFAED581),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationResultItem(location: LocationItem, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (location.subtitle.isNotEmpty()) {
                Text(
                    text = location.subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }

        val isResIndia = (location.country?.equals("India", ignoreCase = true) == true) ||
                (location.country?.equals("IN", ignoreCase = true) == true) ||
                (location.latitude in 6.0..37.6 && location.longitude in 68.0..97.6)

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isResIndia) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF81C784).copy(alpha = 0.2f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "NAQI",
                        color = Color(0xFFAED581),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedSlotCard(
    slotName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    savedEntity: SavedLocationEntity?,
    currentLocation: LocationItem?,
    onSelect: () -> Unit,
    onSetCurrent: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable {
                if (savedEntity != null) onSelect() else onSetCurrent()
            }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = slotName,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = slotName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (savedEntity != null) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Saved",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Empty",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (savedEntity != null) {
                Text(
                    text = savedEntity.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = savedEntity.country ?: "",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Text(
                        text = "Clear",
                        color = Color(0xFFEF5350),
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { onRemove() }
                    )
                }
            } else {
                Text(
                    text = "Tap to set as $slotName",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun mapEntityToItem(e: SavedLocationEntity): LocationItem {
    val terrain = try {
        TerrainCategory.valueOf(e.terrainCategory)
    } catch (ex: Exception) {
        TerrainCategory.CITY_SKYLINE
    }
    return LocationItem(
        name = e.name,
        admin1 = e.admin1,
        country = e.country,
        latitude = e.latitude,
        longitude = e.longitude,
        terrainCategory = terrain
    )
}

private fun mapHistoryToItem(e: SearchHistoryEntity): LocationItem {
    val terrain = try {
        TerrainCategory.valueOf(e.terrainCategory)
    } catch (ex: Exception) {
        TerrainCategory.CITY_SKYLINE
    }
    return LocationItem(
        id = e.id,
        name = e.name,
        admin1 = e.admin1,
        country = e.country,
        latitude = e.latitude,
        longitude = e.longitude,
        terrainCategory = terrain
    )
}
