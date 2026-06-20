import { defineStore } from "pinia";
import type { CurrentUser } from "@/features/auth/model";
import { store, type userType } from "../utils";

export const useUserStore = defineStore("pure-user", {
  state: (): userType => ({
    currentUser: null,
    initialized: false
  }),
  getters: {
    isAdmin: state => state.currentUser?.type === "ADMIN",
    isDemo: state => state.currentUser?.type === "DEMO"
  },
  actions: {
    SET_CURRENT_USER(user: CurrentUser) {
      this.currentUser = user;
    },
    SET_INITIALIZED(initialized: boolean) {
      this.initialized = initialized;
    },
    CLEAR_USER() {
      this.currentUser = null;
    }
  }
});

export function useUserStoreHook() {
  return useUserStore(store);
}
