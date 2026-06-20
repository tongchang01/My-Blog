import { useNav } from "./useNav";
import { useI18n } from "vue-i18n";
import { useRoute } from "vue-router";
import { watch, onBeforeMount, type Ref } from "vue";
import {
  type AdminLocale,
  loadAdminLocale,
  saveAdminLocale
} from "@/features/i18n/locale";

export function useTranslationLang(ref?: Ref) {
  const { $storage, changeTitle, handleResize } = useNav();
  const { locale, t } = useI18n();
  const route = useRoute();

  function changeLocale(nextLocale: AdminLocale) {
    $storage.locale = { locale: nextLocale };
    saveAdminLocale(nextLocale);
    locale.value = nextLocale;
    ref && handleResize(ref.value);
  }

  function translationCh() {
    changeLocale("zh");
  }

  function translationJa() {
    changeLocale("ja");
  }

  function translationEn() {
    changeLocale("en");
  }

  watch(
    () => locale.value,
    () => {
      changeTitle(route.meta);
    }
  );

  onBeforeMount(() => {
    locale.value = loadAdminLocale(navigator.language);
  });

  return {
    t,
    route,
    locale,
    translationCh,
    translationJa,
    translationEn
  };
}
