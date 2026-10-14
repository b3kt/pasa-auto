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
    backToDashboard: 'Kembali ke dasbor'
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
      email: 'Email',
      password: 'Kata sandi',
      submit: 'Masuk',
      emailRequired: 'Email wajib diisi',
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
      emailRequired: 'Email wajib diisi',
      phone: 'Telepon (opsional)',
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
      phone: 'Telepon',
      mobile: 'HP',
      otherPhone: 'Telepon lainnya',
      email: 'Email',
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
        CONVERTED: 'Dikonversi',
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
