package app.salat.mobile

import android.text.format.DateFormat
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.util.Date

class NextPrayerComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        buildComplication(type, "Dhuhr", "13:08")

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val next = WearTimelineStore(this).load()?.next() ?: return NoDataComplicationData()
        return buildComplication(
            request.complicationType,
            next.displayName(),
            DateFormat.getTimeFormat(this).format(Date(next.atMillis))
        ) ?: NoDataComplicationData()
    }

    private fun buildComplication(
        type: ComplicationType,
        prayerName: String,
        prayerTime: String
    ): ComplicationData? {
        val description = PlainComplicationText.Builder("$prayerName $prayerTime").build()
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(prayerTime).build(),
                contentDescription = description
            ).setTitle(PlainComplicationText.Builder(prayerName).build()).build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("$prayerName · $prayerTime").build(),
                contentDescription = description
            ).build()

            else -> null
        }
    }
}
