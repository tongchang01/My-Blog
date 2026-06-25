import { computed, reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import {
  getCurrentUser,
  updateCurrentUserProfile
} from "@/api/auth";
import type { CurrentUser, UserProfile } from "@/features/auth/model";
import { useUserStoreHook } from "@/store/modules/user";
import {
  createUserProfileForm,
  userProfileFormToPayload,
  userProfileToForm,
  validateUserProfileForm,
  type UserProfileForm,
  type UserProfileFormErrors,
  type UserProfilePayload
} from "./form";

export interface ProfileManagementApi {
  getCurrentUser(): Promise<ApiResponse<CurrentUser>>;
  updateCurrentUserProfile(
    payload: UserProfilePayload
  ): Promise<ApiResponse<UserProfile>>;
}

const defaultApi: ProfileManagementApi = {
  getCurrentUser,
  updateCurrentUserProfile
};

function asError(error: unknown): Error {
  return error instanceof Error ? error : new Error(String(error));
}

function clearRecord(record: Record<string, unknown>): void {
  Object.keys(record).forEach(key => delete record[key]);
}

export function useProfileManagement(api: ProfileManagementApi = defaultApi) {
  const userStore = useUserStoreHook();
  const form = reactive<UserProfileForm>(createUserProfileForm());
  const formErrors = reactive<UserProfileFormErrors>({});
  const loading = ref(false);
  const saving = ref(false);
  const error = ref<Error | null>(null);
  const saveError = ref<Error | null>(null);
  const currentUser = computed(() => userStore.currentUser);

  function resetForm(profile: UserProfile): void {
    Object.assign(form, userProfileToForm(profile));
    clearRecord(formErrors);
  }

  async function initialize(): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      const response = await api.getCurrentUser();
      userStore.SET_CURRENT_USER(response.data);
      resetForm(response.data.profile);
    } catch (reason) {
      error.value = asError(reason);
    } finally {
      loading.value = false;
    }
  }

  async function refresh(): Promise<void> {
    await initialize();
  }

  async function save(): Promise<boolean> {
    const errors = validateUserProfileForm(form);
    clearRecord(formErrors);
    Object.assign(formErrors, errors);
    if (Object.keys(errors).length) return false;

    saving.value = true;
    saveError.value = null;
    try {
      const response = await api.updateCurrentUserProfile(
        userProfileFormToPayload(form)
      );
      const current = userStore.currentUser;
      if (current) {
        userStore.SET_CURRENT_USER({
          ...current,
          profile: response.data
        });
      }
      resetForm(response.data);
      return true;
    } catch (reason) {
      saveError.value = asError(reason);
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    currentUser,
    form,
    formErrors,
    loading,
    saving,
    error,
    saveError,
    initialize,
    refresh,
    save
  };
}
