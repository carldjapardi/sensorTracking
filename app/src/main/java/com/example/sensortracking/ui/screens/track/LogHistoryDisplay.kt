package com.example.sensortracking.ui.screens.track

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensortracking.data.PathSegment
import com.example.sensortracking.data.TurnDirection
import com.example.sensortracking.util.math.toFixed

@Composable
fun LogHistoryDisplay(
    segments: List<PathSegment>,
    onSegmentUpdate: (Int, PathSegment) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Log History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (expanded) {
                LazyColumn(
                    modifier = Modifier.height(200.dp).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(segments) { index, segment ->
                        LogEntry(
                            segment = segment,
                            index = index,
                            onEdit = { newSegment -> onSegmentUpdate(index, newSegment) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun LogEntry(
    segment: PathSegment,
    index: Int,
    onEdit: (PathSegment) -> Unit
) {
    val text = when (segment) {
        is PathSegment.Straight -> {
            "straight ${segment.headingRange.start.toInt()}-${segment.headingRange.endInclusive.toInt()}° for ${segment.distance.toFixed(1)}m in ${segment.steps} steps"
        }
        is PathSegment.Turn -> {
            val direction = if (segment.direction == TurnDirection.RIGHT) "right" else "left"
            "$direction turn ${segment.angle.toFixed(1)}° in ${segment.steps} steps"
        }
    }
    
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().clickable { onEdit(segment) }.padding(vertical = 2.dp),
        style = MaterialTheme.typography.bodyMedium
    )
}