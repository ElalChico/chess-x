package com.chessx.data

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeSettingsTest {
    @Test
    fun lichessThemeUsesBrownPalette() {
        assertEquals("Lichess", BoardTheme.LICHESS.labelEn)
        assertEquals(Color(0xFFF0D9B5), BoardTheme.LICHESS.lightSquare)
        assertEquals(Color(0xFFB58863), BoardTheme.LICHESS.darkSquare)
    }

    @Test
    fun pieceSetDefaultsToLichess() {
        assertEquals(PieceSet.LICHESS, PieceSet.DEFAULT)
    }

    @Test
    fun soundPackDefaultsToLichess() {
        assertEquals(SoundPack.LICHESS, SoundPack.DEFAULT)
    }
}
