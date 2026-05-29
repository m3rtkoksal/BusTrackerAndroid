# Android Google Sign-In — "client application are blocked"

Bu hata **Firebase SHA eksikliği değil**; Google Cloud **API anahtarı** Android kısıtlaması uyuşmuyor (`API_KEY_ANDROID_APP_BLOCKED`).

Firebase tarafında **upload/debug** SHA-1 kayıtlı (Play Store sürümü için **üçüncü** SHA gerekir):
- Debug: `58:3E:A6:51:DD:A3:E3:57:C2:65:7A:50:00:DC:CF:7A:E0:B4:2C:2E`
- Release / upload key (`Untitled.jks` / bustracker): `B7:0C:C4:D2:64:27:04:EF:C3:46:02:BF:5A:32:F3:C0:41:90:EB:F3`
- **Play App Signing** (Play Store’dan indirilen APK): `C6:41:DC:75:60:F5:3B:CA:7E:CF:B9:91:F7:D6:67:35:0E:E8:79:F9` — Firebase’e eklendi (2026-05-30)

## Play Store’dan indirince kod 10 (en sık)

Play, yüklediğiniz AAB’yi **kendi imza anahtarıyla** yeniden imzalar. Bu SHA-1, `Untitled.jks` veya debug ile **aynı değildir**.

1. [Play Console](https://play.google.com/console) → **Shuttle Live** → **Setup** → **App integrity**
2. **App signing** sekmesi → **App signing key certificate** → **SHA-1 certificate fingerprint** kopyala
3. [Firebase](https://console.firebase.google.com/project/bustracker-717a3/settings/general) → Android uygulama → **Add fingerprint** → yapıştır → Kaydet
4. (İsteğe bağlı) [Google Cloud Credentials](https://console.cloud.google.com/apis/credentials?project=bustracker-717a3) → Android API key → Application restrictions’a **aynı SHA-1** + paket adı ekle
5. **5–15 dakika** bekle → uygulamayı kapat/aç → Google ile giriş tekrar dene (**yeni APK gerekmez**)

Upload key SHA-1’i Play’e yüklerken kullanılır; kullanıcının telefonundaki imza **App signing key** SHA-1’idir.

## Sizin yapmanız gereken (2 dk)

1. Açın: https://console.cloud.google.com/apis/credentials?project=bustracker-717a3  
2. **Android key (auto created by Firebase)** satırına tıklayın (API key: `AIzaSyD9f9DmuYse87gmBTwCARM4jFgZULZE8eo`).  
3. **Application restrictions** → **Android apps** seçili olsun.  
4. **Add an item** ile **iki kayıt** ekleyin:

| Package name | SHA-1 |
|--------------|--------|
| `com.mikatechnology.BusTracker` | `B7:0C:C4:D2:64:27:04:EF:C3:46:02:BF:5A:32:F3:C0:41:90:EB:F3` |
| `com.mikatechnology.BusTracker` | `58:3E:A6:51:DD:A3:E3:57:C2:65:7A:50:00:DC:CF:7A:E0:B4:2C:2E` |

5. **API restrictions** bölümünde Firebase’in önerdiği API’ler kalsın; en azından **Identity Toolkit API** listede olmalı.  
6. **Save** → 5 dakika bekleyin.  
7. Yeni **release APK** üretip App Distribution’a yükleyin.

### Hızlı test (geçici) — hâlâ blocked ise

1. **Application restrictions → None** → kayıt dene. Çalışırsa SHA satırlarında yazım/package hatası vardır; Firebase’den SHA’yı kopyala-yapıştır.
2. Hâlâ blocked ise **API restrictions → Don’t restrict key** (geçici) → kayıt dene.
3. Çalışınca tekrar kısıtla:
   - Application: Android apps + iki SHA-1
   - API restrictions: en az **Identity Toolkit API**, **Token Service API**, **Firebase Installations API**

### Google Sign-In kodu

Uygulama doğru **Web client ID** kullanıyor (`default_web_client_id` = `851737532018-crjnv0vcpacu575...`). Sorun genelde Google hesap seçicisinde değil, hemen sonra **Firebase Auth** çağrısında olur.

### APK imzasını doğrula (release)

```bash
/Applications/Android\ Studio.app/Contents/jbr/Contents/Home/bin/keytool -printcert -jarfile app/release/app-release.apk | grep SHA1
```

Çıkan SHA1, Cloud’daki **B7:0C:C4:D2...** ile aynı olmalı.

## Firebase MCP ile yapılanlar

- Proje: `bustracker-717a3`  
- Release SHA-1 Firebase Android uygulamasına eklendi  
- `app/google-services.json` güncel (debug + release OAuth client)
