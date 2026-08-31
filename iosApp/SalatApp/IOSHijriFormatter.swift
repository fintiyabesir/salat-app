import Foundation

struct IOSHijriFormatter {
    static func format(
        date: Date,
        timeZone: TimeZone,
        method: String,
        dayAdjustment: Int,
        /// The calendar header wants a month alone, not a full date.
        template: String = "d MMMM y"
    ) -> String {
        var gregorian = Calendar(identifier: .gregorian)
        gregorian.timeZone = timeZone
        let adjusted = gregorian.date(
            byAdding: .day,
            value: min(2, max(-2, dayAdjustment)),
            to: date
        ) ?? date

        let identifier: Calendar.Identifier
        switch method {
        case "TABULAR":
            identifier = .islamicTabular
        case "UMM_AL_QURA", "AUTOMATIC":
            identifier = .islamicUmmAlQura
        default:
            identifier = .islamicUmmAlQura
        }

        var hijri = Calendar(identifier: identifier)
        hijri.timeZone = timeZone
        hijri.locale = L10n.selectedLocale

        let formatter = DateFormatter()
        formatter.calendar = hijri
        formatter.timeZone = timeZone
        formatter.locale = L10n.selectedLocale
        formatter.dateFormat = template
        return formatter.string(from: adjusted)
    }
}
