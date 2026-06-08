package com.mikatechnology.BusTracker.localization

object L10n {
    private fun t(tr: String, en: String): String = LanguageManager.t(tr, en)

    val ok get() = t("Tamam", "OK")
    val back get() = t("Geri", "Back")
    val cancel get() = t("Vazgeç", "Cancel")
    val confirm get() = t("Onayla", "Confirm")
    val loading get() = t("Yükleniyor...", "Loading...")
    val loadingEllipsis get() = t("Yükleniyor…", "Loading…")
    val error get() = t("Hata", "Error")
    val success get() = t("Başarılı", "Success")
    val info get() = t("Bilgi", "Info")
    val service get() = t("Servis", "Shuttle")
    val driver get() = t("Sürücü", "Driver")
    val driverDefaultName get() = t("Şoför", "Driver")
    val settingsLanguage get() = t("Dil", "Language")
    val attendanceComing get() = t("Gelecek", "Coming")
    val attendanceNotComing get() = t("Gelmeyecek", "Not coming")
    val attendanceUnknown get() = t("Belirtmedi", "No response")
    val attendanceComingSelf get() = t("Geliyorum", "I'm coming")
    val attendanceNotComingSelf get() = t("Gelmiyorum", "I'm not coming")
    val attendanceUncertain get() = t("Belirsiz", "Undecided")
    val attendanceBoarded get() = t("Bindi", "On board")
    val attendanceBoardedSelf get() = t("Servise bindim", "On the shuttle")
    fun passengerBoardedNotification(name: String) = t("$name servise bindi", "$name boarded the shuttle")
    fun serviceNotComingListTitle(serviceRelativeName: String) =
        t("$serviceRelativeName Gelmeyenler", "$serviceRelativeName — Not coming")
    val serviceNotComingListEmpty get() = t("Henüz kimse gelmiyorum seçmedi.", "No one has marked not coming yet.")
    val roleDriver get() = t("Sürücü", "Driver")
    val rolePassenger get() = t("Yolcu", "Passenger")
    val tabPassengers get() = t("Yolcular", "Passengers")
    val tabMap get() = t("Harita", "Map")
    val tabSettings get() = t("Ayarlar", "Settings")
    val tabService get() = t("Servis", "Shuttle")
    val active get() = t("AKTİF", "ACTIVE")
    val live get() = t("CANLI", "LIVE")
    val serviceNameLabel get() = t("SERVİS ADI", "SHUTTLE NAME")
    val serviceCodeLabel get() = t("SERVİS KODU", "SHUTTLE CODE")
    val waitingForPassengers get() = t("YOLCU BEKLENİYOR", "WAITING FOR PASSENGERS")
    val passengerList get() = t("YOLCU LİSTESİ", "PASSENGER LIST")
    val statComing get() = t("GELECEK", "COMING")
    val statNotComing get() = t("GELMEYECEK", "NOT COMING")
    val statUnknown get() = t("BELİRTMEDİ", "NO RESPONSE")
    val preparing get() = t("Hazırlanıyor", "Preparing")
    val share get() = t("Paylaş", "Share")
    val copy get() = t("Kopyala", "Copy")
    val stopShuttle get() = t("SERVİSİ DURDUR", "STOP SHUTTLE")
    val startShuttle get() = t("SERVİSİ BAŞLAT", "START SHUTTLE")
    val sharingLocation get() = t("Konum paylaşılıyor", "Sharing location")
    val selectDurationOnStart get() = t("Başlatınca süre seçilir", "Duration is selected when starting")
    val enableAlwaysLocation get() = t("Her zaman iznini aç", "Enable always location")
    val waitingForDriverLocation get() = t("Sürücü konumu bekleniyor", "Waiting for driver location")
    val shuttleNotStarted get() = t("Servis henüz başlamadı", "Shuttle has not started yet")
    val shuttleActive get() = t("Servis aktif", "Shuttle active")
    val waitingForLocation get() = t("Konum bekleniyor", "Waiting for location")
    val shuttleInactive get() = t("Servis pasif", "Shuttle inactive")
    val attendanceTodayQuestion get() = t("BUGÜN GELECEK MİSİNİZ?", "ARE YOU COMING TODAY?")
    fun attendanceQuestionForDate(dateLabel: String) =
        t("$dateLabel GELECEK MİSİNİZ?", "ARE YOU COMING ON $dateLabel?")
    val pickupPoint get() = t("BİNİŞ NOKTASI", "PICKUP POINT")
    val noPickupSaved get() = t("Henüz biniş noktası kaydetmediniz.", "You haven't saved a pickup point yet.")
    val comingBlockedWithoutPickup get() = t(
        "Kayıtlı biniş noktanız olmadığı için seçim yapamazsınız.",
        "You can't make a selection because you haven't saved a pickup point."
    )
    val setOnMap get() = t("HARİTADA BELİRLE", "SET ON MAP")
    val editOnMap get() = t("HARİTADA DÜZENLE", "EDIT ON MAP")
    val savePickupPoint get() = t("BİNİŞ NOKTAMI KAYDET", "SAVE MY PICKUP POINT")
    val change get() = t("DEĞİŞTİR", "CHANGE")
    val pickupPinLabel get() = t("Biniş", "Pickup")
    val settingsServiceCode get() = t("Servis Kodu", "Shuttle Code")
    val settingsYourName get() = t("Adınız", "Your Name")
    val settingsShuttle get() = t("Servis", "Shuttle")
    val holidayModeTitle get() = t("Tatil Modu", "Holiday Mode")
    val holidayModeOff get() = t("Kapalı", "Off")
    val holidayModeBadgeActive get() = t("AKTİF", "ON")
    fun holidayModeUntil(date: String) = t("$date tarihine kadar", "Until $date")
    val holidayModeCardDetailOff get() = t(
        "Seyrek kullanıyorsanız açın: her gün işaretlemeden varsayılan gelmiyorum; servise bineceğiniz günlerde Geliyorum seçin.",
        "For occasional use: default is not coming without daily taps; mark Coming only on shuttle days."
    )
    fun holidayModeCardDetailActive(date: String) = t(
        "$date tarihine kadar seçmediğiniz her gün gelmiyorum; bineceğiniz gün Geliyorum yeterli.",
        "Until $date, unselected days are not coming; on shuttle days, tap Coming."
    )
    val holidayModeCalendarHint get() = t(
        "Modun biteceği son günü seçin (ör. 3 ay). Bu sürede her gün Gelmiyorum işaretlemeniz gerekmez; servise bineceğiniz gün uygulamadan Geliyorum seçin, sürücü anında görür.",
        "Pick when this mode ends (e.g. 3 months). Unselected days count as not coming; on days you ride, choose Coming and the driver sees it right away."
    )
    val holidayModeEndDateLabel get() = t("Bitiş tarihi", "End date")
    val holidayModeSave get() = t("Kaydet", "Save")
    val holidayModeEndEarly get() = t("Tatili bitir", "End holiday now")
    val holidayModeSaved get() = t("Tatil modu kaydedildi.", "Holiday mode saved.")
    val holidayModeEnded get() = t("Tatil modu kapatıldı.", "Holiday mode turned off.")
    val nameUpdated get() = t("İsim güncellendi.", "Name updated.")
    val sparseModeSuggestionTitle get() = t(
        "Servisi az kullanıyor musunuz?",
        "Rarely use the shuttle?"
    )
    fun sparseModeSuggestionBody(comingDays: Int) = t(
        "Son 1 ayda servise yalnızca $comingDays kez \"Geliyorum\" seçtiniz. Tatil Modu ile her gün \"Gelmiyorum\" demek zorunda kalmazsınız; geleceğiniz günlerde sadece \"Geliyorum\" yeterli.",
        "In the last month you chose \"I'm coming\" only $comingDays times. With Holiday Mode you don't need to tap \"Not coming\" every day — on days you ride, just tap \"I'm coming\"."
    )
    val sparseModeSheetTitle get() = t("Servisi az kullanıyorsunuz", "You rarely use the shuttle")
    fun sparseModeSheetMessage(comingDays: Int) = t(
        "Son 1 ayda yalnızca $comingDays kez \"Geliyorum\" seçtiniz. İsterseniz Tatil Modu açarak genel durumunuzu gelmiyorum yapabilirsiniz; servise bineceğiniz günlerde sadece \"Geliyorum\" seçmeniz yeterli.",
        "In the last month you chose \"I'm coming\" only $comingDays times. You can turn on Holiday Mode so your default is not coming — on days you ride, just choose \"I'm coming\"."
    )
    val sparseModeSheetOk get() = t("Tamam", "OK")
    val signOut get() = t("Çıkış Yap", "Sign Out")
    val deleteAccount get() = t("Hesabı Sil", "Delete Account")
    val inviteLinkTitle get() = t("DAVET LİNKİ", "INVITE LINK")
    val deleteAccountPermanently get() = t("Hesabı Kalıcı Olarak Sil", "Delete Account Permanently")
    val accountDeletedSuccess get() = t("Hesabınız başarıyla silindi.", "Your account was deleted successfully.")
    val accountDeleteFailed get() = t("Hesap silinirken bir hata oluştu. Lütfen tekrar deneyin.", "Something went wrong while deleting your account. Please try again.")
    val shuttleStopped get() = t("Servis durduruldu.", "Shuttle stopped.")
    val serviceCodeNotFound get() = t("Servis kodu bulunamadı.", "Shuttle code not found.")
    val serviceCodeCopied get() = t("Servis kodu kopyalandı.", "Shuttle code copied.")
    val markPickupOnMap get() = t("Haritada sabah biniş noktanızı işaretleyin.", "Mark your morning pickup point on the map.")
    val shuttleInfoNotFound get() = t("Servis bilgisi bulunamadı.", "Shuttle information not found.")
    val shuttleInfoNotFoundRejoin get() = t(
        "Servis bilgisi bulunamadı. Çıkış yapıp servise yeniden katılın.",
        "Shuttle information not found. Sign out and join the shuttle again."
    )
    val signOutFailed get() = t("Çıkış yapılamadı.", "Could not sign out.")
    val signingOut get() = t("Çıkış yapılıyor...", "Signing out...")
    val deletingAccount get() = t("Hesap siliniyor...", "Deleting account...")
    val googleVerificationRequiredForDelete get() = t(
        "Hesap silmek için Google doğrulaması gerekli.",
        "Google verification is required to delete your account."
    )
    val googleVerificationFailed get() = t(
        "Google doğrulaması tamamlanamadı.",
        "Google verification could not be completed."
    )
    val updateFailed get() = t("Güncellenemedi.", "Could not update.")
    val saveFailed get() = t("Kaydedilemedi.", "Could not save.")
    val shuttleStartFailed get() = t("Servis başlatılamadı.", "Could not start the shuttle.")
    val shuttleStopFailed get() = t("Servis durdurulamadı.", "Could not stop the shuttle.")
    val startingShuttle get() = t("Servis başlatılıyor...", "Starting shuttle...")
    val stoppingShuttle get() = t("Servis durduruluyor...", "Stopping shuttle...")
    val markPickupOnMapShort get() = t("Haritada biniş noktanızı işaretleyin.", "Mark your pickup point on the map.")
    val shuttleNotFoundRejoin get() = t(
        "Servis bulunamadı. Çıkış yapıp tekrar katılın.",
        "Shuttle not found. Sign out and join again."
    )
    val alwaysLocationRequiredToStartTrip get() = t(
        "Servisi başlatmak için Ayarlar'dan \"Her zaman\" konum iznini açmanız gerekir.",
        "To start the shuttle, enable \"Always\" location permission in Settings."
    )
    val loginTitle get() = t("Giriş", "Sign In")
    val signingIn get() = t("Giriş yapılıyor...", "Signing in...")
    val signInAction get() = t("Giriş Yap", "Sign In")
    val signInWithAppleHint get() = t("Apple hesabınızla giriş yapın.", "Sign in with your Apple account.")
    val signInWithApple get() = t("Apple ile Giriş Yap", "Sign in with Apple")
    val noAccount get() = t("Hesabın yok mu?", "Don't have an account?")
    val createAccount get() = t("Hesap oluştur", "Create account")
    val createAccountTitle get() = t("Hesap Oluştur", "Create Account")
    val roleSelectionTitle get() = t("Hesap Oluştur", "Create Account")
    val roleSelectionSubtitle get() = t("Sürücü müsünüz, yolcu mu?", "Are you a driver or a passenger?")
    val iAmDriver get() = t("Sürücüyüm", "I'm a driver")
    val iAmPassenger get() = t("Yolcuyum", "I'm a passenger")
    val alreadyHaveAccount get() = t("Zaten hesabım var —", "Already have an account —")
    val signInWithAppleLink get() = t("Apple ile giriş yap", "sign in with Apple")
    val driverRegistration get() = t("Sürücü Kaydı", "Driver Registration")
    val passengerRegistration get() = t("Yolcu Kaydı", "Passenger Registration")
    val serviceNameField get() = t("Servis adı", "Shuttle name")
    val serviceCodeField get() = t("Servis kodu", "Shuttle code")
    val serviceNameExample get() = t("Örn. Kadıköy Servisi", "e.g. Kadikoy Shuttle")
    val serviceCodeExample get() = t("Sürücünün verdiği 6 haneli kod", "6-digit code from your driver")
    val nameExampleDriver get() = t("Örn. Ahmet", "e.g. Alex")
    val nameExamplePassenger get() = t("Örn. Ayşe", "e.g. Emma")
    val enterNameToRegister get() = t("Kayıt için adınızı girin.", "Enter your name to register.")
    val enterServiceCode get() = t("Servis kodu girmedin.", "Enter a shuttle code.")
    val serviceCodeMinLength get() = t("Servis kodu en az 4 karakter olmalı.", "Shuttle code must be at least 4 characters.")
    val enterServiceName get() = t("Servis adı girmedin.", "Enter a shuttle name.")
    val driverAccountCreated get() = t("Servis hesabınız oluşturuldu.", "Your shuttle account was created.")
    val joinedShuttle get() = t("Servise katıldınız.", "You joined the shuttle.")
    val yourNameField get() = t("Adınız", "Your name")
    val iAmDriverSubtitle get() = t(
        "Servisi oluştururum, sabah \"Servisi Başlat\" derim ve konumumu paylaşırım.",
        "I create the shuttle, tap \"Start Shuttle\" in the morning, and share my location."
    )
    val iAmPassengerSubtitle get() = t(
        "Servise katılırım, haritadan takip ederim ve geleceğimi bildiririm.",
        "I join the shuttle, track on the map, and let the driver know if I'm coming."
    )
    val driverRegistrationSubtitle get() = t(
        "Servisinizi oluşturun, yolcularınız sizi takip etsin.",
        "Create your shuttle and let passengers track you."
    )
    val passengerRegistrationSubtitle get() = t(
        "Servis kodunuzla katılın, haritadan takip edin.",
        "Join with your shuttle code and track on the map."
    )
    val driverLocationFooter get() = t(
        "Konum paylaşımı yalnızca sürücü hesabında açıktır.",
        "Location sharing is only enabled for driver accounts."
    )
    val passengerLocationFooter get() = t(
        "Yolcu hesabında konum paylaşımı yoktur.",
        "Passenger accounts do not share location."
    )
    val googleRegistrationNote get() = t(
        "Kayıt için Google hesabınız kullanılır; telefon numarası istenmez.",
        "Your Google account is used for registration; no phone number is required."
    )
    val registerWithGoogle get() = t("Google ile Kayıt Ol", "Register with Google")
    val signInWithGoogle get() = t("Google ile Giriş Yap", "Sign in with Google")
    val signInWithGoogleHint get() = t("Google hesabınızla giriş yapın.", "Sign in with your Google account.")
    val backToCreateAccount get() = t("Hesap oluşturmaya dön", "Back to create account")
    val profileNotFound get() = t(
        "Bu Google hesabıyla kayıtlı profil bulunamadı. Hesabınız silinmiş olabilir; yeni hesap oluşturabilirsiniz.",
        "No profile found for this Google account. Your account may have been deleted; you can create a new one."
    )
    val creatingAccount get() = t("Hesap oluşturuluyor...", "Creating account...")
    val accountCreateFailed get() = t("Hesap oluşturulamadı.", "Could not create account.")
    val signInFailed get() = t("Giriş yapılamadı.", "Sign-in failed.")
    val googleSignInFailed get() = t("Google ile giriş başarısız.", "Google sign-in failed.")
    val invalidShuttleCode get() = t("Geçersiz servis kodu.", "Invalid shuttle code.")
    val registerWithApple get() = t("Apple ile Kayıt Ol", "Register with Apple")
    val selectAndContinue get() = t("SEÇ VE DEVAM ET", "SELECT AND CONTINUE")
    val backToRoleSelection get() = t("ROL SEÇİMİNE DÖN", "BACK TO ROLE SELECTION")
    val continueWithApple get() = t("Apple ile Devam Et", "Continue with Apple")
    val notifications get() = t("BİLDİRİMLER", "NOTIFICATIONS")
    val notificationsOn get() = t("Açık", "On")
    val notificationsNotRequested get() = t("Henüz izin istenmedi", "Permission not requested yet")
    val notificationsOffSystemSettings get() = t("Kapalı — sistem ayarlarından açın", "Off — enable in system settings")
    val notificationsOnTemporary get() = t("Açık (geçici)", "On (temporary)")
    val notificationsUnknown get() = t("Bilinmiyor", "Unknown")
    val notificationsDisabledTitle get() = t("Bildirimler kapalı", "Notifications are off")
    val notificationsDisabledMessage get() = t(
        "Servis başladığında ve sürücü yaklaştığında haberdar olmak için bildirimleri açın.",
        "Enable notifications to know when the shuttle starts and when the driver is nearby."
    )
    val openSettings get() = t("Ayarları Aç", "Open Settings")
    val later get() = t("Sonra", "Later")
    val nextStop get() = t("SONRAKİ DURAK", "NEXT STOP")
    val morningPickup get() = t("Sabah biniş", "Morning pickup")
    val noStop get() = t("Durak yok", "No stop")
    val waitingForPassengerPickup get() = t("Yolcu biniş noktası bekleniyor.", "Waiting for passenger pickup point.")
    val capacity get() = t("KAPASİTE", "CAPACITY")
    val stops get() = t("DURAKLAR", "STOPS")
    val shuttleWaiting get() = t("Servis bekliyor", "Shuttle waiting")
    val tripDuration get() = t("Servis Süresi", "Shuttle Duration")
    val locationPermissionTitle get() = t("Servis için konum izni", "Location permission for shuttle")
    val locationPermissionBodySettings get() = t(
        "Ayarlar açıldıysa aşağıdaki adımları uygulayın, sonra bu ekrana dönün.",
        "If Settings opened, follow the steps below, then return to this screen."
    )
    val locationPermissionBodyInitial get() = t(
        "Yolcular sizi haritada görebilsin diye tek seferlik izin gerekir.",
        "One-time permission is required so passengers can see you on the map."
    )
    val locationStep1 get() = t("Açılan pencerede Konum veya İzinler'e dokunun.", "In the dialog, tap Location or Permissions.")
    val locationStep2 get() = t("\"Her zaman izin ver\" seçeneğini işaretleyin.", "Select \"Always allow\".")
    val locationStep3 get() = t("Geri gelip Servisi başlat'a tekrar basın.", "Come back and tap Start Shuttle again.")
    val grantPermission get() = t("İZİN VER", "GRANT PERMISSION")
    val grantPermissionShort get() = t("İzin ver", "Allow")
    val ifWindowDidNotOpen get() = t("Pencere açılmadıysa", "If the dialog didn't open")
    val goToSettings get() = t("Ayarlara git", "Go to Settings")
    val driverLocationPermissionDenied get() = t(
        "Konum izni kapalı. Servis başlatmak ve konum paylaşmak için izin gerekli.",
        "Location permission is off. Permission is required to start the shuttle and share your location."
    )
    val driverLocationSharingStopped get() = t(
        "Konum paylaşımı durdu. Ayarlar'dan \"Her zaman\" iznini açın veya seferi yeniden başlatın.",
        "Location sharing stopped. Enable \"Always\" permission in Settings or restart the trip."
    )
    val motionPermissionRationale get() = t(
        "Servise bindiğinizi otomatik anlamak için hareket veriniz kullanılır; sürekli konum paylaşımı gerekmez.",
        "Motion data is used to detect when you board the shuttle automatically; continuous location sharing is not required."
    )
    val motionPermissionTitle get() = t("Hareket izni gerekli", "Motion permission required")
    val driverLocationForegroundTitle get() = t("Konum izni gerekli", "Location permission required")
    val driverLocationForegroundBody get() = t(
        "Servisi başlatmak için önce konum izni vermeniz gerekir.",
        "Location permission is required before you can start the shuttle."
    )
    val locationForegroundSettingsStep1 get() = t("\"Ayarlara git\"e basın.", "Tap \"Go to Settings\".")
    val locationForegroundSettingsStep2 get() = t(
        "Konum → \"Uygulama Kullanılırken\" veya \"Her Zaman\" seçin.",
        "Location → select \"While Using\" or \"Always\"."
    )
    val locationForegroundSettingsStep3 get() = t("Uygulamaya dönün.", "Return to the app.")
    val motionPermissionDisabledMessage get() = t(
        "Servise bindiğinizi anlamak için hareket izni gerekir. Ayarlardan açın.",
        "Motion permission is required to detect boarding. Enable it in Settings."
    )
    val driverMotionPermissionBody get() = t(
        "Yolcuların servise bindiğini anlamak için hareket izni gerekir.",
        "Motion permission is required to detect when passengers board the shuttle."
    )
    val motionPermissionBodySettings get() = t(
        "Ayarlar açıldıysa aşağıdaki adımları uygulayın, sonra bu ekrana dönün.",
        "If Settings opened, follow the steps below, then return to this screen."
    )
    val motionSettingsStep1 get() = t("\"Ayarlara git\"e basın.", "Tap \"Go to Settings\".")
    val motionSettingsStep2 get() = t(
        "İzinler → Fiziksel aktivite'yi açın.",
        "Permissions → enable Physical activity."
    )
    val motionSettingsStep3 get() = t("Uygulamaya dönün.", "Return to the app.")
    val driverNotificationPermissionBody get() = t(
        "Yolcular katıldığında ve servis başladığında haberdar olmak için bildirimleri açın.",
        "Enable notifications to know when passengers board and when the shuttle starts."
    )
    val passengerNotificationPermissionBody get() = t(
        "Bildirimleri açmadan konum ve kayıt adımlarına devam edemezsiniz.",
        "You must enable notifications before continuing with location and saving."
    )
    val passengerLocationForegroundBody get() = t(
        "Biniş noktanızı kaydetmek için konum izni gerekir.",
        "Location permission is required to save your pickup point."
    )
    val passengerMotionPermissionBody get() = t(
        "Servise bindiğinizi otomatik anlamak için hareket izni gerekir.",
        "Motion permission is required to detect when you board the shuttle automatically."
    )
    val notificationPermissionBodySettings get() = t(
        "Ayarlar açıldıysa aşağıdaki adımları uygulayın, sonra bu ekrana dönün.",
        "If Settings opened, follow the steps below, then return to this screen."
    )
    val notificationSettingsStep1 get() = t("\"Ayarlara git\"e basın.", "Tap \"Go to Settings\".")
    val notificationSettingsStep2 get() = t(
        "Bildirimler → BusTracker'ı açın.",
        "Notifications → enable BusTracker."
    )
    val notificationSettingsStep3 get() = t("Uygulamaya dönün.", "Return to the app.")
    val shuttleStarted get() = t("Servis başladı", "Shuttle started")
    val clothingAdvice get() = t("GİYİM ÖNERİSİ", "CLOTHING TIP")
    val weatherLoading get() = t("Biniş noktana göre öneri hazırlanıyor…", "Preparing a tip for your pickup point…")
    val weatherUnavailable get() = t("Öneri şu an alınamadı.", "Tip unavailable right now.")
    val pickupPlaceFallback get() = t("Biniş noktan", "your pickup point")
    val adviceRain get() = t("Yağmur var — şemsiyeni kap.", "Rain expected — grab an umbrella.")
    val adviceColdHat get() = t("Hava soğuk — bere takmadan çıkma.", "It's cold — don't leave without a hat.")
    val adviceVeryHot get() = t("Hava cehennem gibi — şapka tak, su al.", "Very hot — wear a hat and bring water.")
    val adviceHot get() = t("Hava sıcak — şapka tak, su al.", "It's hot — wear a hat and bring water.")
    val adviceCool get() = t("Hava serin — ince mont veya hırka al.", "It's cool — bring a light jacket.")
    val adviceCold get() = t("Hava soğuk — kalın giyin.", "It's cold — dress warmly.")
    val myShuttles get() = t("SERVİSLERİM", "MY SHUTTLES")
    val systemActive get() = t("SİSTEM AKTİF", "SYSTEM ACTIVE")
    val driverStartedShuttle get() = t("Sürücü servisi başlattı", "Driver started the shuttle")
    val savedRoutes get() = t("KAYITLI ROTALAR", "SAVED ROUTES")
    val switchRoute get() = t("GEÇİŞ YAP", "SWITCH")
    val noOtherRoute get() = t("Başka rota yok", "No other route")
    val addNewShuttle get() = t("YENİ SERVİS EKLE", "ADD NEW SHUTTLE")
    val addShuttleTitle get() = t("Yeni servis ekle", "Add new shuttle")
    val addShuttleBody get() = t(
        "Sürücünün verdiği servis kodunu girin. Ekledikten sonra bu servis aktif olur.",
        "Enter the shuttle code from your driver. This shuttle becomes active after you add it."
    )
    val addShuttleHint get() = t(
        "Yeni bir servis ekleyerek rotalarını genişletebilirsin.",
        "Add another shuttle to expand your routes."
    )
    val sixDigitCode get() = t("6 haneli kod", "6-digit code")
    val joinShuttle get() = t("SERVİSE KATIL", "JOIN SHUTTLE")
    val operationFailed get() = t("İşlem başarısız.", "Operation failed.")
    val noPassengersYet get() = t("Henüz yolcu yok", "No passengers yet")
    val connectionError get() = t("Bağlantı Hatası", "Connection Error")
    val tryAgain get() = t("Tekrar Dene", "Try Again")
    val firebaseChecklistTitle get() = t("Firebase Console kontrol listesi:", "Firebase Console checklist:")
    val firebaseCheck1 get() = t("1. Firestore Database oluşturulmuş olmalı", "1. Firestore Database must be created")
    val firebaseCheck2 get() = t("2. Authentication → Apple etkin olmalı", "2. Authentication → Apple must be enabled")
    val firebaseCheck3 get() = t("3. Push Notifications (APNs) yapılandırılmalı", "3. Push Notifications (APNs) must be configured")
    val shuttleInvite get() = t("Servis daveti", "Shuttle invite")
    val alreadyMemberOfShuttle get() = t("Bu servise zaten kayıtlısınız.", "You are already registered for this shuttle.")
    val signInRequired get() = t("Giriş yapmanız gerekiyor.", "You need to sign in.")
    val shuttleCodeNotFound get() = t("Bu servis kodu bulunamadı.", "This shuttle code was not found.")
    val alreadyInShuttle get() = t("Zaten bir servise kayıtlısınız.", "You are already registered for a shuttle.")
    val serviceNameEmpty get() = t("Servis adı boş olamaz.", "Shuttle name cannot be empty.")
    val nameEmpty get() = t("Adınız boş olamaz.", "Your name cannot be empty.")
    val alreadyRegisteredForShuttle get() = t("Bu servise zaten kayıtlısınız.", "You are already registered for this shuttle.")
    val selectTripDuration get() = t("Servis süresi seçin.", "Select shuttle duration.")
    val appleUserIDNotFound get() = t("Apple hesap kimliği bulunamadı.", "Apple account ID not found.")
    val accountDisabled get() = t("Bu hesap devre dışı bırakılmış.", "This account has been disabled.")
    val appleSignInCancelled get() = t("Apple ile giriş iptal edildi.", "Sign in with Apple was cancelled.")
    val notSignedIn get() = t("Oturum açık değil.", "Not signed in.")
    val userProfileNotFound get() = t("Kullanıcı profili bulunamadı.", "User profile not found.")
    val firebaseNotReady get() = t("Firebase henüz hazır değil. Lütfen tekrar deneyin.", "Firebase is not ready yet. Please try again.")
    val appleUserIDUnavailable get() = t("Apple hesap kimliği alınamadı.", "Could not retrieve Apple account ID.")
    val appleCredentialUnavailable get() = t("Apple giriş bilgisi alınamadı.", "Could not retrieve Apple sign-in credentials.")
    val appleAuthFailed get() = t("Apple kimlik doğrulaması tamamlanamadı.", "Apple authentication could not be completed.")
    val firebaseNotReadyShort get() = t("Firebase hazır değil.", "Firebase is not ready.")
    val signInWithPhone get() = t("Telefon ile giriş yapın.", "Sign in with your phone.")
    val invalidServiceCode get() = t("Geçerli bir servis kodu yok.", "No valid shuttle code.")
    val smlerAPIKeyMissing get() = t("Smler API anahtarı eksik.", "Smler API key is missing.")
    val smlerAPIInvalid get() = t("Smler API adresi geçersiz.", "Smler API URL is invalid.")
    val smlerNoResponse get() = t("Smler yanıt vermedi.", "Smler did not respond.")
    val apiURLNotFound get() = t("API adresi bulunamadı.", "API URL not found.")
    val smlerShareTitle get() = t("Shuttle Live servis daveti", "Shuttle Live shuttle invite")
    val smlerOGTitle get() = t("Shuttle Live — Servis daveti", "Shuttle Live — Shuttle invite")

