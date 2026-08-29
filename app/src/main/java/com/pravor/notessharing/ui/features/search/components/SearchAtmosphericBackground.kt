package com.pravor.notessharing.ui.features.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Animated background illustration for the Search screen using App_animations/search_back.json.
 * Positioned behind search results and inputs with subtle opacity to match app aesthetic.
 */
@Composable
fun SearchAtmosphericBackground(
    modifier: Modifier = Modifier
) {
    val bgLottieCompositionResult = rememberLottieComposition(
        LottieCompositionSpec.Asset("App_animations/search_back.json")
    )
    val bgLottieComposition = bgLottieCompositionResult.value
    val bgLottieProgress by animateLottieCompositionAsState(
        composition = bgLottieComposition,
        iterations = LottieConstants.IterateForever
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (bgLottieComposition != null) {
            LottieAnimation(
                composition = bgLottieComposition,
                progress = { bgLottieProgress },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = 50.dp)
                    .alpha(0.30f)
            )
        }
    }
}
