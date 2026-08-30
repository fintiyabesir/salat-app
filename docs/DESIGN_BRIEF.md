# Awqat — tasarım brief'i

Bu belge tasarım tarafına verilmek üzere hazırlandı. Uygulamanın **mevcut** hali
ekran görüntüleriyle birlikte tarif edilir, sonra istenen iş listelenir.

## Ürün

Namaz vakitleri, kıble ve takvim. Telefon, tablet ve akıllı saat.

- Sonsuza kadar ücretsiz: reklam yok, abonelik yok, ödeme duvarı yok.
- Hesap yok, backend yok. Hesaplama cihazda, çevrimdışı çalışır.
- v1'de analytics ve reklam SDK'sı yok.
- Platformlar: iPhone/iPad (SwiftUI), Apple Watch, Android telefon/tablet
  (Jetpack Compose), Wear OS. Ortak hesaplama katmanı Kotlin Multiplatform.

## Diller — tasarımın en sert kısıtı

Dokuz dil aynı anda destekleniyor: **İngilizce, Türkçe, Arapça, Farsça, Urduca,
Bengalce, Malayca, Basitleştirilmiş Çince, Geleneksel Çince.**

Bunun tasarıma üç doğrudan etkisi var:

1. **Arapça, Farsça ve Urduca sağdan sola.** Her düzen ayna simetrik çalışmalı.
   Bu diller bitişik yazıldığı için **harf aralığı (letter-spacing) kullanılamaz** —
   kelimeyi kopartır. Latin alfabesinde kullanılan geniş aralıklı başlıklar bu üç
   dilde otomatik olarak sıfırlanıyor; tasarım buna dayanmalı.
2. **Metin uzunlukları çok oynuyor.** Aynı etiket İngilizce'de 12, Almanca benzeri
   uzunlukta Türkçe'de 24 karakter olabiliyor. Sabit genişlikli buton ve rozet
   tasarlanmamalı.
3. **Rakamlar dile göre değişebiliyor** (Arapça-Hint rakamları). Saat gösteriminin
   hem `04:53` hem `٠٤:٥٣` ile çalışması gerekiyor.

## Marka durumu

Ürün adı İngilizce'de **Awqat**. Diğer dillerde tamamen yerel adlar kullanılıyor.
App Store'da jenerik namaz-vakti adlarının neredeyse tamamı başka geliştiriciler
tarafından tutulduğu için mağaza adlarının çoğu `Awqat` + yerel açıklama biçiminde.

Mevcut ikon `scripts/generate_app_icon.py` ile üretilen, kod içinde çizilmiş bir
yer tutucu: krem zemin, iki nane yeşili halka, teal elmas çerçeve, altın pusula
iğnesi. **Bu ikon yerine gerçek bir tasarım gerekiyor** — Apple ve Android
(uyarlanabilir ikon: ön plan + arka plan katmanı ayrı) için.

## Mevcut renkler

Kod içinde şu an kullanılanlar:

| Rol | Aydınlık | Karanlık |
|---|---|---|
| Zemin | `#FAF8F3` krem | `#171916` |
| Kart / yüzey | `#FFFFFF` | `#22251F` |
| Vurgu (sage) | `#467A69` | `#91C9B5` |
| Ana metin | `#20221F` | `#F2F1EC` |
| İkincil metin | `#6D716E` | `#AAB0A8` |

Tipografi sistem yazı tipi (SF / Roboto). Özel yazı tipi yok — dokuz dil ve üç
yazı sistemi (Latin, Arap, Bengal, Han) desteklemesi gerektiği için özel yazı tipi
seçimi ciddi bir karar; öneri bekliyoruz.

## Mevcut ekranlar

Ekli görüntüler simülatörden, güncel koddan alındı:

- `01-today-dark.png` — Bugün, karanlık mod
- `02-today-light.png` — Bugün, aydınlık mod
- `03-calendar-light.png` — Takvim
- `04-qibla-light.png` — Kıble
- `05-watch.png` — Apple Watch

### Bugün
Konum başlığı (şehir, bölge, miladi tarih, hicri tarih), "sonraki vakit" kartı
(vakit adı, saat, canlı geri sayım) ve altında altı vaktin listesi. Listedeki bir
vakte dokunmak o vaktin bildirim ayarını açıyor.

### Takvim
Yaklaşan günlerin vakit tablosu.

### Kıble
Derece değeri ve dönen bir ok. **Bu ekran şu an çok boş** — dairenin içinde tek
bir üçgen var, ekranın yarısı kullanılmıyor. En çok tasarım ihtiyacı olan ekran bu.

### Apple Watch
Sıradaki vakit ve saati. Telefondan eşzamanlanan çevrimdışı çizelgeden besleniyor.

## İstenen iş

1. **Uygulama ikonu** — Apple (1024×1024) ve Android uyarlanabilir ikon (ön plan +
   arka plan, 108×108dp güvenli alan kuralına uygun). Bildirim için tek renkli
   siluet versiyonu da gerekiyor.
2. **Ekran tasarımları** — Bugün, Takvim, Kıble, Ayarlar. Aydınlık ve karanlık.
   Kıble ekranı öncelikli.
3. **Apple Watch ve Wear OS** yüzeyleri.
4. **Widget** tasarımları (iOS ana ekran + kilit ekranı, Android ana ekran).
5. **App Store / Play ekran görüntüleri** — İngilizce, Türkçe ve Arapça (RTL)
   setleri; Arapça set aynanın gerçekten çalıştığını göstermeli.
6. **Web sitesi** — tek sayfa yeterli: ne olduğu, ekran görüntüleri, mağaza
   bağlantıları, gizlilik politikası ve destek sayfası. Gizlilik politikası ve
   destek URL'si mağaza gönderimi için zorunlu.

## Ton

Sakin. Bu uygulama günde beş kez, çoğu zaman aceleyle açılıyor; kullanıcının
aradığı tek şey bir saat ve kalan süre. Gösterişli olmamalı, dini sembolleri
dekorasyon olarak kullanmamalı, hiçbir yerde kullanıcıya bir şey satmamalı.
Küresel bir kitleye hitap ediyor — bir ülkenin görsel diline yaslanmamalı.

## Teknik sınırlar

- SwiftUI ve Jetpack Compose ile uygulanabilir olmalı; ağır özel çizim
  istemiyoruz.
- Widget'lar sistem tarafından çizilir; kısıtlı bir alt küme kullanılabilir.
- Uygulama boyutu küçük kalmalı; büyük görsel varlıklardan kaçınılmalı
  (uygulama zaten çevrimdışı şehir kataloğu taşıyor).
- Karanlık mod cihaz temasını takip eder, ayrıca uygulama içinde
  aydınlık/karanlık/sistem seçeneği var. İkisi de tasarlanmalı.
