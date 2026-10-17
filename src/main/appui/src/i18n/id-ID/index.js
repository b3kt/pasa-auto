// Indonesian message catalog for the customer portal. Mirrors the key structure of en-US/index.js.
export default {
  app: {
    name: 'Pasa Auto | Customer'
  },

  common: {
    menu: 'Menu',
    linkNow: 'Tautkan sekarang',
    cancel: 'Batal',
    close: 'Tutup',
    save: 'Simpan',
    logout: 'Keluar',
    loggedOut: 'Berhasil keluar',
    required: 'Wajib diisi',
    backToDashboard: 'Kembali ke dasbor',
    offline: 'Anda sedang offline. Menampilkan data dari kunjungan terakhir; perubahan memerlukan koneksi.'
  },

  language: {
    label: 'Bahasa',
    id: 'Bahasa Indonesia',
    en: 'English'
  },

  nav: {
    dashboard: 'Dasbor',
    profile: 'Profil',
    vehicles: 'Kendaraan',
    bookings: 'Booking',
    serviceStatus: 'Status Servis'
  },

  pages: {
    login: {
      title: 'Masuk',
      identifier: 'Email atau nomor HP',
      password: 'Kata sandi',
      submit: 'Masuk',
      identifierRequired: 'Email atau nomor HP wajib diisi',
      passwordRequired: 'Kata sandi wajib diisi',
      sessionExpired: 'Sesi Anda telah berakhir. Silakan masuk kembali.',
      noAccount: 'Belum punya akun? Daftar',
      success: 'Berhasil masuk',
      failed: 'Gagal masuk'
    },

    register: {
      title: 'Buat akun Anda',
      fullName: 'Nama lengkap',
      fullNameRequired: 'Nama lengkap wajib diisi',
      email: 'Email',
      phone: 'Nomor HP',
      contactHint: 'Isi email, nomor HP, atau keduanya - keduanya bisa dipakai untuk masuk',
      contactRequired: 'Isi email atau nomor HP',
      password: 'Kata sandi',
      passwordRule: 'Minimal 8 karakter',
      submit: 'Daftar',
      hasAccount: 'Sudah punya akun? Masuk',
      success: 'Akun berhasil dibuat',
      failed: 'Pendaftaran gagal'
    },

    dashboard: {
      welcome: 'Selamat datang, {name}',
      notLinked: 'Akun Anda belum tertaut ke riwayat servis Anda.',
      customerRecord: 'Data pelanggan',
      editProfile: 'Ubah profil',
      myVehicles: 'Kendaraan saya',
      viewVehicles: 'Lihat kendaraan yang terdaftar',
      bookService: 'Booking servis',
      requestAppointment: 'Ajukan jadwal servis',
      noVehicles: 'Tidak ada kendaraan terdaftar',
      noUpcomingBookings: 'Tidak ada booking mendatang',
      serviceHistory: 'Riwayat servis',
      viewAll: 'Lihat semua',
      noServiceHistory: 'Belum ada riwayat servis'
    },

    profile: {
      title: 'Profil saya',
      linkFirst: 'Tautkan akun Anda terlebih dahulu untuk mengelola profil pelanggan.',
      fullName: 'Nama lengkap',
      nameRequired: 'Nama wajib diisi',
      address: 'Alamat',
      city: 'Kota',
      postalCode: 'Kode pos',
      mobile: 'HP',
      mobileHint: 'Juga dipakai untuk masuk dengan nomor ini',
      email: 'Email',
      emailHint: 'Email untuk masuk; tidak dapat diubah di sini',
      contactPerson: 'Narahubung',
      updated: 'Profil diperbarui',
      updateFailed: 'Tidak dapat memperbarui profil'
    },

    vehicles: {
      title: 'Kendaraan saya',
      linkFirst: 'Tautkan akun Anda terlebih dahulu untuk melihat kendaraan Anda.',
      none: 'Tidak ada kendaraan pada data Anda.',
      addVehicle: 'Tambah kendaraan baru',
      editVehicle: 'Edit kendaraan',
      plateNumber: 'Nomor polisi',
      brand: 'Merk',
      type: 'Jenis',
      register: 'Daftarkan kendaraan',
      registered: 'Kendaraan berhasil didaftarkan',
      registerFailed: 'Gagal mendaftarkan kendaraan',
      updated: 'Kendaraan berhasil diperbarui',
      saveFailed: 'Gagal menyimpan kendaraan',
      ownedByOther: 'Nomor polisi sudah terdaftar sebelumnya, tautkan sekarang ke profil saya?',
      ownedBySelf: 'Nomor polisi ini sudah terdaftar pada akun Anda.',
      onRecordQuestion: 'Nomor polisi ini sudah terdaftar sebelumnya: {vehicle}, atas nama {owner}. Apakah ini kendaraan Anda?',
      onRecordYes: 'Ya, milik saya',
      onRecordNo: 'Bukan',
      onRecordLinkNote: 'Akun Anda akan ditautkan ke data pelanggan ini beserta riwayat servisnya.',
      onRecordTransferNote: 'Hanya kendaraan yang dipindahkan ke akun Anda; data pelanggan lama tetap seperti semula.',
      useRecordedData: 'Gunakan merk dan jenis yang tercatat',
      tooManyChecks: 'Terlalu banyak nomor polisi yang dicek. Coba lagi dalam {minutes} menit.',
      linkPendingApproval: 'Kontak pada data lama berbeda dengan akun Anda, jadi staf akan mengonfirmasi penautan terlebih dahulu.',
      claimNow: 'Tautkan sekarang',
      claimConfirmTitle: 'Tautkan kendaraan',
      claimConfirmMessage: 'Tautkan nomor polisi {nopol} ke profil Anda sekarang? Kepemilikan akan dialihkan dari pemilik saat ini.',
      claimLinked: 'Kendaraan berhasil ditautkan ke profil Anda',
      claimPending: 'Klaim dikirim untuk persetujuan staf',
      claimFailed: 'Gagal mengirim klaim',
      deleteConfirmTitle: 'Hapus kendaraan',
      deleteConfirmMessage: 'Apakah Anda yakin ingin menghapus kendaraan {nopol}?',
      deleted: 'Kendaraan berhasil dihapus',
      deleteFailed: 'Gagal menghapus kendaraan'
    },

    bookings: {
      title: 'Booking servis',
      linkFirst: 'Tautkan akun Anda terlebih dahulu untuk melakukan booking servis.',
      requestTitle: 'Ajukan booking baru',
      plateNumber: 'Nomor polisi',
      km: 'KM / Odometer',
      kmRequired: 'KM / Odometer wajib diisi',
      noVehicles: 'Tidak ada kendaraan terdaftar pada akun Anda',
      preferredDateTime: 'Tanggal/waktu pilihan',
      serviceType: 'Jenis servis',
      notes: 'Catatan',
      submit: 'Ajukan booking',
      noPlate: 'Tanpa nomor polisi',
      generalService: 'Servis umum',
      staffNote: 'Catatan dari bengkel: {note}',
      none: 'Belum ada booking.',
      requested: 'Booking diajukan',
      requestFailed: 'Tidak dapat mengajukan booking',
      cancelled: 'Booking dibatalkan',
      cancelFailed: 'Tidak dapat membatalkan booking',
      status: {
        REQUESTED: 'Diajukan',
        CONFIRMED: 'Dikonfirmasi',
        CONVERTED: 'Diterima',
        REJECTED: 'Ditolak',
        CANCELLED: 'Dibatalkan'
      }
    },

    spk: {
      title: 'Status servis',
      linkFirst: 'Tautkan akun Anda terlebih dahulu untuk melacak status servis Anda.',
      none: 'Belum ada riwayat servis.',
      invoiceTitle: 'Faktur penjualan',
      downloadPdf: 'Unduh PDF',
      noInvoice: 'Belum ada faktur untuk servis ini.',
      invoiceFailed: 'Tidak dapat memuat faktur',
      status: {
        OPEN: 'Menunggu',
        PROSES: 'Diproses',
        SELESAI: 'Selesai',
        BATAL: 'Batal'
      }
    },

    linkAccount: {
      title: 'Tautkan akun Anda',
      intro:
        'Verifikasi identitas Anda dengan memasukkan nomor polisi (nopol) kendaraan untuk menemukan riwayat servis Anda.',
      alreadyLinked: 'Akun Anda sudah tertaut ke {name}.',
      plateNumber: 'Nomor polisi (nopol)',
      plateRequired: 'Nomor polisi wajib diisi',
      verify: 'Verifikasi nomor polisi',
      notFound: 'Tidak ada kendaraan dengan nomor polisi tersebut',
      notFoundOrLinked: 'Tidak ada data pelanggan yang belum tertaut untuk nomor polisi tersebut',
      plate: 'Nopol: {nopol}',
      phone: 'Telepon: {phone}',
      confirm: 'Ini saya, tautkan sekarang',
      requestSubmitted: 'Permintaan dikirim untuk persetujuan',
      submitFailed: 'Tidak dapat mengirim permintaan tautan',
      skip: 'Lewati dulu',
      skipped: 'Akun belum tertaut. Anda bisa menautkannya nanti dari beranda.'
    },

    unavailable: {
      title: 'Portal pelanggan sementara tidak tersedia',
      message:
        'Silakan coba lagi nanti, atau hubungi kami langsung untuk bantuan booking atau servis kendaraan Anda.'
    },

    notFound: {
      title: 'Halaman tidak ditemukan',
      message: 'Halaman yang Anda cari tidak ada.'
    }
  }
}
