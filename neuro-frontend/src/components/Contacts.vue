<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

const props = defineProps(['user']);
const emit = defineEmits(['transfer-to-contact']);

const contacts = ref([]);
const searchQuery = ref('');
const isLoading = ref(false);
const showAddModal = ref(false);
const newContact = ref({
  contactUserId: '',
  nickname: ''
});
const errorMessage = ref('');
const successMessage = ref('');

const fetchContacts = async () => {
  isLoading.value = true;
  try {
    const res = await axios.get(`http://localhost:8080/api/contacts/${props.user.id}`);
    contacts.value = res.data;
  } catch (error) {
    console.error('Failed to fetch contacts', error);
  } finally {
    isLoading.value = false;
  }
};

const addContact = async () => {
  if (!newContact.value.contactUserId || !newContact.value.nickname) {
    errorMessage.value = 'Please fill in all fields';
    return;
  }

  try {
    const payload = {
      userId: props.user.id,
      contactUserId: Number(newContact.value.contactUserId),
      nickname: newContact.value.nickname
    };
    
    await axios.post('http://localhost:8080/api/contacts', payload);
    showMessage('Contact added successfully', false);
    showAddModal.value = false;
    newContact.value = { contactUserId: '', nickname: '' };
    await fetchContacts();
  } catch (error) {
    showMessage(error.response?.data?.message || 'Failed to add contact', true);
  }
};

const toggleFavorite = async (contactId) => {
  try {
    await axios.post(`http://localhost:8080/api/contacts/${contactId}/toggle-favorite`);
    await fetchContacts();
  } catch (error) {
    showMessage('Failed to toggle favorite', true);
  }
};

const deleteContact = async (contactId) => {
  if (!confirm('Are you sure you want to delete this contact?')) return;
  
  try {
    await axios.delete(`http://localhost:8080/api/contacts/${contactId}`);
    showMessage('Contact deleted successfully', false);
    await fetchContacts();
  } catch (error) {
    showMessage('Failed to delete contact', true);
  }
};

const initiateTransfer = (contact) => {
  emit('transfer-to-contact', contact.contactUserId, contact.nickname);
};

const showMessage = (msg, isError) => {
  if (isError) {
    errorMessage.value = msg;
    setTimeout(() => errorMessage.value = '', 3000);
  } else {
    successMessage.value = msg;
    setTimeout(() => successMessage.value = '', 3000);
  }
};

const filteredContacts = computed(() => {
  if (!searchQuery.value) return contacts.value;
  
  const q = searchQuery.value.toLowerCase();
  return contacts.value.filter(c => 
    c.nickname.toLowerCase().includes(q) || 
    c.contactUserId.toString().includes(q)
  );
});

const favoriteContacts = computed(() => filteredContacts.value.filter(c => c.isFavorite));
const regularContacts = computed(() => filteredContacts.value.filter(c => !c.isFavorite));

onMounted(() => {
  fetchContacts();
});
</script>

