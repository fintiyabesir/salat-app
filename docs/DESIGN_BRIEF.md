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

Ürün adı İngilizce'de **Awqat**. Diğer sekiz dilde hem uygulama içi ad hem
mağaza adı tamamen yereldir; `Awqat` kelimesi o dillerde hiç geçmez:

| Dil | Mağaza adı | Anlamı |
|---|---|---|
| English | Awqat - Prayer Time | *awqāt* = vakitler |
| Türkçe | Mihrap: Namaz Vakti ve Kıble | mihrap = namaz nişi |
| العربية | قبلتي ومواقيت الصلاة | "kıblem ve namaz vakitleri" |
| বাংলা | মিহরাব: নামাজের সময় | mihrab + namaz vakti |
| Bahasa Melayu | Kiblatku: Waktu Solat Harian | "kıblem, günlük namaz vakti" |
| اردو | محراب: نماز کے اوقات | mihrab + namaz vakitleri |
| 简体中文 | 拜功时间与朝向指南 | namaz vakti ve yön rehberi |
| 繁體中文 | 拜功時間與朝向指南 | aynısı, geleneksel |

Jenerik adlar (`Namaz Vakitleri`, `Waktu Solat`) App Store'da dolu olduğu için her
dilde ayırt edici bir kelime seçildi — çoğunda *mihrap/kıble*. **Tasarımın tek bir
Latin wordmark'a yaslanamayacağı anlamına geliyor:** görsel kimlik isimden değil,
ikondan ve renk/biçim dilinden gelmeli.

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

**iOS (iPhone 17, Türkçe):**
- `ios-today-light.png`, `ios-today-dark.png` — Bugün
- `ios-calendar-light.png` — Takvim
- `ios-qibla-light.png` — Kıble

**Android (Pixel sınıfı, İngilizce):**
- `and-today-light.png`, `and-today-dark.png` — Bugün
- `and-calendar-dark.png` — Takvim
- `and-qibla-dark.png` — Kıble
- `and-settings-dark.png` — Ayarlar
- `and-widget-light.png`, `and-widget-dark.png` — ana ekran widget'ı

**Apple Watch:**
- `watch-empty.png` — telefonla henüz eşzamanlanmamış durum

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

## Bilinen zayıf noktalar

Tasarımın çözmesini beklediğimiz, koddan gördüğümüz somut sorunlar:

- **Kıble ekranı neredeyse boş.** Bir daire ve tek bir üçgen; pusula gülü, yön
  harfleri, Kâbe referansı, mesafe bilgisi yok. Ekranın yarısı kullanılmıyor.
- **Uygulama ikonu yer tutucu.** Python ile çizilmiş; mağaza rekabetinde
  ayırt edici değil. Jenerik isimler yüzünden ikon, markanın taşıyıcısı olacak.
- **Widget'ın galeri önizlemesi yok.** Seçicide widget yerine uygulama ikonu
  görünüyor; tamamlanmamış duruyor.
- **Şehir seçici görsel olarak kopuk.** Satırlar bağlantı mavisiyle çiziliyor,
  arama kutusu listenin üstüne biniyor, şehir adları arayüz Türkçe olsa bile
  İngilizce ("Istanbul · Turkey").
- **Boş/hata durumları tasarlanmamış.** Konum yok, izin reddedildi, saat
  eşzamanlanmadı gibi durumlar düz metin.
- **Tipografi hiyerarşisi zayıf.** Bugün ekranında vakit listesi ile "sonraki
  vakit" kartı arasındaki fark yalnızca boyut; ritim ve ağırlık çalışılmamış.

## Claude Design'a verilecek prompt

Aşağıdaki metin doğrudan yapıştırılabilir. Ekran görüntülerini de birlikte ver.

---

Awqat adlı bir namaz vakitleri uygulamasının görsel tasarımını yapmanı istiyorum.
Uygulama çalışır durumda; sana ekli görüntüler simülatörden alınmış **gerçek mevcut
hali**. Sıfırdan bir ürün değil, var olanın görsel dilini kurman gerekiyor.

Ürün: namaz vakitleri, kıble ve takvim. Ücretsiz, reklamsız, hesapsız, tamamen
çevrimdışı çalışıyor. iPhone, iPad, Apple Watch, Android telefon/tablet ve Wear OS.

Üç sert kısıt var:

1. **Dokuz dil aynı anda**: İngilizce, Türkçe, Arapça, Farsça, Urduca, Bengalce,
   Malayca, Basitleştirilmiş ve Geleneksel Çince. Arapça, Farsça ve Urduca sağdan
   sola; her düzen ayna simetrik çalışmalı. Bu üç dilde **harf aralığı
   kullanılamaz**, kelimeyi kopartır. Metin uzunlukları diller arasında iki katına
   çıkabiliyor, sabit genişlikli buton ve rozet tasarlama.
2. **Tek bir Latin wordmark'a yaslanamazsın.** Uygulamanın adı her dilde farklı ve
   çoğu yerel (Mihrap, قبلتي, Kiblatku, 拜功…). Görsel kimlik isimden değil ikondan,
   renkten ve biçim dilinden gelmeli.
3. **SwiftUI ve Jetpack Compose ile uygulanabilir olmalı.** Ağır özel çizim, büyük
   görsel varlık ve özel animasyon istemiyoruz; uygulama boyutu küçük kalmalı.

Ton: sakin. Bu uygulama günde beş kez, çoğu zaman aceleyle açılıyor; kullanıcının
aradığı tek şey bir saat ve kalan süre. Gösterişli olmasın, dini sembolleri
dekorasyon olarak kullanmasın, hiçbir yerde bir şey satmasın. Küresel bir kitleye
hitap ediyor, tek bir ülkenin görsel diline yaslanmasın.

Mevcut renkler (değiştirebilirsin, gerekçesini yaz):
zemin `#FAF8F3` / `#171916`, yüzey `#FFFFFF` / `#22251F`,
vurgu `#467A69` / `#91C9B5`, metin `#20221F` / `#F2F1EC`.
Şu an sistem yazı tipi kullanılıyor. Özel yazı tipi önerirsen Latin, Arap, Bengal
ve Han yazı sistemlerinin dördünü birden kapsamalı.

Öncelik sırasıyla şunları istiyorum:

1. **Kıble ekranı.** Şu an en zayıfı: bir daire ve tek bir üçgen, ekranın yarısı
   boş. Pusula gülü, yön harfleri, Kâbe'ye mesafe, kalibrasyon durumu gibi
   unsurlarla yeniden kurgula.
2. **Uygulama ikonu.** Apple 1024×1024 ve Android uyarlanabilir ikon (ön plan +
   arka plan katmanı ayrı, 108×108dp güvenli alan). Bildirim için tek renkli
   siluet de gerekiyor. Jenerik isimler yüzünden ikon markanın taşıyıcısı.
3. **Bugün ekranı.** Tipografi hiyerarşisi ve ritim; "sonraki vakit" kartı ile
   liste arasındaki fark şu an sadece boyut.
4. **Takvim ve Ayarlar.** Aydınlık ve karanlık.
5. **Widget'lar** (iOS ana ekran + kilit ekranı, Android ana ekran) ve
   **Apple Watch / Wear OS** yüzeyleri. Widget'lar sistem tarafından çizilir,
   kısıtlı bir alt küme kullanılabilir.
6. **Boş ve hata durumları**: konum yok, izin reddedildi, saat eşzamanlanmadı.

Her ekranı aydınlık ve karanlık modda ver. Arapça için en az bir ekranın RTL
halini göster — aynanın gerçekten çalıştığını görmek istiyorum.
