import { ref } from 'vue'
import { api } from 'boot/axios'
import { useQuasar } from 'quasar'

export function useCrud(config) {
    const {
        baseApiUrl,
        idField = 'id',
        defaultPagination = {
            sortBy: null,
            descending: false,
            page: 1,
            rowsPerPage: 10,
            rowsNumber: 0
        },
        transformPayload = (data) => data,
        onSuccess = () => { },
        onError = () => { }
    } = config

    const $q = useQuasar()

    // State
    const rows = ref([])
    const loading = ref(false)
    const saving = ref(false)
    const deleting = ref(false)
    const showDialog = ref(false)
    const showDeleteDialog = ref(false)
    const isEditMode = ref(false)
    const itemToDelete = ref(null)
    const searchText = ref('')
    const initialData = ref(null)

    const pagination = ref({ ...defaultPagination })

    // Methods
    const fetchData = async (customParams = {}) => {
        loading.value = true
        try {
            const params = {
                page: pagination.value.page,
                rowsPerPage: pagination.value.rowsPerPage,
                ...customParams
            }

            if (pagination.value.sortBy) {
                params.sortBy = pagination.value.sortBy
                params.descending = pagination.value.descending
            }

            if (searchText.value) {
                params.search = searchText.value
            }

            const response = await api.get(`${baseApiUrl}/paginated`, { params })
            if (response.data.success) {
                const pageData = response.data.data
                rows.value = pageData.rows || []
                pagination.value.rowsNumber = pageData.rowsNumber
                pagination.value.page = pageData.page
                pagination.value.rowsPerPage = pageData.rowsPerPage

                // Update initialData if we are in edit mode and the currently edited item is in the fetched rows
                if (isEditMode.value && initialData.value) {
                    const currentId = initialData.value[idField]
                    const updatedItem = rows.value.find(row => row[idField] === currentId)
                    if (updatedItem) {
                        // We don't automatically update the form, but we could update initialData to reflect server state
                        // However, it's safer to let the user know there's a change or refresh manually.
                        // For now, the requirement says "fetch the selected data, to prevent optimistic locking failure"
                    }
                }
            }
        } catch (error) {
            $q.notify({
                type: 'negative',
                message: 'Failed to fetch data',
                caption: error.response?.data?.message || error.message
            })
            onError(error)
        } finally {
            loading.value = false
        }
    }

    const onRequest = (props) => {
        const { page, rowsPerPage, sortBy, descending } = props.pagination
        pagination.value.page = page
        pagination.value.rowsPerPage = rowsPerPage
        pagination.value.sortBy = sortBy
        pagination.value.descending = descending
        fetchData()
    }

    const onSearch = (val) => {
        searchText.value = val
        pagination.value.page = 1
        fetchData()
    }

    const saveData = async (formData) => {
        saving.value = true
        try {
            const payload = transformPayload(formData)
            let response

            if (isEditMode.value) {
                response = await api.put(`${baseApiUrl}/${formData[idField]}`, payload)
            } else {
                response = await api.post(baseApiUrl, payload)
            }

            if (response.data.success) {
                $q.notify({
                    type: 'positive',
                    message: isEditMode.value ? 'Item updated successfully' : 'Item created successfully'
                })
                showDialog.value = false
                await fetchData()
                const result = response.data.data
                if (result) {
                    initialData.value = JSON.parse(JSON.stringify(result))
                }
                onSuccess('save', result)
                return result || true
            }
        } catch (error) {
            $q.notify({
                type: 'negative',
                message: 'Failed to save item',
                caption: error.response?.data?.message || error.message
            })
            onError(error)
            return false
        } finally {
            saving.value = false
        }
    }

    const confirmDelete = (row) => {
        itemToDelete.value = row
        showDeleteDialog.value = true
    }

    const deleteItem = async () => {
        // Ensure we have a row selected to delete
        if (!itemToDelete.value) {
            $q.notify({ type: 'warning', message: 'No item selected to delete' })
            return false
        }

        // Resolve identifier safely
        const id = itemToDelete.value?.[idField]
        if (id === undefined || id === null || id === '') {
            $q.notify({
                type: 'negative',
                message: 'Failed to delete item',
                caption: `Missing identifier field "${idField}" on selected row`
            })
            return false
        }

        deleting.value = true
        try {
            const response = await api.delete(`${baseApiUrl}/${id}`)
            const success = response?.data?.success === true
            if (success) {
                $q.notify({
                    type: 'positive',
                    message: 'Item deleted successfully'
                })
                showDeleteDialog.value = false
                itemToDelete.value = null
                await fetchData()
                onSuccess('delete')
                return true
            } else {
                // Backend responded but indicates failure
                $q.notify({
                    type: 'negative',
                    message: 'Failed to delete item',
                    caption: response?.data?.message || 'Unknown error'
                })
                onError(new Error(response?.data?.message || 'Delete failed'))
                return false
            }
        } catch (error) {
            $q.notify({
                type: 'negative',
                message: 'Failed to delete item',
                caption: error.response?.data?.message || error.message
            })
            onError(error)
            return false
        } finally {
            deleting.value = false
        }
    }

    const openCreateDialog = (resetFormCallback) => {
        isEditMode.value = false
        if (resetFormCallback) resetFormCallback()
        initialData.value = null
        showDialog.value = true
    }

    const openEditDialog = (row, setFormCallback) => {
        isEditMode.value = true
        if (setFormCallback) setFormCallback(row)
        initialData.value = JSON.parse(JSON.stringify(row))
        showDialog.value = true
    }

    const isDirty = (currentData) => {
        if (!initialData.value) return true // In create mode, everything is "dirty" or we just allow save
        return JSON.stringify(currentData) !== JSON.stringify(initialData.value)
    }

    return {
        rows,
        loading,
        saving,
        deleting,
        showDialog,
        showDeleteDialog,
        isEditMode,
        itemToDelete,
        searchText,
        pagination,
        initialData,
        fetchData,
        onRequest,
        onSearch,
        saveData,
        confirmDelete,
        deleteItem,
        openCreateDialog,
        openEditDialog,
        isDirty
    }
}