    val waitingForPassengersHint get() = t("Yolcular yukarıdaki servis kodunu kullanarak katıldığında burada görünecek.", "Passengers will appear here when they join using the shuttle code above.")
    val backgroundLocationWarning get() = t("Arka planda konum paylaşımı için \"Her zaman\" iznini açın.", "Enable \"Always\" location permission for background sharing.")
    val attendanceHint get() = t("Seçiminiz sürücüye kaydedilir. Servis bitince yeniden seçmeniz gerekir.", "Your choice is saved for the driver. You must choose again after the shuttle ends.")
    val attendanceHolidayHint get() = t(
        "Seçiminiz yalnızca bugün için geçerli. Mod açıkken diğer günler varsayılan gelmiyorum.",
        "Your choice is for today only. While this mode is on, other days default to not coming."
    )
    val tapMapToSelectPickup get() = t("Haritaya dokunarak biniş noktanızı seçin.", "Tap the map to select your pickup point.")
    val signOutConfirmMessage get() = t("Çıkış yapmak istediğinize emin misiniz?", "Are you sure you want to sign out?")
    val deleteAccountConfirmMessage get() = t("Hesabınızı ve tüm verilerinizi kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz.", "Are you sure you want to permanently delete your account and all data? This cannot be undone.")
    val deleteAccountConfirmMessagePassenger get() = t("Hesabınızı ve tüm verilerinizi (profil, biniş noktaları, katılım kayıtları vb.) kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz.", "Are you sure you want to permanently delete your account and all data (profile, pickup points, attendance records, etc.)? This cannot be undone.")
    val alwaysLocationRequiredToStart get() = t("Servisi başlatmak için \"Her zaman\" konum izni zorunludur. Ayarlar'dan izin verin.", "\"Always\" location permission is required to start the shuttle. Enable it in Settings.")
    val weatherNeedsPickup get() = t("Biniş noktanızı kaydettikten sonra giyim önerisi görünür.", "Save your pickup point to see a clothing tip.")
    val tripDurationBodyCanStart get() = t("Sefer boyunca konumunuz yolculara paylaşılır. Paylaşım süre sonunda otomatik durur.", "Your location is shared with passengers during the trip. Sharing stops automatically when time is up.")
    val tripDurationBodyNeedsPermission get() = t("\"Her zaman\" konum izni olmadan servis başlatılamaz. Önce İZİN VER adımlarını tamamlayın.", "The shuttle cannot start without \"Always\" location permission. Complete the GRANT PERMISSION steps first.")
    val pickupSavedComing get() = t("Biniş noktanız kaydedildi. Durumunuz: Geliyorum.", "Pickup point saved. Your status: I'm coming.")