<template>
  <div class="contacts-container">
    <div class="flex justify-between items-center mb-8">
      <div>
        <h3 class="section-title">Address Book</h3>
        <p class="text-slate-400 text-sm mt-2">Manage your frequent recipients</p>
      </div>
      <button @click="showAddModal = true" class="midnight-btn-sm">
        <i class="fas fa-user-plus mr-2"></i> Add Contact
      </button>
    </div>

    <!-- Search Bar -->
    <div class="relative search-wrapper mb-8">
      <input v-model="searchQuery" type="text" placeholder="Search contacts..." class="search-input w-full" />
      <i class="fas fa-search search-icon"></i>
    </div>

    <!-- Messages -->
    <div v-if="successMessage" class="success-message mb-4">
      <i class="fas fa-check-circle mr-2"></i> {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="error-text mb-4">{{ errorMessage }}</div>

    <!-- Loading State -->
    <div v-if="isLoading" class="text-center py-12">
      <i class="fas fa-circle-notch fa-spin text-4xl text-indigo-400"></i>
      <p class="text-slate-400 mt-4">Loading contacts...</p>
    </div>

    <!-- Contacts List -->
    <div v-else>
      <!-- Favorites -->
      <div v-if="favoriteContacts.length > 0" class="mb-8">
        <h4 class="text-sm font-bold text-indigo-400 uppercase tracking-wider mb-4">
          <i class="fas fa-star mr-2"></i> Favorites
        </h4>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div v-for="contact in favoriteContacts" :key="contact.id" class="contact-card favorite">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-4">
                <div class="contact-avatar">
                  <i class="fas fa-user"></i>
                </div>
                <div>
                  <h5 class="contact-name">{{ contact.nickname }}</h5>
                  <p class="contact-id">Node #{{ contact.contactUserId }}</p>
                </div>
              </div>
              <div class="flex gap-2">
                <button @click="toggleFavorite(contact.id)" class="icon-btn favorite-active" title="Remove from favorites">
                  <i class="fas fa-star"></i>
                </button>
                <button @click="initiateTransfer(contact)" class="icon-btn transfer" title="Transfer">
                  <i class="fas fa-paper-plane"></i>
                </button>
                <button @click="deleteContact(contact.id)" class="icon-btn delete" title="Delete">
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- All Contacts -->
      <div v-if="regularContacts.length > 0">
        <h4 class="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4">
          <i class="fas fa-address-book mr-2"></i> All Contacts
        </h4>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div v-for="contact in regularContacts" :key="contact.id" class="contact-card">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-4">
                <div class="contact-avatar">
                  <i class="fas fa-user"></i>
                </div>
                <div>
                  <h5 class="contact-name">{{ contact.nickname }}</h5>
                  <p class="contact-id">Node #{{ contact.contactUserId }}</p>
                </div>
              </div>
              <div class="flex gap-2">
                <button @click="toggleFavorite(contact.id)" class="icon-btn" title="Add to favorites">
                  <i class="far fa-star"></i>
                </button>
                <button @click="initiateTransfer(contact)" class="icon-btn transfer" title="Transfer">
                  <i class="fas fa-paper-plane"></i>
                </button>
                <button @click="deleteContact(contact.id)" class="icon-btn delete" title="Delete">
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="contacts.length === 0" class="empty-state">
        <i class="fas fa-address-book text-6xl mb-6 opacity-10"></i>
        <p class="uppercase tracking-widest font-black text-xs opacity-40">No contacts found</p>
        <button @click="showAddModal = true" class="midnight-btn mt-6">
          Add Your First Contact
        </button>
      </div>
    </div>

    <!-- Add Contact Modal -->
    <Transition name="modal">
      <div v-if="showAddModal" class="modal-overlay" @click.self="showAddModal = false">
        <div class="modal-content">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold text-white">Add New Contact</h3>
            <button @click="showAddModal = false" class="text-slate-400 hover:text-white">
              <i class="fas fa-times text-xl"></i>
            </button>
          </div>

          <div class="space-y-6">
            <div class="input-group">
              <div class="input-wrapper">
                <input v-model="newContact.contactUserId" type="number" id="contactUserId" placeholder=" " required>
                <label for="contactUserId" class="floating-label">Recipient Node ID</label>
                <i class="fa-solid fa-hashtag input-icon"></i>
              </div>
            </div>

            <div class="input-group">
              <div class="input-wrapper">
                <input v-model="newContact.nickname" type="text" id="nickname" placeholder=" " required>
                <label for="nickname" class="floating-label">Nickname</label>
                <i class="fa-solid fa-tag input-icon"></i>
              </div>
            </div>

            <button @click="addContact" class="midnight-btn w-full">
              <i class="fas fa-plus mr-2"></i> Add Contact
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.contacts-container {
  animation: fadeIn 0.5s ease-out;
}

.contact-card {
  background: rgba(255, 255, 255, 0.02);
  padding: 1.25rem;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.contact-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  background: rgba(99, 102, 241, 0.05);
  transform: translateY(-2px);
}

.contact-card.favorite {
  border-color: rgba(251, 191, 36, 0.3);
  background: rgba(251, 191, 36, 0.05);
}

.contact-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.25rem;
}

.contact-name {
  font-weight: 700;
  font-size: 1rem;
  color: white;
}

.contact-id {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 2px;
  font-weight: 700;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-btn:hover {
  background: rgba(99, 102, 241, 0.2);
  border-color: #6366f1;
  color: white;
}

.icon-btn.favorite-active {
  background: rgba(251, 191, 36, 0.2);
  border-color: rgba(251, 191, 36, 0.4);
  color: #fbbf24;
}

.icon-btn.transfer:hover {
  background: rgba(16, 185, 129, 0.2);
  border-color: #10b981;
  color: #10b981;
}

.icon-btn.delete:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: #ef4444;
  color: #ef4444;
}

.midnight-btn-sm {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.4);
}

.midnight-btn-sm:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 30px -5px rgba(99, 102, 241, 0.5);
}

.success-message {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #10b981;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 700;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-content {
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(24px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 2rem;
  max-width: 480px;
  width: 90%;
  box-shadow: 0 40px 100px -20px rgba(0, 0, 0, 0.8);
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-content,
.modal-leave-to .modal-content {
  transform: scale(0.9) translateY(20px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
