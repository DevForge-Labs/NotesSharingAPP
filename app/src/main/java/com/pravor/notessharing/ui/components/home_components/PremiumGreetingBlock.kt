package com.pravor.notessharing.ui.components.home_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

sealed interface SmartBannerState {
    object GreetingMode : SmartBannerState
    // Future extensibility:
    // data class ExamCampaign(val title: String, val subtitle: String, val accentColor: Color) : SmartBannerState
}

@Composable
fun SmartBannerSlot(
    modifier: Modifier = Modifier,
    state: SmartBannerState = SmartBannerState.GreetingMode
) {
    when (state) {
        is SmartBannerState.GreetingMode -> {
            PremiumGreetingBlock(modifier = modifier)
        }
    }
}

@Composable
private fun PremiumGreetingBlock(
    modifier: Modifier = Modifier
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    
    val baseGreeting = when {
        hour in 5..11 -> "Good Morning"
        hour in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val displayName = remember(currentUser) {
        currentUser?.displayName?.trim()?.takeIf { it.isNotEmpty() }
    }

    val greeting = if (displayName != null) {
        "$baseGreeting, $displayName"
    } else {
        baseGreeting
    }

    val subtitle = remember { curatedOneLiners.random() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                letterSpacing = 0.15.sp,
                lineHeight = 28.sp
            ),
            color = Color(0xFFF5F7FA)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                letterSpacing = 0.25.sp,
                lineHeight = 20.sp
            ),
            color = Color(0xFF94A3B8)
        )
    }
}

private val curatedOneLiners = listOf(
    "Steady progress builds confidence",
    "A focused session goes a long way",
    "Small progress adds up over time",
    "Consistency makes preparation easier",
    "A calm session can be highly productive",
    "Good preparation reduces pressure",
    "Today's effort supports tomorrow's success",
    "Focus often beats intensity",
    "A little revision goes a long way",
    "Steady preparation brings confidence",
    "A focused hour can make a difference",
    "Small efforts build strong results",
    "Progress grows through consistency",
    "Learning works best with patience",
    "Preparation becomes easier with rhythm",
    "Consistency supports better outcomes",
    "A little focus goes a long way",
    "Good habits simplify preparation",
    "Careful revision strengthens understanding",
    "Small improvements compound over time",
    "Learning rewards consistency",
    "Preparation works best when steady",
    "Progress is built one session at a time",
    "A thoughtful review strengthens memory",
    "Focused effort builds confidence"
)