    fun yourChoice(choice: String) = t("Seçiminiz: $choice", "Your choice: $choice")
    fun savedAt(time: String) = t("Kayıtlı: $time", "Saved: $time")
    fun tripEndTime(time: String) = t("Bitiş: $time", "Ends: $time")
    fun weatherContext(placeName: String, temperature: Int) = t("Bugün $placeName · $temperature°", "Today $placeName · $temperature°")
    fun choiceSaved(choice: String) = t("Seçiminiz kaydedildi: $choice", "Your choice was saved: $choice")
    fun shuttleStartedAutoStop(hoursLabel: String) = t("Servis başlatıldı. $hoursLabel sonra otomatik duracak.", "Shuttle started. It will stop automatically after $hoursLabel.")
    val shuttleStartedMotionAutoStop get() = t(
        "Servis başlatıldı. Yolcular indiğinde otomatik duracak.",
        "Shuttle started. It will stop automatically when passengers have been dropped off."
    )
    val tripStartConfirmTitle get() = t("Servisi Başlat", "Start Shuttle")
    val tripStartConfirmBody get() = t(
        "Konumunuz yolcularla paylaşılır. Yolcular indiğinde servis otomatik durur. En geç 3 saatte de durur.",
        "Your location is shared with passengers. The shuttle stops when passengers have been dropped off, or after 3 hours at most."
    )
    fun driverStartedTrip(driverName: String) = t("$driverName servisi yola çıktı. Bugün gelecek misiniz?", "$driverName's shuttle is on the way. Are you coming today?")
    fun unspecifiedCount(count: Int) = t("$count belirtmedi", "$count no response")
    fun startedAt(time: String) = t("Başlangıç: $time", "Started: $time")
    fun shuttleAdded(name: String) = t("$name eklendi ve aktif yapıldı.", "$name was added and set as active.")
    fun shuttleFallbackName(prefix: String) = t("Servis $prefix", "Shuttle $prefix")

    fun hoursLabel(hours: Double): String {
        val whole = hours.toLong()
        return if (hours == whole.toDouble()) {
            t("${whole.toInt()} saat", "${whole.toInt()} hours")
        } else {
            t("$hours saat", "$hours hours")
        }
    }
}
