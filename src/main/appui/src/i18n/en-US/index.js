// English (default) message catalog for the customer portal.
// Keys are namespaced by area: `common`/`nav`/`language` for shared UI, `pages.<page>` for screens.
export default {
  app: {
    name: 'Pasa Auto | Customer'
  },

  common: {
    menu: 'Menu',
    linkNow: 'Link now',
    cancel: 'Cancel',
    close: 'Close',
    save: 'Save',
    logout: 'Logout',
    loggedOut: 'Logged out successfully',
    required: 'Required',
    backToDashboard: 'Back to dashboard'
  },

  language: {
    label: 'Language',
    id: 'Bahasa Indonesia',
    en: 'English'
  },

  nav: {
    dashboard: 'Dashboard',
    profile: 'Profile',
    vehicles: 'Vehicles',
    bookings: 'Bookings',
    serviceStatus: 'Service Status'
  },

  pages: {
    login: {
      title: 'Sign in',
      email: 'Email',
      password: 'Password',
      submit: 'Sign in',
      emailRequired: 'Email is required',
      passwordRequired: 'Password is required',
      sessionExpired: 'Your session has expired. Please sign in again.',
      noAccount: "Don't have an account? Register",
      success: 'Signed in successfully',
      failed: 'Login failed'
    },

    register: {
      title: 'Create your account',
      fullName: 'Full name',
      fullNameRequired: 'Full name is required',
      email: 'Email',
      emailRequired: 'Email is required',
      phone: 'Phone (optional)',
      password: 'Password',
      passwordRule: 'At least 8 characters',
      submit: 'Register',
      hasAccount: 'Already have an account? Sign in',
      success: 'Account created',
      failed: 'Registration failed'
    },

    dashboard: {
      welcome: 'Welcome, {name}',
      notLinked: "Your account isn't linked to your service history yet.",
      customerRecord: 'Customer record',
      editProfile: 'Edit profile',
      myVehicles: 'My vehicles',
      viewVehicles: 'View your registered vehicles',
      bookService: 'Book a service',
      requestAppointment: 'Request an appointment',
      noVehicles: 'No vehicles registered',
      noUpcomingBookings: 'No upcoming bookings',
      serviceHistory: 'Service history',
      viewAll: 'View all',
      noServiceHistory: 'No service records yet'
    },

    profile: {
      title: 'My profile',
      linkFirst: 'Link your account first to manage your customer profile.',
      fullName: 'Full name',
      nameRequired: 'Name is required',
      address: 'Address',
      city: 'City',
      postalCode: 'Postal code',
      phone: 'Phone',
      mobile: 'Mobile',
      otherPhone: 'Other phone',
      email: 'Email',
      contactPerson: 'Contact person',
      updated: 'Profile updated',
      updateFailed: 'Could not update profile'
    },

    vehicles: {
      title: 'My vehicles',
      linkFirst: 'Link your account first to see your vehicles.',
      none: 'No vehicles found on your record.',
      addVehicle: 'Add new vehicle',
      editVehicle: 'Edit vehicle',
      plateNumber: 'Plate number',
      brand: 'Brand',
      type: 'Type',
      register: 'Register vehicle',
      registered: 'Vehicle registered successfully',
      registerFailed: 'Failed to register vehicle',
      updated: 'Vehicle updated successfully',
      saveFailed: 'Failed to save vehicle',
      deleteConfirmTitle: 'Delete vehicle',
      deleteConfirmMessage: 'Are you sure you want to delete vehicle {nopol}?',
      deleted: 'Vehicle deleted successfully',
      deleteFailed: 'Failed to delete vehicle'
    },

    bookings: {
      title: 'Service bookings',
      linkFirst: 'Link your account first to book a service.',
      requestTitle: 'Request a new booking',
      plateNumber: 'Plate number',
      km: 'Odometer (km)',
      kmRequired: 'Odometer reading is required',
      noVehicles: 'No registered vehicles found on your account',
      preferredDateTime: 'Preferred date/time',
      serviceType: 'Service type',
      notes: 'Notes',
      submit: 'Request booking',
      noPlate: 'No plate given',
      generalService: 'General service',
      staffNote: 'Note from shop: {note}',
      none: 'No bookings yet.',
      requested: 'Booking requested',
      requestFailed: 'Could not request booking',
      cancelled: 'Booking cancelled',
      cancelFailed: 'Could not cancel booking',
      status: {
        REQUESTED: 'Requested',
        CONFIRMED: 'Confirmed',
        CONVERTED: 'Converted',
        REJECTED: 'Rejected',
        CANCELLED: 'Cancelled'
      }
    },

    spk: {
      title: 'Service status',
      linkFirst: 'Link your account first to track your service status.',
      none: 'No service records yet.',
      invoiceTitle: 'Sales invoice',
      downloadPdf: 'Download PDF',
      noInvoice: 'No invoice available for this service yet.',
      invoiceFailed: 'Could not load the invoice',
      status: {
        OPEN: 'Waiting',
        PROSES: 'In progress',
        SELESAI: 'Done',
        BATAL: 'Cancelled'
      }
    },

    linkAccount: {
      title: 'Link your account',
      intro:
        'Verify your identity by entering the plate number (nopol) of your vehicle to find your service record.',
      alreadyLinked: 'Your account is already linked to {name}.',
      plateNumber: 'Plate number (nopol)',
      plateRequired: 'Plate number is required',
      verify: 'Verify plate number',
      notFound: 'No vehicle found with that plate number',
      notFoundOrLinked: 'No unlinked vehicle record found for that plate number',
      plate: 'Plate: {nopol}',
      phone: 'Phone: {phone}',
      confirm: 'This is me, link now',
      requestSubmitted: 'Request submitted for approval',
      submitFailed: 'Could not submit link request',
      skip: 'Skip for now',
      skipped: 'Account not linked yet. You can link it later from the dashboard.'
    },

    unavailable: {
      title: 'Customer portal temporarily unavailable',
      message:
        'Please check back later, or contact us directly for help with your booking or vehicle service.'
    },

    notFound: {
      title: 'Page not found',
      message: 'The page you are looking for does not exist.'
    }
  }
}
