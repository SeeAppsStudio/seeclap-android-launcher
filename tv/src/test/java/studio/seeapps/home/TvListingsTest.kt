package studio.seeapps.home

import android.media.tv.TvContract.PreviewPrograms
import org.junit.Assert.*
import org.junit.Test

class TvListingsTest {
    private val link = "seeclap://open#playlistId=a&type=movie&id=f%26g&skipReloadPlaylist=1"
    private val art = "https://image.tmdb.org/t/p/w1280/film.jpg"
    private val movie = TvListing("Film", PreviewPrograms.TYPE_MOVIE, link, art, 7_200_000, 1_800_000)
    private val youtube = "com.google.android.youtube.tv"

    @Test fun postersSquaresAndWideImagesShareOneHeight() {
        fun width(ratio: Int?) = shelfCardWidth(360, movie.copy(aspectCode = ratio).toProgram("another.video.app")!!.artAspect!!)
        assertEquals(240, width(null))
        assertEquals(240, width(5))
        assertEquals(240, width(PreviewPrograms.ASPECT_RATIO_2_3))
        assertEquals(360, width(PreviewPrograms.ASPECT_RATIO_1_1))
        assertEquals(480, width(PreviewPrograms.ASPECT_RATIO_4_3))
        assertEquals(540, width(PreviewPrograms.ASPECT_RATIO_3_2))
        assertEquals(640, width(PreviewPrograms.ASPECT_RATIO_16_9))
    }

    @Test fun youtubeVideoWithSquareArtStillUsesAWideCard() {
        val clip = movie.copy(type = PreviewPrograms.TYPE_CLIP, aspectCode = PreviewPrograms.ASPECT_RATIO_1_1)
        assertEquals(16f / 9, clip.toProgram(youtube)!!.artAspect!!, 0f)
    }

    @Test fun seeClapMovieAndSeriesWithOnlyABackdropUsePosterFrames() {
        for (type in listOf(PreviewPrograms.TYPE_MOVIE, PreviewPrograms.TYPE_TV_SERIES, PreviewPrograms.TYPE_TV_EPISODE)) {
            val row = movie.copy(type = type, aspectCode = PreviewPrograms.ASPECT_RATIO_16_9)
            assertEquals(2f / 3, row.toProgram()!!.artAspect!!, 0f)
        }
    }

    @Test fun mixedCardsKeepTheirFullWidthWhenMovingInEitherDirection() {

        assertEquals(0, shelfScroll(0, 1032, 1392, 84, 1920))
        val end = shelfScroll(0, 1426, 2066, 84, 1920)
        assertEquals(230, end)
        assertEquals(end, shelfScroll(end, 358, 998, 84, 1920))
        assertEquals(0, shelfScroll(end, 84, 324, 84, 1920))
    }

    @Test fun glassCoverKeepsTheTitleLegibleWithProviderReleaseTags() {
        assertEquals("Les Soprano", shelfCoverTitle("|FR| Les Soprano (The Sopranos) 1999 FHD MULTI"))
        assertEquals("Le Croque-mitaine", shelfCoverTitle("|FR| Le Croque-mitaine (The Boogeyman) 2023 FHD MULTI"))
        assertEquals("1917", shelfCoverTitle("1917 4K"))
        assertEquals("Brazil (Director’s Cut)", shelfCoverTitle("Brazil (Director’s Cut)"))
        assertEquals("Une histoire en HD et en couleurs", shelfCoverTitle("Une histoire en HD et en couleurs"))
    }

    @Test fun usePublishedCardsWhenGoogleTvShowsNoneOfTheAppsChannels() {
        val uploads = TvChannel(1, "Vidéos", false)
        val library = TvChannel(2, "Bibliothèque", false)
        assertEquals(listOf(uploads, library), TvListings.channelsForFavorite(listOf(uploads, library)))
        val visible = TvChannel(3, "Abonnements", true)
        assertEquals(listOf(visible), TvListings.channelsForFavorite(listOf(uploads, visible)))
    }

