<script setup>
import { ref } from 'vue';
import axios from 'axios';

const props = defineProps(['user']);
const emit = defineEmits(['update-user', 'logout']);

const uploadFile = ref(null);
const previewUrl = ref(props.user.avatarUrl || null);
const isUploading = ref(false);
const updateForm = ref({
  username: props.user.username,
  password: ''
});
const isUpdating = ref(false);
const message = ref('');
const isError = ref(false);

const handleFileChange = (event) => {
  const file = event.target.files[0];
  if (file) {
    uploadFile.value = file;
    // Create a local blob URL for preview
    previewUrl.value = URL.createObjectURL(file);
  }
};

const getAvatarSrc = () => {
  if (!previewUrl.value) {
    // Default gravatar if no image
    return 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y';
  }
  // Check if it's a blob url (local preview)
  if (previewUrl.value.startsWith('blob:')) {
    return previewUrl.value;
  }
  // Otherwise it's a server path, prepend backend URL
  return `http://localhost:8080${previewUrl.value}`;
};

const uploadAvatar = async () => {
  if (!uploadFile.value) return;
  
  isUploading.value = true;
  const formData = new FormData();
  formData.append('file', uploadFile.value);
  
  try {
    const token = localStorage.getItem('jwt_token');
    const res = await axios.post(`http://localhost:8080/api/users/${props.user.id}/upload-avatar`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${token}`
      }
    });
    
    // Update local user data
    const updatedUser = { ...props.user, avatarUrl: res.data.avatarUrl };
    emit('update-user', updatedUser);
    showMessage('Avatar uploaded successfully', false);
    // Clear the upload file state but keep preview
    uploadFile.value = null;
  } catch (error) {
    showMessage(error.response?.data?.message || 'Failed to upload avatar', true);
  } finally {
    isUploading.value = false;
  }
};

const updateProfile = async () => {
  isUpdating.value = true;
  try {
    const token = localStorage.getItem('jwt_token');
    
    // Create clean payload - remove password if empty
    const payload = { ...updateForm.value };
    if (!payload.password) {
      delete payload.password;
    }

    const res = await axios.put(`http://localhost:8080/api/users/${props.user.id}`, payload, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    
    emit('update-user', res.data);
    updateForm.value.password = ''; // Clear password
    showMessage('Profile updated successfully', false);
  } catch (error) {
    // Handle validation errors (Map) or simple message
    let errorMsg = error.response?.data?.message;
    
    if (!errorMsg && error.response?.data?.errors) {
      // Join first error from each field
      errorMsg = Object.values(error.response.data.errors).join(', ');
    }
    
    showMessage(errorMsg || 'Failed to update profile', true);
  } finally {
    isUpdating.value = false;
  }
};

const deleteAccount = async () => {
  if(!confirm('Are you sure you want to delete your account? This cannot be undone.')) return;
  
  try {
    const token = localStorage.getItem('jwt_token');
    await axios.delete(`http://localhost:8080/api/users/${props.user.id}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    emit('logout');
  } catch (error) {
    showMessage('Failed to delete account', true);
  }
};

const showMessage = (msg, error) => {
  message.value = msg;
  isError.value = error;
  setTimeout(() => message.value = '', 3000);
};
</script>

<template>
  <div class="midnight-card p-10 relative overflow-hidden">
    <!-- Decorative background glow -->
    <div class="absolute top-0 right-0 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2"></div>
    
    <div class="flex items-center justify-between mb-10 relative z-10">
      <div>
        <h3 class="text-2xl font-bold text-white mb-2">Account Settings</h3>
        <p class="text-slate-400 text-sm">Manage your profile and security preference</p>
      </div>
      <div class="px-4 py-2 bg-indigo-500/10 rounded-lg border border-indigo-500/20 text-indigo-300 text-xs font-bold uppercase tracking-widest">
        Level 1 Verified
      </div>
    </div>
    
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-12 relative z-10">
      <!-- Left Column: Avatar -->
      <div class="lg:col-span-1 flex flex-col items-center">
        <div class="relative group">
          <div class="w-48 h-48 rounded-full border-4 border-indigo-500/20 p-1 shadow-2xl shadow-indigo-500/20 transition-all duration-300 hover:border-indigo-400 hover:shadow-indigo-500/40" style="border-radius: 50%;">
            <div class="w-full h-full rounded-full overflow-hidden bg-slate-800 relative" style="border-radius: 50%;">
              <img :src="getAvatarSrc()" alt="Profile" class="w-full h-full object-cover" style="border-radius: 50%;" />
              <!-- Hover Overlay -->
              <label class="absolute inset-0 bg-black/60 flex flex-col items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer backdrop-blur-sm" style="border-radius: 50%;">
                <i class="fas fa-camera text-2xl text-white mb-2"></i>
                <span class="text-xs font-bold text-white tracking-wider uppercase">Change Photo</span>
                <input type="file" @change="handleFileChange" accept="image/*" class="hidden">
              </label>
            </div>
          </div>
          
          <!-- Online status dot -->
          <div class="absolute bottom-4 right-4 w-6 h-6 bg-green-500 rounded-full border-4 border-[#0f172a] shadow-lg"></div>
        </div>
        
        <div v-if="uploadFile" class="mt-6 animate-in">
          <button @click="uploadAvatar" :disabled="isUploading" class="midnight-btn-sm w-full shadow-lg shadow-indigo-500/30">
            <i v-if="isUploading" class="fas fa-circle-notch fa-spin mr-2"></i>
            {{ isUploading ? 'Uploading...' : 'Save New Avatar' }}
          </button>
        </div>
        
        <div class="mt-8 text-center text-xs text-slate-500 leading-relaxed max-w-[200px]">
          Accepted formats: JPG, PNG, GIF<br>
          Max file size: 5MB
        </div>
      </div>

      <!-- Right Column: Form -->
      <div class="lg:col-span-2 space-y-8">
        <div class="grid gap-6">
          <div class="space-y-2">
            <label class="text-xs text-indigo-300 uppercase font-bold tracking-widest pl-1">Display Name / Identity</label>
            <div class="relative group">
              <input v-model="updateForm.username" type="text" class="input-modern pl-12" />
              <i class="fas fa-user absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-indigo-400 transition-colors"></i>
            </div>
          </div>
          
          <div class="space-y-2">
            <label class="text-xs text-indigo-300 uppercase font-bold tracking-widest pl-1">Security Phrase (Password)</label>
            <div class="relative group">
              <input v-model="updateForm.password" type="password" placeholder="Enter new passphrase to update" class="input-modern pl-12" />
              <i class="fas fa-lock absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-indigo-400 transition-colors"></i>
            </div>
            <p class="text-xs text-slate-500 pl-1 mt-1">Leave blank unless you wish to change your access credentials.</p>
          </div>
        </div>

        <div class="pt-6 flex flex-col sm:flex-row gap-4 justify-end border-t border-white/5">
          <button @click="deleteAccount" class="px-6 py-3 rounded-xl border border-red-500/20 text-red-400 hover:bg-red-500/10 hover:border-red-500/50 text-sm font-bold uppercase tracking-wider transition-all">
            Delete Profile
          </button>
          
          <button @click="updateProfile" :disabled="isUpdating" class="midnight-btn flex-1 sm:flex-none sm:w-auto px-8 py-3">
             <span v-if="!isUpdating">Save Changes</span>
             <span v-else><i class="fas fa-circle-notch fa-spin mr-2"></i> Processing</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Notification Toast -->
    <Transition name="fade">
      <div v-if="message" class="absolute bottom-8 left-1/2 -translate-x-1/2 px-6 py-3 rounded-full backdrop-blur-md shadow-xl flex items-center gap-3 border z-20"
           :class="isError ? 'bg-red-500/10 border-red-500/30 text-red-200' : 'bg-green-500/10 border-green-500/30 text-green-200'">
        <i :class="['fas', isError ? 'fa-exclamation-circle' : 'fa-check-circle']"></i>
        <span class="text-sm font-bold">{{ message }}</span>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.input-modern {
  width: 100%;
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1rem; 
  border-radius: 12px;
  color: white; 
  font-weight: 500;
  outline: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.input-modern:focus {
  border-color: #6366f1;
  background: rgba(15, 23, 42, 0.8);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.midnight-btn {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white; border: none; border-radius: 12px;
  font-weight: 800; text-transform: uppercase; letter-spacing: 0.1em;
  font-size: 0.9rem;
  box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.4);
  transition: all 0.3s;
}
.midnight-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 15px 30px -5px rgba(99, 102, 241, 0.5); filter: brightness(1.1); }
.midnight-btn:disabled { opacity: 0.7; cursor: not-allowed; }

.midnight-btn-sm {
  background: #6366f1; color: white; padding: 0.75rem 1.5rem;
  border-radius: 20px; font-weight: 700; font-size: 0.8rem;
  text-transform: uppercase; letter-spacing: 0.1em;
  transition: all 0.3s;
}
.midnight-btn-sm:hover:not(:disabled) { transform: scale(1.05); background: #4f46e5; }

/* Custom Animations */
.fade-enter-active, .fade-leave-active { transition: all 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translate(-50%, 20px); }

.animate-in { animation: fadeInUp 0.5s ease-out forwards; opacity: 0; }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