    @Test fun playCopyWithIncompatibleDirectSharingUsesThePublicTvProvider() {
        assertEquals(ProgramSource(SeeClap.PKG, true), Programs.source(SeeClap.PKG, SeeClap.PKG, 34, true) { true })
        assertNull(Programs.source(SeeClap.PKG, SeeClap.PKG, 34, true) { false })
        assertNull(Programs.source(SeeClap.PKG, "other.app", 34, true) { true })
    }

    @Test fun longMovieSectionLeavesRoomForShowsAndLiveInPublicationOrder() {
        val base = movie.toProgram()!!
        fun group(name: String, count: Int) = (1..count).map { base.copy(section = name, link = "$name/$it") }
        val shows = group("Séries", 2)
        val films = group("Films", 11)
        val live = group("Direct", 1)
        assertEquals(shows + films.take(5) + live, TvListings.shelfSections(shows + films + live))
        assertEquals(films.take(8), TvListings.shelfSections(films))
        assertTrue(TvListings.shelfSections(emptyList()).isEmpty())
    }

    @Test fun anotherFavoriteReadsItsOwnPublishedPrograms() {
        assertEquals(ProgramSource(youtube, true), Programs.source(youtube, null, 26) { true })
        assertNull(Programs.source(youtube, null, 25) { true })
    }

    @Test fun aPublishedVideoKeepsItsActionArtworkAndChannelName() {
        val target = "https://www.youtube.com/watch?v=demo"
        val thumb = "https://i.ytimg.com/vi/demo/hqdefault.jpg"
        val program = movie.copy(type = PreviewPrograms.TYPE_CLIP, link = target, art = thumb,
            duration = 3_600_000, position = 900_000).toProgram(youtube, "Abonnements")!!
        assertEquals(target, program.link)
        assertEquals(thumb, program.image)
        assertEquals(16f / 9, program.artAspect!!, 0f)
        assertEquals("Abonnements", program.section)
        assertEquals(45, program.remainingMin)
    }

    @Test fun existingDirectProviderAlwaysOwnsTheShelf() {
        for (sdk in listOf(24, 26, 36)) {
            val source = Programs.source(SeeClap.PKG, SeeClap.PKG, sdk) { error("Lecture du système inattendue") }
            assertEquals(ProgramSource(SeeClap.PKG, false), source)
        }
    }

    @Test fun legacyListingsRequireThePublishingEditionAndAnAvailableTvProvider() {
        assertEquals(ProgramSource(SeeClap.PKG, true), Programs.source(SeeClap.PKG, null, 26) { true })
        assertNull(Programs.source(SeeClap.PKG, null, 25) { true })
        assertNull(Programs.source(SeeClap.PKG, null, 26) { false })
        assertNull(Programs.source(SeeClap.PKG_AMAZON, null, 32) { true })
        assertNull(Programs.source(SeeClap.PKG, "other.app", 32) { true })
    }

    @Test fun legacyMovieKeepsThePublishedDestinationAndPosition() {
        val program = movie.toProgram()!!
        assertEquals(link, program.link)
        assertEquals(art, program.image)
        assertEquals(0.25f, program.progress, 0f)
        assertEquals(90, program.remainingMin)
        assertNull(program.restartLink)
        assertTrue(program.opensDetails)
    }

    @Test fun oldReleasesUsedOneHourToEncodeAPercentage() {
        val program = movie.copy(duration = 3_600_000, position = 900_000).toProgram()!!
        assertEquals(0.25f, program.progress, 0f)
        assertEquals(0, program.remainingMin)
    }

    @Test fun movieWithoutTimingOrUsableArtworkStillHasATitle() {
        val program = movie.copy(duration = 0, position = 0, art = "https://provider.example/film.jpg").toProgram()!!
        assertNull(program.image)
        assertEquals("Film", program.title)
        assertEquals(0, program.remainingMin)
        assertEquals(0f, program.progress, 0f)
    }

    @Test fun categoryAndExternalDestinationsDoNotBecomeResumeCards() {
        assertNull(movie.copy(type = PreviewPrograms.TYPE_CLIP).toProgram())
        assertNull(movie.copy(link = "https://example.org/").toProgram())
        assertNull(movie.copy(title = " ").toProgram())
    }
}
